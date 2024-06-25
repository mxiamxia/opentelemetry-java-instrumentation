/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.json.JSONObject;
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
      JSONObject jsonBody;
      if (target instanceof SdkBytes) {
        jsonBody = new JSONObject(((SdkBytes) target).asUtf8String());
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
    } catch (RuntimeException e) {
      return null;
    }
  }

  private static String getFinishReason(JSONObject body) {
    if (body.has("stop_reason")) {
      return body.getString("stop_reason");
    } else if (body.has("results")) {
      JSONObject result = (JSONObject) body.getJSONArray("results").get(0);
      if (result.has("completionReason")) {
        return result.getString("completionReason");
      }
    }
    return null;
  }

  private static String getInputTokens(JSONObject body) {
    if (body.has("prompt_token_count")) {
      return String.valueOf(body.getInt("prompt_token_count"));
    } else if (body.has("inputTextTokenCount")) {
      return String.valueOf(body.getInt("inputTextTokenCount"));
    } else if (body.has("usage")) {
      JSONObject usage = (JSONObject) body.get("usage");
      if (usage.has("input_tokens")) {
        return String.valueOf(usage.getInt("input_tokens"));
      }
    }
    return null;
  }

  private static String getOutputTokens(JSONObject body) {
    if (body.has("generation_token_count")) {
      return String.valueOf(body.getInt("generation_token_count"));
    } else if (body.has("results")) {
      JSONObject result = (JSONObject) body.getJSONArray("results").get(0);
      if (result.has("tokenCount")) {
        return String.valueOf(result.getInt("tokenCount"));
      }
    } else if (body.has("inputTextTokenCount")) {
      return String.valueOf(body.getInt("inputTextTokenCount"));
    } else if (body.has("usage")) {
      JSONObject usage = (JSONObject) body.get("usage");
      if (usage.has("output_tokens")) {
        return String.valueOf(usage.getInt("output_tokens"));
      }
    }
    return null;
  }

  private static String getTopP(JSONObject body) {
    if (body.has("top_p")) {
      return String.valueOf(body.getFloat("top_p"));
    } else if (body.has("textGenerationConfig")) {
      JSONObject usage = (JSONObject) body.get("textGenerationConfig");
      if (usage.has("topP")) {
        return String.valueOf(usage.getFloat("topP"));
      }
    }
    return null;
  }

  private static String getTemperature(JSONObject body) {
    if (body.has("temperature")) {
      return String.valueOf(body.getFloat("temperature"));
    } else if (body.has("textGenerationConfig")) {
      JSONObject usage = (JSONObject) body.get("textGenerationConfig");
      if (usage.has("temperature")) {
        return String.valueOf(usage.getFloat("temperature"));
      }
    }
    return null;
  }

  private static String getMaxTokens(JSONObject body) {
    if (body.has("max_tokens")) {
      return String.valueOf(body.getInt("max_tokens"));
    } else if (body.has("max_gen_len")) {
      return String.valueOf(body.getInt("max_gen_len"));
    } else if (body.has("textGenerationConfig")) {
      JSONObject usage = (JSONObject) body.get("textGenerationConfig");
      if (usage.has("maxTokenCount")) {
        return String.valueOf(usage.getInt("maxTokenCount"));
      }
    }
    return null;
  }
}
