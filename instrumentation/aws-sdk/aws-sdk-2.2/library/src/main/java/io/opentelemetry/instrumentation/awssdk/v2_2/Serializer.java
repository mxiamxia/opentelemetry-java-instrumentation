/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.json.JSONPointerException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;

class Serializer {

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
    if (target instanceof SdkBytes) {
      return serialize((SdkBytes) target);
    }
    // simple type
    return target.toString();
  }

  @Nullable
  @SuppressWarnings("unchecked")
  <T> T serialize(String attributeName, Object target, Class<T> returnType) {
    try {
      JSONObject jsonBody;
      if (target instanceof SdkBytes) {
        jsonBody = new JSONObject(((SdkBytes) target).asUtf8String());
      } else {
        return null;
      }
      switch (attributeName) {
        case "gen_ai.request.temperature":
          return (T) getTemperature(jsonBody);
        case "gen_ai.request.max_tokens":
          return (T) getMaxTokens(jsonBody);
        case "gen_ai.request.top_p":
          return (T) getTopP(jsonBody);
        case "gen_ai.usage.input_tokens":
          return (T) getInputTokens(jsonBody);
        case "gen_ai.usage.output_tokens":
          return (T) getOutputTokens(jsonBody);
        case "gen_ai.response.finish_reasons":
          return (T) getFinishReasons(jsonBody);
        default:
          return null;
      }
    } catch (RuntimeException e) {
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

  private static Double getTemperature(JSONObject body) {
    // NOTE: There is no way to get the model id information without a large-effort refactor. As a
    // workaround we try every possible path and take whichever value is not null (there will only
    // ever exist one non-null value at a time).
    // Model -> Path Mapping:
    // Amazon Titan -> "/textGenerationConfig/temperature"
    // Anthropic Claude -> "/temperature"
    // Cohere Command -> "/temperature"
    // Cohere Command R -> "/temperature"
    // AI21 Jamba -> "/temperature"
    // Meta Llama -> "/temperature"
    // Mistral AI -> "/temperature"
    return Stream.of("/textGenerationConfig/temperature", "/temperature")
        .map(
            path -> {
              try {
                Object val = new JSONPointer(path).queryFrom(body);
                return val != null ? Double.valueOf(val.toString()) : null;
              } catch (JSONPointerException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static Integer getMaxTokens(JSONObject body) {
    // Model -> Path Mapping:
    // Amazon Titan -> "/textGenerationConfig/maxTokenCount"
    // Anthropic Claude -> "/max_tokens"
    // Cohere Command -> "/max_tokens"
    // Cohere Command R -> "/max_tokens"
    // AI21 Jamba -> "/max_tokens"
    // Meta Llama -> "/max_gen_len"
    // Mistral AI -> "/max_tokens"
    return Stream.of("/textGenerationConfig/maxTokenCount", "/max_tokens", "/max_gen_len")
        .map(
            path -> {
              try {
                Object val = new JSONPointer(path).queryFrom(body);
                return val != null ? Integer.valueOf(val.toString()) : null;
              } catch (JSONPointerException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static Double getTopP(JSONObject body) {
    // Model -> Path Mapping:
    // Amazon Titan -> "/textGenerationConfig/topP"
    // Anthropic Claude -> "/top_p"
    // Cohere Command -> "/p"
    // Cohere Command R -> "/p"
    // AI21 Jamba -> "/top_p"
    // Meta Llama -> "/top_p"
    // Mistral AI -> "/top_p"
    return Stream.of("/textGenerationConfig/topP", "/top_p", "/p")
        .map(
            path -> {
              try {
                Object val = new JSONPointer(path).queryFrom(body);
                return val != null ? Double.valueOf(val.toString()) : null;
              } catch (JSONPointerException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static Integer getInputTokens(JSONObject body) {
    // Model -> Path Mapping:
    // Amazon Titan -> "/inputTextTokenCount"
    // Anthropic Claude -> "/usage/input_tokens"
    // Cohere Command -> "/prompt"
    // Cohere Command R -> "/message"
    // AI21 Jamba -> "/usage/prompt_tokens"
    // Meta Llama -> "/prompt_token_count"
    // Mistral AI -> "/prompt"
    Integer directTokenCount =
        Stream.of(
                "/inputTextTokenCount",
                "/usage/input_tokens",
                "/usage/prompt_tokens",
                "/prompt_token_count")
            .map(
                path -> {
                  try {
                    Object val = new JSONPointer(path).queryFrom(body);
                    return val != null ? Integer.valueOf(val.toString()) : null;
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
                Object val = new JSONPointer(path).queryFrom(body);
                // NOTE: For some models, the token count is not directly available. In these cases
                // we approximate the token count with (total_chars / 6). This is recommended by the
                // Bedrock docs for pricing purposes.
                // https://docs.aws.amazon.com/bedrock/latest/userguide/model-customization-prepare.html
                return val != null ? (int) Math.ceil(val.toString().length() / 6.0) : null;
              } catch (JSONPointerException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static Integer getOutputTokens(JSONObject body) {
    // Model -> Path Mapping:
    // Amazon Titan -> "/results/0/tokenCount"
    // Anthropic Claude -> "/usage/output_tokens"
    // Cohere Command -> "/generations/0/text"
    // Cohere Command R -> "/text"
    // AI21 Jamba -> "/usage/completion_tokens"
    // Meta Llama -> "/generation_token_count"
    // Mistral AI -> "/outputs/0/text"
    Integer directTokenCount =
        Stream.of(
                "/results/0/tokenCount",
                "/usage/output_tokens",
                "/usage/completion_tokens",
                "/generation_token_count")
            .map(
                path -> {
                  try {
                    Object val = new JSONPointer(path).queryFrom(body);
                    return val != null ? Integer.valueOf(val.toString()) : null;
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

    return Stream.of("/generations/0/text", "/outputs/0/text", "/text")
        .map(
            path -> {
              try {
                Object val = new JSONPointer(path).queryFrom(body);
                // NOTE: For some models, the token count is not directly available. In these cases
                // we approximate the token count with (total_chars / 6). This is recommended by the
                // Bedrock docs for pricing purposes.
                // https://docs.aws.amazon.com/bedrock/latest/userguide/model-customization-prepare.html
                return val != null ? (int) Math.ceil(val.toString().length() / 6.0) : null;
              } catch (JSONPointerException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static String getFinishReasons(JSONObject body) {
    // Model -> Path Mapping:
    // Amazon Titan -> "/results/0/completionReason"
    // Anthropic Claude -> "/stop_reason"
    // Cohere Command -> "/generations/0/finish_reason"
    // Cohere Command R -> "/finish_reason"
    // AI21 Jamba -> "/choices/0/finish_reason"
    // Meta Llama -> "/stop_reason"
    // Mistral AI -> "/outputs/0/stop_reason"
    return Stream.of(
            "/results/0/completionReason",
            "/stop_reason",
            "/generations/0/finish_reason",
            "/choices/0/finish_reason",
            "/outputs/0/stop_reason",
            "/finish_reason")
        .map(
            path -> {
              try {
                Object val = new JSONPointer(path).queryFrom(body);
                // NOTE: the Span class defined in upstream Otel core library does not support
                // String[] attribute
                // values at the moment. However, String[] is recommended by the Otel Gen AI
                // semantic
                // conventions so we assign this string literal which looks like an array for now.
                // TODO: Change type to String[] once it is supported.
                return val != null ? ("[" + val.toString() + "]") : null;
              } catch (JSONPointerException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }
}
