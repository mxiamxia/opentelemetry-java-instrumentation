/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import io.opentelemetry.semconv.SemanticAttributes
import spock.lang.Shared

import static io.opentelemetry.api.trace.SpanKind.CLIENT

class LambdaTracingTest extends AgentInstrumentationSpecification {

  @Shared
  AwsConnector awsConnector = AwsConnector.localstack()

  def cleanupSpec() {
    awsConnector.disconnect()
  }

  def "Lambda operations and tracing"() {
    setup:
    String roleName = "lambda-execution-role"
    String rolePolicy = """
      {
        "Version": "2012-10-17",
        "Statement": [
          {
            "Effect": "Allow",
            "Principal": {
              "Service": "lambda.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
          }
        ]
      }
    """
    String lambdaPolicyArn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
    String policyName = "LambdaSQSPolicy"
    String lammbdaPolicyDocument = """
      {
        "Version": "2012-10-17",
        "Statement": [
          {
            "Effect": "Allow",
            "Action": [
              "sqs:ReceiveMessage",
              "sqs:DeleteMessage",
              "sqs:GetQueueAttributes"
            ],
            "Resource": "*"
          }
        ]
      }
    """
    String handler = "lambda_function.lambda_handler"

    // Create a role
    String roleARN = awsConnector.createRole(roleName, rolePolicy)
    awsConnector.attachRolePolicy(roleName, lambdaPolicyArn)
    awsConnector.putRolePolicy(roleName, policyName, lammbdaPolicyDocument)

    // Create a Lambda function
    String functionName = "testFunction"
    awsConnector.createFunction(functionName, roleARN, handler)

    // Create an SQS queue
    String queueName = "testQueue"
    String queueUrl = awsConnector.createQueue(queueName)
    String queueArn = awsConnector.getQueueArn(queueUrl)

    def functionConfiguration = awsConnector.getFunction(functionName)

    // Create an event source mapping
    String uuid = awsConnector.createEventSourceMapping(functionName, queueArn)
    def eventSourceMapping = awsConnector.getEventSourceMapping(uuid)

    when:
    functionConfiguration != null && eventSourceMapping != null

    then:
    assertTraces(9) {
      trace(0, 1) {
        span(0) {
          name "IdentityManagement.CreateRole"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "rpc.method" "CreateRole"
            "rpc.system" "aws-api"
            "rpc.service" "AmazonIdentityManagement"
            "http.method" "POST"
            "http.response_content_length" { it == null || Number }
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
          name "IdentityManagement.AttachRolePolicy"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "rpc.method" "AttachRolePolicy"
            "rpc.system" "aws-api"
            "rpc.service" "AmazonIdentityManagement"
            "http.method" "POST"
            "http.response_content_length" { it == null || Number }
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
          name "IdentityManagement.PutRolePolicy"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "rpc.method" "PutRolePolicy"
            "rpc.system" "aws-api"
            "rpc.service" "AmazonIdentityManagement"
            "http.method" "POST"
            "http.response_content_length" { it == null || Number }
            "http.status_code" 200
            "http.url" String
            "net.peer.name" String
            "net.peer.port" { it == null || Number }
            "net.protocol.name" String
            "net.protocol.version" String
          }
        }
      }
      trace(3, 1) {
        span(0) {
          name "AWSLambda.CreateFunction"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "aws.lambda.function_name" functionName
            "rpc.method" "CreateFunction"
            "rpc.system" "aws-api"
            "rpc.service" "AWSLambda"
            "http.method" "POST"
            "http.response_content_length" { it == null || Number }
            "http.request_content_length" { it == null || Number }
            "http.status_code" 201
            "http.url" String
            "net.peer.name" String
            "net.peer.port" { it == null || Number }
            "net.protocol.name" String
            "net.protocol.version" String
          }
        }
      }
      trace(4, 1) {
        span(0) {
          name "SQS.CreateQueue"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "rpc.method" "CreateQueue"
            "aws.queue.name" queueName
            "rpc.system" "aws-api"
            "rpc.service" "AmazonSQS"
            "http.method" "POST"
            "http.status_code" 200
            "http.url" String
            "net.peer.name" String
            "$SemanticAttributes.NET_PROTOCOL_NAME" "http"
            "$SemanticAttributes.NET_PROTOCOL_VERSION" "1.1"
            "net.peer.port" { it == null || Number }
            "$SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH" Long
          }
        }
      }
      trace(5, 1) {
        span(0) {
          name "SQS.GetQueueAttributes"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "rpc.method" "GetQueueAttributes"
            "aws.queue.url" queueUrl
            "rpc.system" "aws-api"
            "rpc.service" "AmazonSQS"
            "http.method" "POST"
            "http.status_code" 200
            "http.url" String
            "net.peer.name" String
            "$SemanticAttributes.NET_PROTOCOL_NAME" "http"
            "$SemanticAttributes.NET_PROTOCOL_VERSION" "1.1"
            "net.peer.port" { it == null || Number }
            "$SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH" Long
          }
        }
      }
      trace(6, 1) {
        span(0) {
          name "AWSLambda.GetFunction"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "aws.lambda.function_name" functionName
            "rpc.method" "GetFunction"
            "rpc.system" "aws-api"
            "rpc.service" "AWSLambda"
            "http.method" "GET"
            "http.response_content_length" { it == null || Number }
            "http.status_code" 200
            "http.url" String
            "net.peer.name" String
            "net.peer.port" { it == null || Number }
            "net.protocol.name" String
            "net.protocol.version" String
          }
        }
      }
      trace(7, 1) {
        span(0) {
          name "AWSLambda.CreateEventSourceMapping"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "aws.lambda.function_name" functionName
            "aws.lambda.resource_mapping_id" uuid
            "rpc.method" "CreateEventSourceMapping"
            "rpc.system" "aws-api"
            "rpc.service" "AWSLambda"
            "http.method" "POST"
            "http.response_content_length" { it == null || Number }
            "http.request_content_length" { it == null || Number }
            "http.status_code" 202
            "http.url" String
            "net.peer.name" String
            "net.peer.port" { it == null || Number }
            "net.protocol.name" String
            "net.protocol.version" String
          }
        }
      }
      trace(8, 1) {
        span(0) {
          name "AWSLambda.GetEventSourceMapping"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "aws.lambda.resource_mapping_id" uuid
            "rpc.method" "GetEventSourceMapping"
            "rpc.system" "aws-api"
            "rpc.service" "AWSLambda"
            "http.method" "GET"
            "http.response_content_length" { it == null || Number }
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
