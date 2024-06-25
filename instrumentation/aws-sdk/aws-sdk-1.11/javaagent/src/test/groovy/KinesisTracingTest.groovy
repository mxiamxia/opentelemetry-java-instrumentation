/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import spock.lang.Shared

import static io.opentelemetry.api.trace.SpanKind.CLIENT

class KinesisTracingTest extends AgentInstrumentationSpecification {

  @Shared
  AwsConnector awsConnector = AwsConnector.localstack()

  def cleanupSpec() {
    awsConnector.disconnect()
  }

  def "Kinesis stream operations and tracing"() {
    setup:
    String streamName = "testStream"
    Integer shardCount = 1
    String consumerName = "testConsumer"

    // Create a stream
    String streamARN = awsConnector.createStream(streamName, shardCount)

    // Register a consumer
    String consumerARN = awsConnector.registerStreamConsumer(streamARN, consumerName)

    when:
    consumerARN != null

    then:
    assertTraces(3) {
      trace(0, 1) {
        span(0) {
          name "Kinesis.CreateStream"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "aws.stream.name" streamName
            "rpc.method" "CreateStream"
            "rpc.system" "aws-api"
            "rpc.service" "AmazonKinesis"
            "http.method" "POST"
            "http.response_content_length" { it == null || Number }
            "http.request_content_length" { it == null || Number }
            "http.status_code" 200
            "http.url" String
            "net.peer.name" String
            "net.peer.port" { it == null || Number }
            "net.protocol.name" String
            "net.protocol.version" String
          }
        }
      }
      trace(1, 1) {
        span(0) {
          name "Kinesis.DescribeStream"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "rpc.method" "DescribeStream"
            "aws.stream.name" streamName
            "rpc.system" "aws-api"
            "rpc.service" "AmazonKinesis"
            "http.method" "POST"
            "http.response_content_length" { it == null || Number }
            "http.request_content_length" { it == null || Number }
            "http.status_code" 200
            "http.url" String
            "net.peer.name" String
            "net.peer.port" { it == null || Number }
            "net.protocol.name" String
            "net.protocol.version" String
          }
        }
      }
      trace(2, 1) {
        span(0) {
          name "Kinesis.RegisterStreamConsumer"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "rpc.method" "RegisterStreamConsumer"
            "rpc.system" "aws-api"
            "rpc.service" "AmazonKinesis"
            "aws.stream.consumer_name" consumerName
            "http.method" "POST"
            "http.response_content_length" { it == null || Number }
            "http.request_content_length" { it == null || Number }
            "http.status_code" 200
            "http.url" String
            "net.peer.name" String
            "net.peer.port" { it == null || Number }
            "net.protocol.name" String
            "net.protocol.version" String
          }
        }
      }
    }
  }
}
