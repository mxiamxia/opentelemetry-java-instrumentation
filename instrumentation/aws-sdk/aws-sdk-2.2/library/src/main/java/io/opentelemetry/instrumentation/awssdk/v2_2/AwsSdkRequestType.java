/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.AWS_BEDROCK_AGENT_ID;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.AWS_BEDROCK_DATASOURCE_ID;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.AWS_BEDROCK_GUARDRAIL_ID;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.AWS_BEDROCK_KNOWLEDGEBASE_ID;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.AWS_BUCKET_NAME;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.AWS_QUEUE_NAME;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.AWS_QUEUE_URL;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.AWS_STREAM_NAME;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.AWS_TABLE_NAME;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.GEN_AI_COMPLETION_TOKENS;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.GEN_AI_FINISH_REASON;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.GEN_AI_MAX_TOKENS;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.GEN_AI_MODEL;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.GEN_AI_PROMPT_TOKENS;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.GEN_AI_TEMPERATURE;
import static io.opentelemetry.instrumentation.awssdk.v2_2.AwsExperimentalAttributes.GEN_AI_TOP_P;
import static io.opentelemetry.instrumentation.awssdk.v2_2.FieldMapping.request;
import static io.opentelemetry.instrumentation.awssdk.v2_2.FieldMapping.response;

import java.util.Collections;
import java.util.List;
import java.util.Map;

enum AwsSdkRequestType {
  S3(request(AWS_BUCKET_NAME.getKey(), "Bucket")),
  SQS(request(AWS_QUEUE_URL.getKey(), "QueueUrl"), request(AWS_QUEUE_NAME.getKey(), "QueueName")),
  KINESIS(request(AWS_STREAM_NAME.getKey(), "StreamName")),
  DYNAMODB(request(AWS_TABLE_NAME.getKey(), "TableName")),
  BEDROCK(request(AWS_BEDROCK_GUARDRAIL_ID.getKey(), "guardrailIdentifier")),
  BEDROCKAGENT(
      request(AWS_BEDROCK_AGENT_ID.getKey(), "agentId"),
      request(AWS_BEDROCK_DATASOURCE_ID.getKey(), "dataSourceId"),
      request(AWS_BEDROCK_KNOWLEDGEBASE_ID.getKey(), "knowledgeBaseId")),
  BEDROCKRUNTIME(
      request(GEN_AI_MODEL.getKey(), "modelId"),
      request(GEN_AI_TEMPERATURE.getKey(), "body"),
      request(GEN_AI_TOP_P.getKey(), "body"),
      request(GEN_AI_MAX_TOKENS.getKey(), "body"),
      response(GEN_AI_FINISH_REASON.getKey(), "body"),
      response(GEN_AI_PROMPT_TOKENS.getKey(), "body"),
      response(GEN_AI_COMPLETION_TOKENS.getKey(), "body"));

  // Wrapping in unmodifiableMap
  @SuppressWarnings("ImmutableEnumChecker")
  private final Map<FieldMapping.Type, List<FieldMapping>> fields;

  AwsSdkRequestType(FieldMapping... fieldMappings) {
    this.fields = Collections.unmodifiableMap(FieldMapping.groupByType(fieldMappings));
  }

  List<FieldMapping> fields(FieldMapping.Type type) {
    return fields.get(type);
  }
}
