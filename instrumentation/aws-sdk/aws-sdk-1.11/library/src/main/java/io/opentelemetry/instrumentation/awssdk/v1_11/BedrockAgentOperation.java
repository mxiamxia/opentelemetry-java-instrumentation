/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_AGENT_ID;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

class BedrockAgentOperation extends AbstractBedrockAgentOperation {
  @Override
  public void onStart(
      AttributesBuilder attributes,
      Context parentContext,
      AmazonWebServiceRequest originalRequest) {
    setAttribute(attributes, AWS_AGENT_ID, originalRequest, RequestAccess::getAgentId);
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      Request<?> request,
      @Nullable Object awsResps,
      @Nullable Throwable error) {
    setAttribute(attributes, AWS_AGENT_ID, awsResps, RequestAccess::getAgentId);
  }

  @Override
  public List<String> requestClassNames() {
    return Arrays.asList(
        "CreateAgentActionGroupRequest",
        "CreateAgentAliasRequest",
        "DeleteAgentActionGroupRequest",
        "DeleteAgentAliasRequest",
        "DeleteAgentRequest",
        "DeleteAgentVersionRequest",
        "GetAgentActionGroupRequest",
        "GetAgentAliasRequest",
        "GetAgentRequest",
        "GetAgentVersionRequest",
        "ListAgentActionGroupsRequest",
        "ListAgentAliasesRequest",
        "ListAgentKnowledgeBasesRequest",
        "ListAgentVersionsRequest",
        "PrepareAgentRequest",
        "UpdateAgentActionGroupRequest",
        "UpdateAgentAliasRequest",
        "UpdateAgentRequest");
  }

  @Override
  public List<String> responseClassNames() {
    return Arrays.asList(
        "DeleteAgentAliasResult",
        "DeleteAgentResult",
        "DeleteAgentVersionResult",
        "PrepareAgentResult");
  }
}
