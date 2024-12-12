/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public class LlmJson {
  private final Map<String, Object> jsonBody;

  public LlmJson(Map<String, Object> jsonBody) {
    this.jsonBody = jsonBody;
  }

  @Nullable
  private String approximateTokenCount(String... textPaths) {
    return Arrays.stream(textPaths)
        .map(
            path -> {
              Object value = BedrockJsonParser.JsonPathResolver.resolvePath(jsonBody, path);
              if (value instanceof String) {
                int tokenEstimate = (int) Math.ceil(((String) value).length() / 6.0);
                return Integer.toString(tokenEstimate);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  // Model -> Path Mapping:
  // Amazon Nova -> "/inferenceConfig/max_new_tokens"
  // Amazon Titan -> "/textGenerationConfig/maxTokenCount"
  // Anthropic Claude -> "/max_tokens"
  // Cohere Command -> "/max_tokens"
  // Cohere Command R -> "/max_tokens"
  // AI21 Jamba -> "/max_tokens"
  // Meta Llama -> "/max_gen_len"
  // Mistral AI -> "/max_tokens"
  @Nullable
  public String getMaxTokens() {
    Object value =
        BedrockJsonParser.JsonPathResolver.resolvePath(
            jsonBody,
            "/max_tokens",
            "/max_gen_len",
            "/textGenerationConfig/maxTokenCount",
            "inferenceConfig/max_new_tokens");
    return value != null ? String.valueOf(value) : null;
  }

  // Model -> Path Mapping:
  // Amazon Nova -> "/inferenceConfig/temperature"
  // Amazon Titan -> "/textGenerationConfig/temperature"
  // Anthropic Claude -> "/temperature"
  // Cohere Command -> "/temperature"
  // Cohere Command R -> "/temperature"
  // AI21 Jamba -> "/temperature"
  // Meta Llama -> "/temperature"
  // Mistral AI -> "/temperature"
  @Nullable
  public String getTemperature() {
    Object value =
        BedrockJsonParser.JsonPathResolver.resolvePath(
            jsonBody,
            "/temperature",
            "/textGenerationConfig/temperature",
            "inferenceConfig/temperature");
    return value != null ? String.valueOf(value) : null;
  }

  // Model -> Path Mapping:
  // Amazon Nova -> "/inferenceConfig/top_p"
  // Amazon Titan -> "/textGenerationConfig/topP"
  // Anthropic Claude -> "/top_p"
  // Cohere Command -> "/p"
  // Cohere Command R -> "/p"
  // AI21 Jamba -> "/top_p"
  // Meta Llama -> "/top_p"
  // Mistral AI -> "/top_p"
  @Nullable
  public String getTopP() {
    Object value =
        BedrockJsonParser.JsonPathResolver.resolvePath(
            jsonBody, "/top_p", "/p", "/textGenerationConfig/topP", "inferenceConfig/top_p");
    return value != null ? String.valueOf(value) : null;
  }

  // Model -> Path Mapping:
  // Amazon Nova -> "/stopReason"
  // Amazon Titan -> "/results/0/completionReason"
  // Anthropic Claude -> "/stop_reason"
  // Cohere Command -> "/generations/0/finish_reason"
  // Cohere Command R -> "/finish_reason"
  // AI21 Jamba -> "/choices/0/finish_reason"
  // Meta Llama -> "/stop_reason"
  // Mistral AI -> "/outputs/0/stop_reason"
  @Nullable
  public String getFinishReasons() {
    Object value =
        BedrockJsonParser.JsonPathResolver.resolvePath(
            jsonBody,
            "/stopReason",
            "/finish_reason",
            "/stop_reason",
            "/results/0/completionReason",
            "/generations/0/finish_reason",
            "/choices/0/finish_reason",
            "/outputs/0/stop_reason");

    return value != null ? "[" + value + "]" : null;
  }

  // Model -> Path Mapping:
  // Amazon Nova -> "/usage/inputTokens"
  // Amazon Titan -> "/inputTextTokenCount"
  // Anthropic Claude -> "/usage/input_tokens"
  // Cohere Command -> "/prompt"
  // Cohere Command R -> "/message"
  // AI21 Jamba -> "/usage/prompt_tokens"
  // Meta Llama -> "/prompt_token_count"
  // Mistral AI -> "/prompt"
  @Nullable
  public String getInputTokens() {
    // Try direct tokens counts first
    Object directCount =
        BedrockJsonParser.JsonPathResolver.resolvePath(
            jsonBody,
            "/inputTextTokenCount",
            "/prompt_token_count",
            "/usage/input_tokens",
            "/usage/prompt_tokens",
            "/usage/inputTokens");

    if (directCount != null) {
      return String.valueOf(directCount);
    }

    // Fall back to token approximation
    Object approxTokenCount = approximateTokenCount("/prompt", "/message");

    return approxTokenCount != null ? String.valueOf(approxTokenCount) : null;
  }

  // Model -> Path Mapping:
  // Amazon Nova -> "/usage/outputTokens"
  // Amazon Titan -> "/results/0/tokenCount"
  // Anthropic Claude -> "/usage/output_tokens"
  // Cohere Command -> "/generations/0/text"
  // Cohere Command R -> "/text"
  // AI21 Jamba -> "/usage/completion_tokens"
  // Meta Llama -> "/generation_token_count"
  // Mistral AI -> "/outputs/0/text"
  @Nullable
  public String getOutputTokens() {
    // Try direct token counts first
    Object directCount =
        BedrockJsonParser.JsonPathResolver.resolvePath(
            jsonBody,
            "/generation_token_count",
            "/results/0/tokenCount",
            "/usage/output_tokens",
            "/usage/completion_tokens",
            "/usage/outputTokens");

    if (directCount != null) {
      return String.valueOf(directCount);
    }

    // Fall back to token approximation
    Object approxTokenCount = approximateTokenCount("/text", "/outputs/0/text");

    return approxTokenCount != null ? String.valueOf(approxTokenCount) : null;
  }
}
