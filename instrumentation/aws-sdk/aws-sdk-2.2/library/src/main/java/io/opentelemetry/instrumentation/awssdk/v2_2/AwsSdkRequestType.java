/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static io.opentelemetry.instrumentation.awssdk.v2_2.FieldMapping.request;
import static io.opentelemetry.instrumentation.awssdk.v2_2.FieldMapping.response;

import java.util.Collections;
import java.util.List;
import java.util.Map;

enum AwsSdkRequestType {
  S3(request("aws.bucket.name", "Bucket")),
  SQS(request("aws.queue.url", "QueueUrl"), request("aws.queue.name", "QueueName")),
  KINESIS(
      request("aws.stream.name", "StreamName"),
      request("aws.stream.arn", "StreamARN"),
      request("aws.stream.consumer_name", "ConsumerName")),
  DYNAMODB(request("aws.table.name", "TableName")),
  SNS(
      /*
       * Only one of TopicArn and TargetArn are permitted on an SNS request.
       */
      request("messaging.destination.name", "TargetArn"),
      request("messaging.destination.name", "TopicArn")),
  SECRETSMANAGER(response("aws.secretsmanager.secret_arn", "ARN")),

  STEPFUNCTION(
      request("aws.stepfunctions.state_machine_arn", "stateMachineArn"),
      request("aws.stepfunctions.activity_arn", "activityArn")),
  BEDROCK(request("aws.bedrock.guardrail_id", "guardrailIdentifier")),
  BEDROCKAGENT(
      request("aws.bedrock.agent_id", "agentId"),
      request("aws.bedrock.datasource_id", "dataSourceId"),
      request("aws.bedrock.knowledgebase_id", "knowledgeBaseId")),

  LAMBDA(
      request("aws.lambda.function_name", "FunctionName"),
      request("aws.lambda.resource_mapping_id", "UUID"));

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
