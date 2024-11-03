/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.json.JSONPointerException;

final class RequestAccess {

  private static final ClassValue<RequestAccess> REQUEST_ACCESSORS =
      new ClassValue<RequestAccess>() {
        @Override
        protected RequestAccess computeValue(Class<?> type) {
          return new RequestAccess(type);
        }
      };

  @Nullable
  static String getLambdaName(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getLambdaName, request);
  }

  @Nullable
  static String getLambdaResourceId(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getLambdaResourceId, request);
  }

  @Nullable
  static String getSecretArn(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getSecretArn, request);
  }

  @Nullable
  static String getSnsTopicArn(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getSnsTopicArn, request);
  }

  @Nullable
  static String getStepFunctionsActivityArn(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getStepFunctionsActivityArn, request);
  }

  @Nullable
  static String getStateMachineArn(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getStateMachineArn, request);
  }

  @Nullable
  static String getBucketName(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getBucketName, request);
  }

  @Nullable
  static String getQueueUrl(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getQueueUrl, request);
  }

  @Nullable
  static String getQueueName(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getQueueName, request);
  }

  @Nullable
  static String getStreamName(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getStreamName, request);
  }

  @Nullable
  static String getTableName(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getTableName, request);
  }

  @Nullable
  static String getAgentId(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getAgentId, request);
  }

  @Nullable
  static String getKnowledgeBaseId(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getKnowledgeBaseId, request);
  }

  @Nullable
  static String getDataSourceId(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getDataSourceId, request);
  }

  @Nullable
  static String getGuardrailId(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getGuardrailId, request);
  }

  @Nullable
  static String getGuardrailArn(Object request) {
    if (request == null) {
      return null;
    }
    return findNestedAccessorOrNull(request, "getGuardrailArn");
  }

  @Nullable
  static String getModelId(Object request) {
    if (request == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(request.getClass());
    return invokeOrNull(access.getModelId, request);
  }

  private static JSONObject parseRequestBody(ByteBuffer bodyBuffer) {
    bodyBuffer.rewind();
    byte[] bytes = new byte[bodyBuffer.remaining()];
    bodyBuffer.get(bytes);
    String bodyString = new String(bytes, StandardCharsets.UTF_8);
    return new JSONObject(bodyString);
  }

  private static final Map<String, String> MODEL_TO_MAX_TOKENS_PATH;

  static {
    Map<String, String> map = new HashMap<>();
    map.put("amazon.titan", "/textGenerationConfig/maxTokenCount");
    map.put("anthropic.claude", "/max_tokens");
    map.put("cohere.command-r", "/max_tokens");
    map.put("ai21.jamba", "/max_tokens");
    map.put("meta.llama", "/max_gen_len");
    map.put("mistral.mistral", "/max_tokens");
    MODEL_TO_MAX_TOKENS_PATH = Collections.unmodifiableMap(map);
  }

  @Nullable
  static String getGenAiMaxTokens(Object apiBody) {
    if (apiBody == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(apiBody.getClass());
    String modelId = invokeOrNull(access.getModelId, apiBody);
    ByteBuffer bodyBuffer = invokeOrNullGeneric(access.getBody, apiBody, ByteBuffer.class);

    if (modelId == null || bodyBuffer == null) {
      return null;
    }

    try {
      JSONObject jsonBody = parseRequestBody(bodyBuffer);

      String jsonPath =
          MODEL_TO_MAX_TOKENS_PATH.entrySet().stream()
              .filter(entry -> modelId.contains(entry.getKey()))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElse(null);

      if (jsonPath == null) {
        return null;
      }

      Object val = new JSONPointer(jsonPath).queryFrom(jsonBody);
      return val != null ? val.toString() : null;
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static final Map<String, String> MODEL_TO_TEMPERATURE_PATH;

  static {
    Map<String, String> map = new HashMap<>();
    map.put("amazon.titan", "/textGenerationConfig/temperature");
    map.put("anthropic.claude", "/temperature");
    map.put("cohere.command-r", "/temperature");
    map.put("ai21.jamba", "/temperature");
    map.put("meta.llama", "/temperature");
    map.put("mistral.mistral", "/temperature");
    MODEL_TO_TEMPERATURE_PATH = Collections.unmodifiableMap(map);
  }

  @Nullable
  static String getGenAiTemperature(Object apiBody) {
    if (apiBody == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(apiBody.getClass());
    String modelId = invokeOrNull(access.getModelId, apiBody);
    ByteBuffer bodyBuffer = invokeOrNullGeneric(access.getBody, apiBody, ByteBuffer.class);

    if (modelId == null || bodyBuffer == null) {
      return null;
    }

    try {
      JSONObject jsonBody = parseRequestBody(bodyBuffer);

      String jsonPath =
          MODEL_TO_TEMPERATURE_PATH.entrySet().stream()
              .filter(entry -> modelId.contains(entry.getKey()))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElse(null);

      if (jsonPath == null) {
        return null;
      }

      Object val = new JSONPointer(jsonPath).queryFrom(jsonBody);
      return val != null ? val.toString() : null;
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static final Map<String, String> MODEL_TO_TOP_P_PATH;

  static {
    Map<String, String> map = new HashMap<>();
    map.put("amazon.titan", "/textGenerationConfig/topP");
    map.put("anthropic.claude", "/top_p");
    map.put("cohere.command-r", "/p");
    map.put("ai21.jamba", "/top_p");
    map.put("meta.llama", "/top_p");
    map.put("mistral.mistral", "/top_p");
    MODEL_TO_TOP_P_PATH = Collections.unmodifiableMap(map);
  }

  @Nullable
  static String getGenAiTopP(Object apiBody) {
    if (apiBody == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(apiBody.getClass());
    String modelId = invokeOrNull(access.getModelId, apiBody);
    ByteBuffer bodyBuffer = invokeOrNullGeneric(access.getBody, apiBody, ByteBuffer.class);

    if (modelId == null || bodyBuffer == null) {
      return null;
    }

    try {
      JSONObject jsonBody = parseRequestBody(bodyBuffer);
      String jsonPath =
          MODEL_TO_TOP_P_PATH.entrySet().stream()
              .filter(entry -> modelId.contains(entry.getKey()))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElse(null);

      if (jsonPath == null) {
        return null;
      }

      Object val = new JSONPointer(jsonPath).queryFrom(jsonBody);
      return val != null ? val.toString() : null;
    } catch (RuntimeException e) {
      return null;
    }
  }

  @Nullable
  static String getGenAiInputTokens(Object apiBody) {
    // Model -> Path Mapping:
    // Amazon Titan: "/inputTextTokenCount"
    // Anthropic Claude: "/usage/input_tokens"
    // Cohere Command R: "/message"
    // AI21 Jamba: "/usage/prompt_tokens"
    // Meta Llama: "/prompt_token_count"
    // Mistral AI: "/prompt"
    if (apiBody == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(apiBody.getClass());
    ByteBuffer bodyBuffer = invokeOrNullGeneric(access.getBody, apiBody, ByteBuffer.class);

    if (bodyBuffer == null) {
      return null;
    }

    JSONObject jsonBody = parseRequestBody(bodyBuffer);

    String directTokenCount =
        Stream.of(
                "/inputTextTokenCount",
                "/usage/input_tokens",
                "/usage/prompt_tokens",
                "/prompt_token_count")
            .map(
                path -> {
                  try {
                    Object val = new JSONPointer(path).queryFrom(jsonBody);
                    return val != null ? val.toString() : null;
                  } catch (JSONPointerException e) {
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

    if (directTokenCount != null) {
      return directTokenCount;
    }

    return Stream.of("/prompt", "/message")
        .map(
            path -> {
              try {
                Object inputText = new JSONPointer(path).queryFrom(jsonBody);
                if (inputText == null) {
                  return null;
                }
                int indirectTokenCount = (int) Math.ceil(inputText.toString().length() / 6.0);
                return Integer.toString(indirectTokenCount);
              } catch (JSONPointerException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  static String getGenAiOutputTokens(Object apiBody) {
    // Model -> Path Mapping:
    // Amazon Titan: "/results/0/tokenCount"
    // Anthropic Claude: "/usage/output_tokens"
    // Cohere Command R: "/text"
    // AI21 Jamba: "/usage/completion_tokens"
    // Meta Llama: "/generation_token_count"
    // Mistral AI: "/outputs/0/text"
    if (apiBody == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(apiBody.getClass());
    ByteBuffer bodyBuffer = invokeOrNullGeneric(access.getBody, apiBody, ByteBuffer.class);

    if (bodyBuffer == null) {
      return null;
    }

    JSONObject jsonBody = parseRequestBody(bodyBuffer);

    String directTokenCount =
        Stream.of(
                "/results/0/tokenCount",
                "/usage/output_tokens",
                "/usage/completion_tokens",
                "/generation_token_count")
            .map(
                path -> {
                  try {
                    Object val = new JSONPointer(path).queryFrom(jsonBody);
                    return val != null ? val.toString() : null;
                  } catch (JSONPointerException e) {
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

    if (directTokenCount != null) {
      return directTokenCount;
    }

    return Stream.of("/outputs/0/text", "/text")
        .map(
            path -> {
              try {
                Object inputText = new JSONPointer(path).queryFrom(jsonBody);
                if (inputText == null) {
                  return null;
                }
                int indirectTokenCount = (int) Math.ceil(inputText.toString().length() / 6.0);
                return Integer.toString(indirectTokenCount);
              } catch (JSONPointerException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  static String getGenAiFinishReasons(Object apiBody) {
    // Model -> Path Mapping:
    // Amazon Titan: "/results/0/completionReason"
    // Anthropic Claude: "/stop_reason"
    // Cohere Command R: "/finish_reason"
    // AI21 Jamba: "/choices/0/finish_reason"
    // Meta Llama: "/stop_reason"
    // Mistral AI: "/outputs/0/stop_reason"
    if (apiBody == null) {
      return null;
    }
    RequestAccess access = REQUEST_ACCESSORS.get(apiBody.getClass());
    ByteBuffer bodyBuffer = invokeOrNullGeneric(access.getBody, apiBody, ByteBuffer.class);

    if (bodyBuffer == null) {
      return null;
    }

    JSONObject jsonBody = parseRequestBody(bodyBuffer);

    return Stream.of(
            "/results/0/completionReason",
            "/stop_reason",
            "/choices/0/finish_reason",
            "/stop_reason",
            "/outputs/0/stop_reason",
            "/finish_reason")
        .map(
            path -> {
              try {
                Object val = new JSONPointer(path).queryFrom(jsonBody);
                return val != null ? "[" + val.toString() + "]" : null;
              } catch (JSONPointerException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @Nullable
  private static String invokeOrNull(@Nullable MethodHandle method, Object obj) {
    if (method == null) {
      return null;
    }
    try {
      return (String) method.invoke(obj);
    } catch (Throwable t) {
      return null;
    }
  }

  @Nullable
  private static <T> T invokeOrNullGeneric(
      @Nullable MethodHandle method, Object obj, Class<T> returnType) {
    if (method == null) {
      return null;
    }
    try {
      return returnType.cast(method.invoke(obj));
    } catch (Throwable e) {
      return null;
    }
  }

  @Nullable private final MethodHandle getBucketName;
  @Nullable private final MethodHandle getQueueUrl;
  @Nullable private final MethodHandle getQueueName;
  @Nullable private final MethodHandle getStreamName;
  @Nullable private final MethodHandle getTableName;
  @Nullable private final MethodHandle getAgentId;
  @Nullable private final MethodHandle getKnowledgeBaseId;
  @Nullable private final MethodHandle getDataSourceId;
  @Nullable private final MethodHandle getGuardrailId;
  @Nullable private final MethodHandle getModelId;
  @Nullable private final MethodHandle getBody;
  @Nullable private final MethodHandle getStateMachineArn;
  @Nullable private final MethodHandle getStepFunctionsActivityArn;
  @Nullable private final MethodHandle getSnsTopicArn;
  @Nullable private final MethodHandle getSecretArn;
  @Nullable private final MethodHandle getLambdaName;
  @Nullable private final MethodHandle getLambdaResourceId;

  private RequestAccess(Class<?> clz) {
    getBucketName = findAccessorOrNull(clz, "getBucketName", String.class);
    getQueueUrl = findAccessorOrNull(clz, "getQueueUrl", String.class);
    getQueueName = findAccessorOrNull(clz, "getQueueName", String.class);
    getStreamName = findAccessorOrNull(clz, "getStreamName", String.class);
    getTableName = findAccessorOrNull(clz, "getTableName", String.class);
    getAgentId = findAccessorOrNull(clz, "getAgentId", String.class);
    getKnowledgeBaseId = findAccessorOrNull(clz, "getKnowledgeBaseId", String.class);
    getDataSourceId = findAccessorOrNull(clz, "getDataSourceId", String.class);
    getGuardrailId = findAccessorOrNull(clz, "getGuardrailId", String.class);
    getModelId = findAccessorOrNull(clz, "getModelId", String.class);
    getBody = findAccessorOrNull(clz, "getBody", ByteBuffer.class);
    getStateMachineArn = findAccessorOrNull(clz, "getStateMachineArn", String.class);
    getStepFunctionsActivityArn = findAccessorOrNull(clz, "getActivityArn", String.class);
    getSnsTopicArn = findAccessorOrNull(clz, "getTopicArn", String.class);
    getSecretArn = findAccessorOrNull(clz, "getARN", String.class);
    getLambdaName = findAccessorOrNull(clz, "getFunctionName", String.class);
    getLambdaResourceId = findAccessorOrNull(clz, "getUUID", String.class);
  }

  @Nullable
  private static MethodHandle findAccessorOrNull(
      Class<?> clz, String methodName, Class<?> returnType) {
    try {
      return MethodHandles.publicLookup()
          .findVirtual(clz, methodName, MethodType.methodType(returnType));
    } catch (Throwable t) {
      return null;
    }
  }

  @Nullable
  private static String findNestedAccessorOrNull(Object obj, String... methodNames) {
    Object current = obj;
    for (String methodName : methodNames) {
      if (current == null) {
        return null;
      }
      try {
        Method method = current.getClass().getMethod(methodName);
        current = method.invoke(current);
      } catch (Exception e) {
        return null;
      }
    }
    return (current instanceof String) ? (String) current : null;
  }
}
