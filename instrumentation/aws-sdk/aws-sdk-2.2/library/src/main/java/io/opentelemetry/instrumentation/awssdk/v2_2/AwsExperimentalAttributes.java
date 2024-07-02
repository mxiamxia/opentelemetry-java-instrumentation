/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

final class AwsExperimentalAttributes {
  static final AttributeKey<String> AWS_BUCKET_NAME = stringKey("aws.bucket.name");
  static final AttributeKey<String> AWS_QUEUE_URL = stringKey("aws.queue.url");
  static final AttributeKey<String> AWS_QUEUE_NAME = stringKey("aws.queue.name");
  static final AttributeKey<String> AWS_STREAM_NAME = stringKey("aws.stream.name");
  static final AttributeKey<String> AWS_TABLE_NAME = stringKey("aws.table.name");
  static final AttributeKey<String> AWS_BEDROCK_GUARDRAIL_ID =
      stringKey("aws.bedrock.guardrail_id");
  static final AttributeKey<String> AWS_BEDROCK_AGENT_ID = stringKey("aws.bedrock.agent_id");
  static final AttributeKey<String> AWS_BEDROCK_DATASOURCE_ID =
      stringKey("aws.bedrock.datasource_id");
  static final AttributeKey<String> AWS_BEDROCK_KNOWLEDGEBASE_ID =
      stringKey("aws.bedrock.knowledgebase_id");
  static final AttributeKey<String> GEN_AI_FINISH_REASON =
      stringKey("gen_ai.response.finish_reason");
  static final AttributeKey<String> GEN_AI_PROMPT_TOKENS = stringKey("gen_ai.usage.prompt_tokens");
  static final AttributeKey<String> GEN_AI_COMPLETION_TOKENS =
      stringKey("gen_ai.usage.completion_tokens");

  // OTel GenAI/LLM group has defined gen_ai attributes but not yet add it in
  // semantic-conventions-java package
  // https://github.com/open-telemetry/semantic-conventions/blob/main/docs/gen-ai/gen-ai-spans.md#genai-attributes
  static final AttributeKey<String> GEN_AI_MODEL = stringKey("gen_ai.request.model");
  static final AttributeKey<String> GEN_AI_TEMPERATURE = stringKey("gen_ai.request.temperature");
  static final AttributeKey<String> GEN_AI_TOP_P = stringKey("gen_ai.request.top_p");
  static final AttributeKey<String> GEN_AI_MAX_TOKENS = stringKey("gen_ai.request.max_tokens");
  static final AttributeKey<String> GEN_AI_SYSTEM = stringKey("gen_ai.system");

  private AwsExperimentalAttributes() {}

  static boolean isGenAiAttribute(String attributeKey) {
    return attributeKey.equals(GEN_AI_MODEL.getKey())
        || attributeKey.equals(GEN_AI_FINISH_REASON.getKey())
        || attributeKey.equals(GEN_AI_PROMPT_TOKENS.getKey())
        || attributeKey.equals(GEN_AI_COMPLETION_TOKENS.getKey())
        || attributeKey.equals(GEN_AI_TEMPERATURE.getKey())
        || attributeKey.equals(GEN_AI_TOP_P.getKey())
        || attributeKey.equals(GEN_AI_MAX_TOKENS.getKey())
        || attributeKey.equals(GEN_AI_SYSTEM.getKey());
  }
}
