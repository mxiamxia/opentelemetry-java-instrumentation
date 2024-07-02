/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_KNOWLEDGEBASE_ID;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

class BedrockKnowledgeBaseOperation extends AbstractBedrockAgentOperation {
  @Override
  public void onStart(
      AttributesBuilder attributes,
      Context parentContext,
      AmazonWebServiceRequest originalRequest) {
    setAttribute(
        attributes, AWS_KNOWLEDGEBASE_ID, originalRequest, RequestAccess::getKnowledgeBaseId);
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      Request<?> request,
      Object awsResps,
      @Nullable Throwable error) {
    setAttribute(attributes, AWS_KNOWLEDGEBASE_ID, awsResps, RequestAccess::getKnowledgeBaseId);
  }

  @Override
  public List<String> requestClassNames() {
    return Arrays.asList(
        "AssociateAgentKnowledgeBaseRequest",
        "CreateDataSourceRequest",
        "DeleteKnowledgeBaseRequest",
        "DisassociateAgentKnowledgeBaseRequest",
        "GetAgentKnowledgeBaseRequest",
        "GetKnowledgeBaseRequest",
        "ListDataSourcesRequest",
        "UpdateAgentKnowledgeBaseRequest");
  }

  @Override
  public List<String> responseClassNames() {
    return Arrays.asList("DeleteKnowledgeBaseResult");
  }
}
