/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;

abstract class AbstractBedrockRuntimeModel {
  public abstract void onStart(
      AttributesBuilder attributes, Context parentContext, AmazonWebServiceRequest request)
      throws JsonProcessingException;

  public abstract void onEnd(
      AttributesBuilder attributes,
      Context context,
      Request<?> request,
      Object response,
      @Nullable Throwable error)
      throws JsonProcessingException;

  public abstract List<String> modelNames();

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
