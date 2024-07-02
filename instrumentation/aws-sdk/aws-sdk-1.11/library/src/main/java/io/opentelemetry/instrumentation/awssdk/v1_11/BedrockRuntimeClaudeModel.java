/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_BEDROCK_FINISH_REASONS;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_BEDROCK_RUNTIME_MAX_TOKEN_COUNT;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_BEDROCK_RUNTIME_TEMPRATURE;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_BEDROCK_RUNTIME_TOP_P;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;

class BedrockRuntimeClaudeModel extends AbstractBedrockRuntimeModel {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, AmazonWebServiceRequest originalRequest)
      throws JsonProcessingException {
    Function<Object, ByteBuffer> getter = RequestAccess::getBody;
    ByteBuffer body = getter.apply(originalRequest);
    ByteBuffer resultBodyBuffer = body.asReadOnlyBuffer();
    byte[] bytes = new byte[resultBodyBuffer.remaining()];
    resultBodyBuffer.get(bytes);
    String resultBody = new String(bytes, StandardCharsets.UTF_8);
    JsonNode jsonNode = objectMapper.readTree(resultBody);
    if (jsonNode.has("max_tokens_to_sample") && jsonNode.get("max_tokens_to_sample").isInt()) {
      int maxTokenCount = jsonNode.get("max_tokens_to_sample").asInt();
      attributes.put(String.valueOf(AWS_BEDROCK_RUNTIME_MAX_TOKEN_COUNT), maxTokenCount);
    } else if (jsonNode.has("max_tokens") && jsonNode.get("max_tokens").isInt()) {
      int maxTokenCount = jsonNode.get("max_tokens").asInt();
      attributes.put(String.valueOf(AWS_BEDROCK_RUNTIME_MAX_TOKEN_COUNT), maxTokenCount);
    }
    if (jsonNode.has("temperature") && jsonNode.get("temperature").isDouble()) {
      double temperature = jsonNode.get("temperature").asDouble();
      attributes.put(String.valueOf(AWS_BEDROCK_RUNTIME_TEMPRATURE), temperature);
    }
    if (jsonNode.has("top_p") && jsonNode.get("top_p").isDouble()) {
      double topP = jsonNode.get("top_p").asDouble();
      attributes.put(String.valueOf(AWS_BEDROCK_RUNTIME_TOP_P), topP);
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      Request<?> request,
      Object awsResps,
      @Nullable Throwable error)
      throws JsonProcessingException {
    Function<Object, ByteBuffer> getter = RequestAccess::getBody;
    ByteBuffer body = getter.apply(awsResps);
    ByteBuffer resultBodyBuffer = body.asReadOnlyBuffer();
    byte[] bytes = new byte[resultBodyBuffer.remaining()];
    resultBodyBuffer.get(bytes);
    String resultBody = new String(bytes, StandardCharsets.UTF_8);
    JsonNode jsonNode = objectMapper.readTree(resultBody);
    if (jsonNode.has("stop_reason") && jsonNode.get("stop_reason").isTextual()) {
      String completionReason = jsonNode.get("stop_reason").asText();
      attributes.put(AWS_BEDROCK_FINISH_REASONS, completionReason);
    }
  }

  @Override
  public List<String> modelNames() {
    return Arrays.asList("anthropic.claude");
  }
}
