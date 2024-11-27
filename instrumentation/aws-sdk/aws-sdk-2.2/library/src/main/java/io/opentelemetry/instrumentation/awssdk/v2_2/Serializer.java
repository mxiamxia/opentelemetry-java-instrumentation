/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;

class Serializer {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Nullable
  String serialize(Object target) {

    if (target == null) {
      return null;
    }

    if (target instanceof SdkPojo) {
      return serialize((SdkPojo) target);
    }
    if (target instanceof Collection) {
      return serialize((Collection<?>) target);
    }
    if (target instanceof Map) {
      return serialize(((Map<?, ?>) target).keySet());
    }
    // simple type
    return target.toString();
  }

  @Nullable
  String serialize(String attributeName, Object target) {
    try {
      JsonNode jsonBody;
      if (target instanceof SdkBytes) {
        String jsonString = ((SdkBytes) target).asUtf8String();
        jsonBody = objectMapper.readTree(jsonString);
      } else {
        if (target != null) {
          return target.toString();
        }
        return null;
      }

      switch (attributeName) {
        case "gen_ai.request.max_tokens":
          return getMaxTokens(jsonBody);
        case "gen_ai.request.temperature":
          return getTemperature(jsonBody);
        case "gen_ai.request.top_p":
          return getTopP(jsonBody);
        case "gen_ai.response.finish_reasons":
          return getFinishReasons(jsonBody);
        case "gen_ai.usage.input_tokens":
          return getInputTokens(jsonBody);
        case "gen_ai.usage.output_tokens":
          return getOutputTokens(jsonBody);
        default:
          return null;
      }
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  @Nullable
  private static String serialize(SdkPojo sdkPojo) {
    ProtocolMarshaller<SdkHttpFullRequest> marshaller =
        AwsJsonProtocolFactoryAccess.createMarshaller();
    if (marshaller == null) {
      return null;
    }
    Optional<ContentStreamProvider> optional = marshaller.marshall(sdkPojo).contentStreamProvider();
    return optional
        .map(
            csp -> {
              try (InputStream cspIs = csp.newStream()) {
                return IoUtils.toUtf8String(cspIs);
              } catch (IOException e) {
                return null;
              }
            })
        .orElse(null);
  }

  private String serialize(Collection<?> collection) {
    String serialized = collection.stream().map(this::serialize).collect(Collectors.joining(","));
    return (StringUtils.isEmpty(serialized) ? null : "[" + serialized + "]");
  }

  @Nullable
  private static String findFirstMatchingPath(JsonNode jsonBody, String... paths) {
    if (jsonBody == null) {
      return null;
    }

    return Stream.of(paths)
        .map(
            path -> {
              JsonNode node = jsonBody.at(path);
              if (node != null && !node.isMissingNode()) {
                return node.asText();
              }
              return null;
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @Nullable
  private static String approximateTokenCount(JsonNode jsonBody, String... textPaths) {
    if (jsonBody == null) {
      return null;
    }

    return Stream.of(textPaths)
        .map(
            path -> {
              JsonNode node = jsonBody.at(path);
              if (node != null && !node.isMissingNode()) {
                int tokenEstimate = (int) Math.ceil(node.asText().length() / 6.0);
                return Integer.toString(tokenEstimate);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  // Model -> Path Mapping:
  // Amazon Titan -> "/textGenerationConfig/maxTokenCount"
  // Anthropic Claude -> "/max_tokens"
  // Cohere Command -> "/max_tokens"
  // Cohere Command R -> "/max_tokens"
  // AI21 Jamba -> "/max_tokens"
  // Meta Llama -> "/max_gen_len"
  // Mistral AI -> "/max_tokens"
  @Nullable
  private static String getMaxTokens(JsonNode jsonBody) {
    return findFirstMatchingPath(
        jsonBody, "/textGenerationConfig/maxTokenCount", "/max_tokens", "/max_gen_len");
  }

  // Model -> Path Mapping:
  // Amazon Titan -> "/textGenerationConfig/temperature"
  // Anthropic Claude -> "/temperature"
  // Cohere Command -> "/temperature"
  // Cohere Command R -> "/temperature"
  // AI21 Jamba -> "/temperature"
  // Meta Llama -> "/temperature"
  // Mistral AI -> "/temperature"
  @Nullable
  private static String getTemperature(JsonNode jsonBody) {
    return findFirstMatchingPath(jsonBody, "/textGenerationConfig/temperature", "/temperature");
  }

  // Model -> Path Mapping:
  // Amazon Titan -> "/textGenerationConfig/topP"
  // Anthropic Claude -> "/top_p"
  // Cohere Command -> "/p"
  // Cohere Command R -> "/p"
  // AI21 Jamba -> "/top_p"
  // Meta Llama -> "/top_p"
  // Mistral AI -> "/top_p"
  @Nullable
  private static String getTopP(JsonNode jsonBody) {
    return findFirstMatchingPath(jsonBody, "/textGenerationConfig/topP", "/top_p", "/p");
  }

  // Model -> Path Mapping:
  // Amazon Titan -> "/results/0/completionReason"
  // Anthropic Claude -> "/stop_reason"
  // Cohere Command -> "/generations/0/finish_reason"
  // Cohere Command R -> "/finish_reason"
  // AI21 Jamba -> "/choices/0/finish_reason"
  // Meta Llama -> "/stop_reason"
  // Mistral AI -> "/outputs/0/stop_reason"
  @Nullable
  private static String getFinishReasons(JsonNode jsonBody) {
    String finishReason =
        findFirstMatchingPath(
            jsonBody,
            "/results/0/completionReason",
            "/stop_reason",
            "/generations/0/finish_reason",
            "/choices/0/finish_reason",
            "/outputs/0/stop_reason",
            "/finish_reason");

    return finishReason != null ? "[" + finishReason + "]" : null;
  }

  // Model -> Path Mapping:
  // Amazon Titan -> "/inputTextTokenCount"
  // Anthropic Claude -> "/usage/input_tokens"
  // Cohere Command -> "/prompt"
  // Cohere Command R -> "/message"
  // AI21 Jamba -> "/usage/prompt_tokens"
  // Meta Llama -> "/prompt_token_count"
  // Mistral AI -> "/prompt"
  @Nullable
  private static String getInputTokens(JsonNode jsonBody) {
    // Try direct tokens counts first
    String directCount =
        findFirstMatchingPath(
            jsonBody,
            "/inputTextTokenCount",
            "/usage/input_tokens",
            "/usage/prompt_tokens",
            "/prompt_token_count");

    if (directCount != null) {
      return directCount;
    }

    // Fall back to token approximation
    return approximateTokenCount(jsonBody, "/prompt", "/message");
  }

  // Model -> Path Mapping:
  // Amazon Titan -> "/results/0/tokenCount"
  // Anthropic Claude -> "/usage/output_tokens"
  // Cohere Command -> "/generations/0/text"
  // Cohere Command R -> "/text"
  // AI21 Jamba -> "/usage/completion_tokens"
  // Meta Llama -> "/generation_token_count"
  // Mistral AI -> "/outputs/0/text"
  @Nullable
  private static String getOutputTokens(JsonNode jsonBody) {
    // Try direct token counts first
    String directCount =
        findFirstMatchingPath(
            jsonBody,
            "/results/0/tokenCount",
            "/usage/output_tokens",
            "/usage/completion_tokens",
            "/generation_token_count");

    if (directCount != null) {
      return directCount;
    }

    // Fall back to token approximation
    return approximateTokenCount(jsonBody, "/outputs/0/text", "/text");
  }
}
