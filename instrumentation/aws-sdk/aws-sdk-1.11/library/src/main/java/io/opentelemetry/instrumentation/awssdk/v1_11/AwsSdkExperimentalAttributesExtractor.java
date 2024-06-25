/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_ACTIVITY_ARN;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_AGENT;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_BUCKET_NAME;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_ENDPOINT;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_LAMBDA_FUNCTION_NAME;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_LAMBDA_SOURCE_MAPPING_ID;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_QUEUE_NAME;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_QUEUE_URL;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_REQUEST_ID;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_SECRET_ARN;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_STATE_MACHINE_ARN;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_STREAM_CONSUMER_NAME;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_STREAM_NAME;
import static io.opentelemetry.instrumentation.awssdk.v1_11.AwsExperimentalAttributes.AWS_TABLE_NAME;

import com.amazonaws.AmazonWebServiceResponse;
import com.amazonaws.Request;
import com.amazonaws.Response;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.function.Function;
import javax.annotation.Nullable;

class AwsSdkExperimentalAttributesExtractor
    implements AttributesExtractor<Request<?>, Response<?>> {
  private static final String COMPONENT_NAME = "java-aws-sdk";

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, Request<?> request) {
    attributes.put(AWS_AGENT, COMPONENT_NAME);
    attributes.put(AWS_ENDPOINT, request.getEndpoint().toString());

    Object originalRequest = request.getOriginalRequest();
    setAttribute(attributes, AWS_BUCKET_NAME, originalRequest, RequestAccess::getBucketName);
    setAttribute(attributes, AWS_QUEUE_URL, originalRequest, RequestAccess::getQueueUrl);
    setAttribute(attributes, AWS_QUEUE_NAME, originalRequest, RequestAccess::getQueueName);
    setAttribute(attributes, AWS_STREAM_NAME, originalRequest, RequestAccess::getStreamName);
    setAttribute(attributes, AWS_TABLE_NAME, originalRequest, RequestAccess::getTableName);
    setAttribute(
        attributes,
        AWS_STREAM_CONSUMER_NAME,
        originalRequest,
        RequestAccess::getStreamConsumerName);
    setAttribute(attributes, AWS_SECRET_ARN, originalRequest, RequestAccess::getSecretArn);
    setAttribute(
        attributes, AWS_STATE_MACHINE_ARN, originalRequest, RequestAccess::getStateMachineArn);
    setAttribute(attributes, AWS_ACTIVITY_ARN, originalRequest, RequestAccess::getActivityArn);
    setAttribute(
        attributes, AWS_LAMBDA_FUNCTION_NAME, originalRequest, RequestAccess::getFunctionName);
    setAttribute(
        attributes,
        AWS_LAMBDA_SOURCE_MAPPING_ID,
        originalRequest,
        RequestAccess::getResourceEventMappingId);
  }

  private static void setAttribute(
      AttributesBuilder attributes,
      AttributeKey<String> key,
      Object object,
      Function<Object, String> getter) {
    String value = getter.apply(object);
    if (value != null) {
      attributes.put(key, value);
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      Request<?> request,
      @Nullable Response<?> response,
      @Nullable Throwable error) {
    if (response != null) {
      Object awsResps = response.getAwsResponse();
      setAttribute(attributes, AWS_SECRET_ARN, awsResps, RequestAccess::getSecretArn);
      setAttribute(attributes, AWS_STATE_MACHINE_ARN, awsResps, RequestAccess::getStateMachineArn);
      setAttribute(attributes, AWS_ACTIVITY_ARN, awsResps, RequestAccess::getActivityArn);
      setAttribute(attributes, AWS_LAMBDA_FUNCTION_NAME, awsResps, RequestAccess::getFunctionName);
      setAttribute(
          attributes,
          AWS_LAMBDA_SOURCE_MAPPING_ID,
          awsResps,
          RequestAccess::getResourceEventMappingId);
      if (awsResps instanceof AmazonWebServiceResponse) {
        AmazonWebServiceResponse<?> awsResp = (AmazonWebServiceResponse<?>) awsResps;
        String requestId = awsResp.getRequestId();
        if (requestId != null) {
          attributes.put(AWS_REQUEST_ID, requestId);
        }
      }
    }
  }
}
