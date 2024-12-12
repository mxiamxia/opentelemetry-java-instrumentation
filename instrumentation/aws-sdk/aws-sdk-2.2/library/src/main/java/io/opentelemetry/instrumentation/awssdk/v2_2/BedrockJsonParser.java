/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BedrockJsonParser {

  // Prevent instantiation
  private BedrockJsonParser() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static LlmJson parse(String jsonString) {
    JsonParser parser = new JsonParser(jsonString);
    Map<String, Object> jsonBody = parser.parse();
    return new LlmJson(jsonBody);
  }

  static class JsonParser {
    private final String json;
    private int position;

    public JsonParser(String json) {
      this.json = json.trim();
      this.position = 0;
    }

    private void skipWhitespace() {
      while (position < json.length() && Character.isWhitespace(json.charAt(position))) {
        position++;
      }
    }

    private char currentChar() {
      return json.charAt(position);
    }

    private void expect(char c) {
      skipWhitespace();
      if (currentChar() != c) {
        throw new IllegalArgumentException(
            "Expected '" + c + "' but found '" + currentChar() + "'");
      }
      position++;
    }

    private String readString() {
      skipWhitespace();
      expect('"'); // Ensure the string starts with a quote
      StringBuilder result = new StringBuilder();
      while (currentChar() != '"') {
        // Handle escaped quotes within the string
        if (currentChar() == '\\'
            && position + 1 < json.length()
            && json.charAt(position + 1) == '"') {
          result.append('"');
          position += 2; // Skip the backslash and the escaped quote
        } else {
          result.append(currentChar());
          position++;
        }
      }
      position++; // Skip closing quote
      return result.toString();
    }

    private Object readValue() {
      skipWhitespace();
      char c = currentChar();

      if (c == '"') {
        return readString();
      } else if (Character.isDigit(c)) {
        return readScopedNumber();
      } else if (c == '{') {
        return readObject(); // JSON Objects
      } else if (c == '[') {
        return readArray(); // JSON Arrays
      } else if (json.startsWith("true", position)) {
        position += 4;
        return true;
      } else if (json.startsWith("false", position)) {
        position += 5;
        return false;
      } else if (json.startsWith("null", position)) {
        position += 4;
        return null; // JSON null
      } else {
        throw new IllegalArgumentException("Unexpected character: " + c);
      }
    }

    private Number readScopedNumber() {
      int start = position;

      // Consume digits and the optional decimal point
      while (position < json.length()
          && (Character.isDigit(json.charAt(position)) || json.charAt(position) == '.')) {
        position++;
      }

      String number = json.substring(start, position);

      if (number.contains(".")) {
        double value = Double.parseDouble(number);
        if (value < 0.0 || value > 1.0) {
          throw new IllegalArgumentException(
              "Value out of bounds for Bedrock Floating Point Attribute: " + number);
        }
        return value;
      } else {
        return Integer.parseInt(number);
      }
    }

    private Map<String, Object> readObject() {
      Map<String, Object> map = new HashMap<>();
      expect('{');
      skipWhitespace();
      while (currentChar() != '}') {
        String key = readString();
        expect(':');
        Object value = readValue();
        map.put(key, value);
        skipWhitespace();
        if (currentChar() == ',') {
          position++;
        }
      }
      position++; // Skip closing brace
      return map;
    }

    private List<Object> readArray() {
      List<Object> list = new ArrayList<>();
      expect('[');
      skipWhitespace();
      while (currentChar() != ']') {
        list.add(readValue());
        skipWhitespace();
        if (currentChar() == ',') {
          position++;
        }
      }
      position++;
      return list;
    }

    public Map<String, Object> parse() {
      return readObject();
    }
  }

  // Resolves paths in a JSON structure
  static class JsonPathResolver {

    // Private constructor to prevent instantiation
    private JsonPathResolver() {
      throw new UnsupportedOperationException("Utility class");
    }

    public static Object resolvePath(Map<String, Object> json, String... paths) {
      for (String path : paths) {
        Object value = resolvePath(json, path);
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    private static Object resolvePath(Map<String, Object> json, String path) {
      String[] keys = path.split("/");
      Object current = json;

      for (String key : keys) {
        if (key.isEmpty()) {
          continue;
        }

        if (current instanceof Map) {
          current = ((Map<?, ?>) current).get(key);
        } else if (current instanceof List) {
          try {
            int index = Integer.parseInt(key);
            current = ((List<?>) current).get(index);
          } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
          }
        } else {
          return null;
        }

        if (current == null) {
          return null;
        }
      }
      return current;
    }
  }
}
