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
  String serialize(String attributeName, Object target) {
    try {
      // Extract JSON string from target if it is a Bedrock Runtime JSON blob
      String jsonString;
      if (target instanceof SdkBytes) {
        jsonString = ((SdkBytes) target).asUtf8String();
      } else {
        if (target != null) {
          return target.toString();
        }
        return null;
      }

      // Parse the LLM JSON string into a Map
      LlmJson llmJson = BedrockJsonParser.parse(jsonString);

      // Use attribute name to extract the corresponding value
      switch (attributeName) {
        case "gen_ai.request.max_tokens":
          return llmJson.getMaxTokens();
        case "gen_ai.request.temperature":
          return llmJson.getTemperature();
        case "gen_ai.request.top_p":
          return llmJson.getTopP();
        case "gen_ai.response.finish_reasons":
          return llmJson.getFinishReasons();
        case "gen_ai.usage.input_tokens":
          return llmJson.getInputTokens();
        case "gen_ai.usage.output_tokens":
          return llmJson.getOutputTokens();
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
}
