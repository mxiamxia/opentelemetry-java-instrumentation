/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.Test;

public class BedrockJsonParserTest {

  @Test
  public void shouldParseSimpleObject() {
    // given
    String json = "{\"key\":\"value\",\"number\":123,\"boolean\":true}";

    // when
    LlmJson parsedJson = BedrockJsonParser.parse(json);

    // then
    assertThat(parsedJson.getJsonBody()).containsEntry("key", "value");
    assertThat(parsedJson.getJsonBody()).containsEntry("number", 123);
    assertThat(parsedJson.getJsonBody()).containsEntry("boolean", true);
  }

  @Test
  public void shouldParseNestedObject() {
    // given
    String json = "{\"parent\":{\"child\":\"value\"}}";

    // when
    LlmJson parsedJson = BedrockJsonParser.parse(json);

    // then
    Object parentObj = parsedJson.getJsonBody().get("parent");
    assertThat(parentObj).isInstanceOf(Map.class); // Ensure it's a Map

    @SuppressWarnings("unchecked")
    Map<String, Object> parent = (Map<String, Object>) parentObj;
    assertThat(parent).containsEntry("child", "value");
  }

  @Test
  public void shouldParseEscapeSequences() {
    // given
    String json =
        "{\"escaped\":\"Line1\\nLine2\\tTabbed\\\"Quoted\\\"\\bBackspace\\fFormfeed\\rCarriageReturn\\\\Backslash\\/Slash\\u0041\"}";

    // when
    LlmJson parsedJson = BedrockJsonParser.parse(json);

    // then
    assertThat(parsedJson.getJsonBody())
        .containsEntry(
            "escaped",
            "Line1\nLine2\tTabbed\"Quoted\"\bBackspace\fFormfeed\rCarriageReturn\\Backslash/SlashA");
  }

  @Test
  public void shouldParseUnicodeEscapeSequences() {
    // given
    String json = "{\"unicode\":\"\\u0041\\u0042\\u0043\"}";

    // when
    LlmJson parsedJson = BedrockJsonParser.parse(json);

    // then
    assertThat(parsedJson.getJsonBody()).containsEntry("unicode", "ABC");
  }

  @Test
  public void shouldHandleEmptyObject() {
    // given
    String json = "{}";

    // when
    LlmJson parsedJson = BedrockJsonParser.parse(json);

    // then
    assertThat(parsedJson.getJsonBody()).isEmpty();
  }

  @Test
  public void shouldHandleEmptyArray() {
    // given
    String json = "{\"array\":[]}";

    // when
    LlmJson parsedJson = BedrockJsonParser.parse(json);

    // then
    assertThat(parsedJson.getJsonBody()).containsKey("array");
    assertThat((Iterable<?>) parsedJson.getJsonBody().get("array")).isEmpty();
  }
}
