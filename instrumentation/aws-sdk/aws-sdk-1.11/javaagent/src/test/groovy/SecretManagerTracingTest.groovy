/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import spock.lang.Shared

import static io.opentelemetry.api.trace.SpanKind.CLIENT

class SecretManagerTracingTest extends AgentInstrumentationSpecification {

  @Shared
  AwsConnector awsConnector = AwsConnector.localstack()

  def cleanupSpec() {
    awsConnector.disconnect()
  }

  def "Secret Manager secret operations and tracing"() {
    setup:
    String secretName = "testSecret"
    String secretValue = "testValue"

    // Create a secret
    String secretARN = awsConnector.createSecret(secretName, secretValue)

    // Describe the secret
    def secretDescription = awsConnector.describeSecret(secretARN)

    when:
    secretDescription != null

    then:
    assertTraces(2) {
      trace(0, 1) {
        span(0) {
          name "AWSSecretsManager.CreateSecret"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "aws.secretsmanager.secret_arn" secretARN
            "rpc.method" "CreateSecret"
            "rpc.system" "aws-api"
            "rpc.service" "AWSSecretsManager"
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
          name "AWSSecretsManager.DescribeSecret"
          kind CLIENT
          hasNoParent()
          attributes {
            "aws.agent" "java-aws-sdk"
            "aws.endpoint" String
            "aws.secretsmanager.secret_arn" secretARN
            "rpc.method" "DescribeSecret"
            "rpc.system" "aws-api"
            "rpc.service" "AWSSecretsManager"
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
