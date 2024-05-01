/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awslambdacore.v1_0.internal;

import com.amazonaws.services.lambda.runtime.Context;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.awslambdacore.v1_0.AwsLambdaRequest;
import io.opentelemetry.semconv.incubating.CloudIncubatingAttributes;
import io.opentelemetry.semconv.incubating.FaasIncubatingAttributes;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class AwsLambdaFunctionAttributesExtractor
    implements AttributesExtractor<AwsLambdaRequest, Object> {

  @Nullable private static final MethodHandle GET_FUNCTION_ARN;

  static {
    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    MethodHandle getFunctionArn;
    try {
      getFunctionArn =
          lookup.findVirtual(
              Context.class, "getInvokedFunctionArn", MethodType.methodType(String.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      getFunctionArn = null;
    }
    GET_FUNCTION_ARN = getFunctionArn;
  }

  // cached accountId value
  private volatile String accountId;

  @Override
  public void onStart(
      AttributesBuilder attributes,
      io.opentelemetry.context.Context parentContext,
      AwsLambdaRequest request) {
    Context awsContext = request.getAwsContext();
    attributes.put(FaasIncubatingAttributes.FAAS_INVOCATION_ID, awsContext.getAwsRequestId());
    String arn = getFunctionArn(awsContext);
    if (arn != null) {
      attributes.put(CloudIncubatingAttributes.CLOUD_RESOURCE_ID, arn);
      attributes.put(CloudIncubatingAttributes.CLOUD_ACCOUNT_ID, getAccountId(arn));
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      io.opentelemetry.context.Context context,
      AwsLambdaRequest request,
      @Nullable Object response,
      @Nullable Throwable error) {}

  @Nullable
  private static String getFunctionArn(Context awsContext) {
    if (GET_FUNCTION_ARN == null) {
      return null;
    }
    try {
      return (String) GET_FUNCTION_ARN.invoke(awsContext);
    } catch (Throwable throwable) {
      return null;
    }
  }

  private String getAccountId(String arn) {
    if (accountId == null) {
      synchronized (this) {
        if (accountId == null) {
          String[] arnParts = arn.split(":");
          if (arnParts.length >= 5) {
            accountId = arnParts[4];
          }
        }
      }
    }
    return accountId;
  }
}
