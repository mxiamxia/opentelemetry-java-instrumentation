/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_DATASOURCE_ID;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

class BedrockDataSourceOperation extends AbstractBedrockAgentOperation {
  @Override
  public void onStart(
      AttributesBuilder attributes,
      Context parentContext,
      AmazonWebServiceRequest originalRequest) {
    setAttribute(attributes, AWS_DATASOURCE_ID, originalRequest, RequestAccess::getDataSourceId);
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      Request<?> request,
      Object awsResps,
      @Nullable Throwable error) {
    setAttribute(attributes, AWS_DATASOURCE_ID, awsResps, RequestAccess::getDataSourceId);
  }

  @Override
  public List<String> requestClassNames() {
    return Arrays.asList(
        "DeleteDataSourceRequest", "GetDataSourceRequest", "UpdateDataSourceRequest");
  }

  @Override
  public List<String> responseClassNames() {
    return Arrays.asList("DeleteDataSourceResult");
  }
}
