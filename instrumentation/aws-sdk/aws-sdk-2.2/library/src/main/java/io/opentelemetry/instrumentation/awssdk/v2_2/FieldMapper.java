/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import io.opentelemetry.api.trace.Span;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.utils.StringUtils;

class FieldMapper {

  private final Serializer serializer;
  private final MethodHandleFactory methodHandleFactory;

  FieldMapper() {
    serializer = new Serializer();
    methodHandleFactory = new MethodHandleFactory();
  }

  FieldMapper(Serializer serializer, MethodHandleFactory methodHandleFactory) {
    this.methodHandleFactory = methodHandleFactory;
    this.serializer = serializer;
  }

  void mapToAttributes(SdkRequest sdkRequest, AwsSdkRequest request, Span span) {
    mapToAttributes(
        field -> sdkRequest.getValueForField(field, Object.class).orElse(null),
        FieldMapping.Type.REQUEST,
        request,
        span);
  }

  void mapToAttributes(SdkResponse sdkResponse, AwsSdkRequest request, Span span) {
    mapToAttributes(
        field -> sdkResponse.getValueForField(field, Object.class).orElse(null),
        FieldMapping.Type.RESPONSE,
        request,
        span);
  }

  private void mapToAttributes(
      Function<String, Object> fieldValueProvider,
      FieldMapping.Type type,
      AwsSdkRequest request,
      Span span) {
    for (FieldMapping fieldMapping : request.fields(type)) {
      mapToAttributes(fieldValueProvider, fieldMapping, span);
    }
    for (FieldMapping fieldMapping : request.type().fields(type)) {
      mapToAttributes(fieldValueProvider, fieldMapping, span);
    }
  }

  private void mapToAttributes(
      Function<String, Object> fieldValueProvider, FieldMapping fieldMapping, Span span) {
    // traverse path
    List<String> path = fieldMapping.getFields();
    Object target = fieldValueProvider.apply(path.get(0));
    for (int i = 1; i < path.size() && target != null; i++) {
      target = next(target, path.get(i));
    }
    String attributeName = fieldMapping.getAttribute();
    if (target != null) {
      // NOTE: The attributes have varying data types according to Otel Gen AI semantic conventions.
      // https://github.com/open-telemetry/semantic-conventions/blob/main/docs/gen-ai/gen-ai-spans.md#genai-attributes
      if (AwsExperimentalAttributes.isGenAiInferenceAttribute(attributeName)) {
        switch (attributeName) {
          case "gen_ai.request.temperature":
          case "gen_ai.request.top_p":
            Double doubleVal = serializer.serialize(attributeName, target, Double.class);
            if (doubleVal != null) {
              span.setAttribute(attributeName, doubleVal);
            }
            break;
          case "gen_ai.request.max_tokens":
          case "gen_ai.usage.input_tokens":
          case "gen_ai.usage.output_tokens":
            Integer intVal = serializer.serialize(attributeName, target, Integer.class);
            if (intVal != null) {
              span.setAttribute(attributeName, intVal);
            }
            break;
          case "gen_ai.response.finish_reasons":
            String finishReasons = serializer.serialize(attributeName, target, String.class);
            if (finishReasons != null) {
              // NOTE: setAttribute only support primitive data types so we are restricted to String
              // instead of String[] as recommended by semantic conventions.
              span.setAttribute(attributeName, finishReasons);
            }
            break;
          default:
            String value = serializer.serialize(target);
            if (!StringUtils.isEmpty(value)) {
              span.setAttribute(attributeName, value);
            }
        }
      } else {
        String value = serializer.serialize(target);
        if (!StringUtils.isEmpty(value)) {
          span.setAttribute(attributeName, value);
        }
      }
    }
  }

  @Nullable
  private Object next(Object current, String fieldName) {
    try {
      return methodHandleFactory.forField(current.getClass(), fieldName).invoke(current);
    } catch (Throwable t) {
      // ignore
    }
    return null;
  }
}
