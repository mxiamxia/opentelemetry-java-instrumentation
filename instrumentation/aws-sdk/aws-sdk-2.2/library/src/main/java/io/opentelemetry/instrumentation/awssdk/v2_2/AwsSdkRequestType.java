/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static io.opentelemetry.instrumentation.awssdk.v2_2.FieldMapping.request;

import io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Map;

enum AwsSdkRequestType {
  S3(request("aws.bucket.name", "Bucket")),
  SQS(request("aws.queue.url", "QueueUrl"), request("aws.queue.name", "QueueName")),
  KINESIS(request("aws.stream.name", "StreamName")),
  DYNAMODB(request("aws.table.name", "TableName")),
  SNS(
      /*
       * Only one of TopicArn and TargetArn are permitted on an SNS request.
       */
      request(MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME.getKey(), "TargetArn"),
      request(MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME.getKey(), "TopicArn"));

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
