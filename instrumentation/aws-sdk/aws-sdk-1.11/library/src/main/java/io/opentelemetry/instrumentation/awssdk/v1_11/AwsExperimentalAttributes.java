/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

final class AwsExperimentalAttributes {
  static final AttributeKey<String> AWS_AGENT = stringKey("aws.agent");
  static final AttributeKey<String> AWS_ENDPOINT = stringKey("aws.endpoint");
  static final AttributeKey<String> AWS_BUCKET_NAME = stringKey("aws.bucket.name");
  static final AttributeKey<String> AWS_QUEUE_URL = stringKey("aws.queue.url");
  static final AttributeKey<String> AWS_QUEUE_NAME = stringKey("aws.queue.name");
  static final AttributeKey<String> AWS_STREAM_NAME = stringKey("aws.stream.name");
  static final AttributeKey<String> AWS_STREAM_CONSUMER_ARN = stringKey("aws.kinesis.consumer_arn");
  static final AttributeKey<String> AWS_TABLE_NAME = stringKey("aws.table.name");
  static final AttributeKey<String> AWS_TOPIC_ARN = stringKey("aws.sns.topic_arn");
  static final AttributeKey<String> AWS_SECRET_ARN = stringKey("aws.secretsmanager.secret_arn");
  static final AttributeKey<String> AWS_STATE_MACHINE_ARN = stringKey("aws.stepfunctions.state_machine_arn");
  static final AttributeKey<String> AWS_REQUEST_ID = stringKey("aws.requestId");

  private AwsExperimentalAttributes() {}
}
