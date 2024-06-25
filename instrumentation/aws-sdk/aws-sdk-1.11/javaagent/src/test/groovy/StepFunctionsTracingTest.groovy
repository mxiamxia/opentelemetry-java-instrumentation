/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import spock.lang.Shared

import static io.opentelemetry.api.trace.SpanKind.CLIENT

class StepFunctionsTracingTest extends AgentInstrumentationSpecification {

  @Shared
  AwsConnector awsConnector = AwsConnector.localstack()

  def cleanupSpec() {
    awsConnector.disconnect()
  }

  def "Step Functions state machine operations and tracing"() {
    setup:
    String stateMachineName = "testStateMachine"
    String stateMachineDefinition = """
      {
        "Comment": "A simple AWS Step Functions state machine",
        "StartAt": "HelloWorld",
        "States": {
          "HelloWorld": {
            "Type": "Pass",
            "Result": "Hello, World!",
            "End": true
          }
        }
      }
    """
    String roleName = "stateMachineRole"
    String policy = """
      {
        "Version": "2012-10-17",
        "Statement": [
          {
            "Effect": "Allow",
            "Principal": {
              "Service": "states.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
          }
        ]
      }
    """

    // Create a role
    String roleARN = awsConnector.createRole(roleName, policy)
    String policyArn = "arn:aws:iam::aws:policy/service-role/AWSLambdaRole"
    awsConnector.attachRolePolicy(roleName, policyArn)

    // Create a state machine
    String stateMachineARN = awsConnector.createStateMachine(stateMachineName, stateMachineDefinition, roleARN)

    // Describe the state machine
    def stateMachineDescription = awsConnector.describeStateMachine(stateMachineARN)


    when:
    stateMachineDescription != null

    then:
    assertTraces(4) {
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
          name "AWSStepFunctions.CreateStateMachine"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "aws.stepfunctions.state_machine_arn" stateMachineARN
            "rpc.method" "CreateStateMachine"
            "rpc.system" "aws-api"
            "rpc.service" "AWSStepFunctions"
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
      trace(3, 1) {
        span(0) {
          name "AWSStepFunctions.DescribeStateMachine"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "aws.stepfunctions.state_machine_arn" stateMachineARN
            "rpc.method" "DescribeStateMachine"
            "rpc.system" "aws-api"
            "rpc.service" "AWSStepFunctions"
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
