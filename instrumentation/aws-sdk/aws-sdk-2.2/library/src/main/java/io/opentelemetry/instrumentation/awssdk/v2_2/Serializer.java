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
import java.util.Optional;
import java.util.stream.Collectors;
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
    if (target instanceof SdkBytes) {
      return serialize((SdkBytes) target);
    }
    // simple type
    return target.toString();
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
  String serialize(String attributeName, Object target) {
    try {
      JsonNode jsonBody;
      if (target instanceof SdkBytes) {
        jsonBody = objectMapper.readTree(((SdkBytes) target).asUtf8String());
      } else {
        if (target != null) {
          return target.toString();
        }
        return null;
      }
      switch (attributeName) {
        case "gen_ai.response.finish_reason":
          return getFinishReason(jsonBody);
        case "gen_ai.usage.completion_tokens":
          return getOutputTokens(jsonBody);
        case "gen_ai.usage.prompt_tokens":
          return getInputTokens(jsonBody);
        case "gen_ai.request.top_p":
          return getTopP(jsonBody);
        case "gen_ai.request.temperature":
          return getTemperature(jsonBody);
        case "gen_ai.request.max_tokens":
          return getMaxTokens(jsonBody);
        default:
          return null;
      }
    } catch (RuntimeException | JsonProcessingException e) {
      throw new IllegalStateException("Failed to instantiate operation class", e);
    }
  }

  private static String getFinishReason(JsonNode body) {
    if (body.has("stop_reason")) {
      return body.get("stop_reason").asText();
    } else if (body.has("results")) {
      JsonNode result = body.get("results").get(0);
      if (result.has("completionReason")) {
        return result.get("completionReason").asText();
      }
    }
    return null;
  }

  private static String getInputTokens(JsonNode body) {
    if (body.has("prompt_token_count")) {
      return String.valueOf(body.get("prompt_token_count").asInt());
    } else if (body.has("inputTextTokenCount")) {
      return String.valueOf(body.get("inputTextTokenCount").asInt());
    } else if (body.has("usage")) {
      JsonNode usage = body.get("usage");
      if (usage.has("input_tokens")) {
        return String.valueOf(usage.get("input_tokens").asInt());
      }
    }
    return null;
  }

  private static String getOutputTokens(JsonNode body) {
    if (body.has("generation_token_count")) {
      return String.valueOf(body.get("generation_token_count").asInt());
    } else if (body.has("results")) {
      JsonNode result = body.get("results").get(0);
      if (result.has("tokenCount")) {
        return String.valueOf(result.get("tokenCount").asInt());
      }
    } else if (body.has("inputTextTokenCount")) {
      return String.valueOf(body.get("inputTextTokenCount").asInt());
    } else if (body.has("usage")) {
      JsonNode usage = body.get("usage");
      if (usage.has("output_tokens")) {
        return String.valueOf(usage.get("output_tokens").asInt());
      }
    }
    return null;
  }

  private static String getTopP(JsonNode body) {
    if (body.has("top_p")) {
      return String.valueOf(body.get("top_p").asDouble());
    } else if (body.has("textGenerationConfig")) {
      JsonNode usage = body.get("textGenerationConfig");
      if (usage.has("topP")) {
        return String.valueOf(usage.get("topP").asInt());
      }
    }
    return null;
  }

  private static String getTemperature(JsonNode body) {
    if (body.has("temperature")) {
      return String.valueOf(body.get("temperature").asDouble());
    } else if (body.has("textGenerationConfig")) {
      JsonNode usage = body.get("textGenerationConfig");
      if (usage.has("temperature")) {
        return String.valueOf(usage.get("temperature").asDouble());
      }
    }
    return null;
  }

  private static String getMaxTokens(JsonNode body) {
    if (body.has("max_tokens")) {
      return String.valueOf(body.get("max_tokens").asInt());
    } else if (body.has("max_gen_len")) {
      return String.valueOf(body.get("max_gen_len").asInt());
    } else if (body.has("textGenerationConfig")) {
      JsonNode usage = body.get("textGenerationConfig");
      if (usage.has("maxTokenCount")) {
        return String.valueOf(usage.get("maxTokenCount").asInt());
      }
    }
    return null;
  }
}
