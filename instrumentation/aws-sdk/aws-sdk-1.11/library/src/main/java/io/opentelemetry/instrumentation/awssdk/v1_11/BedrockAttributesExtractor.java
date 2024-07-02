/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_AGENT_ID;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_BEDROCK_COMPLETION_TOKENS;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_BEDROCK_PROMOT_TOKENS;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_BEDROCK_RUNTIME_MODEL_ID;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_BEDROCK_SYSTEM;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_GUARDRAIL_ID;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_KNOWLEDGEBASE_ID;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class BedrockAttributesExtractor implements AttributesExtractor<Request<?>, Response<?>> {
  private String serviceName;
  private String modelName;
  private static final Map<String, Class<? extends AbstractBedrockAgentOperation>>
      REQUEST_CLASS_MAPPING;
  private static final Map<String, Class<? extends AbstractBedrockAgentOperation>>
      RESPONSE_CLASS_MAPPING;
  private static final Map<String, Class<? extends AbstractBedrockRuntimeModel>>
      BEDROC_RUNTIME_MODEL_MAPPING;

  static {
    List<Class<? extends AbstractBedrockAgentOperation>> operations =
        Arrays.asList(
            BedrockKnowledgeBaseOperation.class,
            BedrockDataSourceOperation.class,
            BedrockAgentOperation.class);

    List<Class<? extends AbstractBedrockRuntimeModel>> models =
        Arrays.asList(
            BedrockRuntimeClaudeModel.class,
            BedrockRuntimeLlamaModel.class,
            BedrockRuntimeTitanModel.class);

    REQUEST_CLASS_MAPPING =
        operations.stream()
            .flatMap(
                opClass -> {
                  try {
                    return opClass
                        .getDeclaredConstructor()
                        .newInstance()
                        .requestClassNames()
                        .stream()
                        .collect(
                            Collectors.toMap(
                                requestClassName -> requestClassName, requestClassName -> opClass))
                        .entrySet()
                        .stream();
                  } catch (InstantiationException
                      | IllegalAccessException
                      | InvocationTargetException
                      | NoSuchMethodException e) {
                    throw new IllegalStateException("Failed to instantiate operation class", e);
                  }
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    RESPONSE_CLASS_MAPPING =
        operations.stream()
            .flatMap(
                opClass -> {
                  try {
                    return opClass
                        .getDeclaredConstructor()
                        .newInstance()
                        .responseClassNames()
                        .stream()
                        .collect(
                            Collectors.toMap(
                                responseClassName -> responseClassName,
                                responseClassName -> opClass))
                        .entrySet()
                        .stream();
                  } catch (InstantiationException
                      | IllegalAccessException
                      | InvocationTargetException
                      | NoSuchMethodException e) {
                    throw new IllegalStateException("Failed to instantiate operation class", e);
                  }
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    BEDROC_RUNTIME_MODEL_MAPPING =
        models.stream()
            .flatMap(
                modelClass -> {
                  try {
                    return modelClass.getDeclaredConstructor().newInstance().modelNames().stream()
                        .collect(Collectors.toMap(modelName -> modelName, modelName -> modelClass))
                        .entrySet()
                        .stream();
                  } catch (InstantiationException
                      | IllegalAccessException
                      | InvocationTargetException
                      | NoSuchMethodException e) {
                    throw new IllegalStateException("Failed to instantiate operation class", e);
                  }
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, Request<?> request) {
    serviceName = request.getServiceName();
    AmazonWebServiceRequest originalRequest = request.getOriginalRequest();
    String[] requestClass = originalRequest.getClass().getName().split("\\.");
    switch (serviceName) {
      case "AmazonBedrock":
        setAttribute(attributes, AWS_GUARDRAIL_ID, originalRequest, RequestAccess::getGuardrailId);
        // Perform action for AmazonBedrock
        break;
      case "AWSBedrockAgent":
        Class<? extends AbstractBedrockAgentOperation> operationClass =
            REQUEST_CLASS_MAPPING.get(requestClass[requestClass.length - 1]);
        if (operationClass != null) {
          try {
            operationClass
                .getDeclaredConstructor()
                .newInstance()
                .onStart(attributes, parentContext, originalRequest);
          } catch (InstantiationException
              | IllegalAccessException
              | NoSuchMethodException
              | InvocationTargetException e) {
            throw new IllegalStateException("Failed to instantiate operation class", e);
          }
        }
        break;
      case "AWSBedrockAgentRuntime":
        setAttribute(attributes, AWS_AGENT_ID, originalRequest, RequestAccess::getAgentId);
        setAttribute(
            attributes, AWS_KNOWLEDGEBASE_ID, originalRequest, RequestAccess::getKnowledgeBaseId);
        break;
      case "AmazonBedrockRuntime":
        // TODO: Implement onStart for AWSBedrockRuntime
        if (!Objects.equals(requestClass[requestClass.length - 1], "InvokeModelRequest")) {
          break;
        }
        attributes.put(AWS_BEDROCK_SYSTEM, "AWS Bedrock");
        Function<Object, String> getter = RequestAccess::getModelId;
        String modelId = getter.apply(originalRequest);
        attributes.put(AWS_BEDROCK_RUNTIME_MODEL_ID, modelId);
        modelName = modelId.split("-")[0];
        Class<? extends AbstractBedrockRuntimeModel> modelClass =
            BEDROC_RUNTIME_MODEL_MAPPING.get(modelName);
        if (modelClass != null) {
          try {
            modelClass
                .getDeclaredConstructor()
                .newInstance()
                .onStart(attributes, parentContext, originalRequest);
          } catch (InstantiationException
              | IllegalAccessException
              | NoSuchMethodException
              | InvocationTargetException
              | JsonProcessingException e) {
            throw new IllegalStateException("Failed to instantiate operation class", e);
          }
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      Request<?> request,
      @Nullable Response<?> response,
      @Nullable Throwable error) {
    if (response != null) {
      Object awsResps = response.getAwsResponse();
      if (awsResps != null) {
        switch (serviceName) {
          case "AmazonBedrock":
            setAttribute(attributes, AWS_GUARDRAIL_ID, awsResps, RequestAccess::getGuardrailId);
            break;
          case "AWSBedrockAgent":
            String[] responseClass = awsResps.getClass().getName().split("\\.");
            Class<? extends AbstractBedrockAgentOperation> operationClass =
                RESPONSE_CLASS_MAPPING.get(responseClass[responseClass.length - 1]);
            if (operationClass != null) {
              try {
                operationClass
                    .getDeclaredConstructor()
                    .newInstance()
                    .onEnd(attributes, context, request, awsResps, error);
              } catch (InstantiationException
                  | IllegalAccessException
                  | NoSuchMethodException
                  | InvocationTargetException e) {
                throw new IllegalStateException("Failed to instantiate operation class", e);
              }
            }
            break;
          case "AWSBedrockAgentRuntime":
            setAttribute(attributes, AWS_AGENT_ID, awsResps, RequestAccess::getAgentId);
            setAttribute(
                attributes, AWS_KNOWLEDGEBASE_ID, awsResps, RequestAccess::getKnowledgeBaseId);
            break;
          case "AmazonBedrockRuntime":
            // TODO: Implement onEnd for AWSBedrockRuntime
            HttpResponse httpResps = response.getHttpResponse();
            Map<String, String> headers = httpResps.getHeaders();
            if (headers.containsKey("X-Amzn-Bedrock-Input-Token-Count")) {
              int inputTokenCount =
                  Integer.parseInt(headers.get("X-Amzn-Bedrock-Input-Token-Count"));
              attributes.put(String.valueOf(AWS_BEDROCK_PROMOT_TOKENS), inputTokenCount);
            }
            if (headers.containsKey("X-Amzn-Bedrock-Output-Token-Count")) {
              int outputTokenCount =
                  Integer.parseInt(headers.get("X-Amzn-Bedrock-Output-Token-Count"));
              attributes.put(String.valueOf(AWS_BEDROCK_COMPLETION_TOKENS), outputTokenCount);
            }

            Class<? extends AbstractBedrockRuntimeModel> modelClass =
                BEDROC_RUNTIME_MODEL_MAPPING.get(modelName);
            if (modelClass != null) {
              try {
                modelClass
                    .getDeclaredConstructor()
                    .newInstance()
                    .onEnd(attributes, context, request, awsResps, error);
              } catch (InstantiationException
                  | IllegalAccessException
                  | NoSuchMethodException
                  | InvocationTargetException e) {
                throw new IllegalStateException("Failed to instantiate operation class", e);
              } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to convert String reponse to Json", e);
              }
            }
            break;
          default:
            // Perform default action
            break;
        }
      }
    }
  }

  protected static void setAttribute(
      AttributesBuilder attributes,
      AttributeKey<String> key,
      Object object,
      Function<Object, String> getter) {
    String value = getter.apply(object);
    if (value != null) {
      attributes.put(key, value);
    }
  }
}
