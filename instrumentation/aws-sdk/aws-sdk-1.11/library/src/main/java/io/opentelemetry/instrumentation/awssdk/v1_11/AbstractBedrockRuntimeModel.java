/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

abstract class AbstractBedrockRuntimeModel {
  public abstract void onStart(
      AttributesBuilder attributes, Context parentContext, AmazonWebServiceRequest request);
  public abstract void onEnd(
      AttributesBuilder attributes,
      Context context,
      Request<?> request,
      Object response,
      @Nullable Throwable error);

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
