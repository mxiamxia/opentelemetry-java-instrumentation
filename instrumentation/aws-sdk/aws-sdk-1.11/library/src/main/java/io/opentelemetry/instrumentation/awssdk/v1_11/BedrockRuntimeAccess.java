/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

 package io.opentelemetry.instrumentation.awssdk.v1_11;

 import java.lang.reflect.Method;
 import java.nio.ByteBuffer;
 import java.nio.charset.StandardCharsets;
 import javax.annotation.Nullable;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.json.JSONPointer;
 
 final class BedrockRuntimeAccess {
   private BedrockRuntimeAccess() {}
 
   @Nullable
   static String getMaxTokens(Object request) {
     if (request == null) {
       return null;
     }
 
     ByteBuffer bodyBuffer = getBody(request);
     if (bodyBuffer == null) {
       return null;
     }
 
     try {
       JSONObject jsonBody = parseBody(bodyBuffer);
       Object maxTokens = new JSONPointer("/max_tokens").queryFrom(jsonBody);
       return maxTokens != null ? maxTokens.toString() : null;
     } catch (JSONException e) {
       return null;
     }
   }
 
   private static JSONObject parseBody(ByteBuffer bodyBuffer) {
     bodyBuffer.rewind();
     byte[] bytes = new byte[bodyBuffer.remaining()];
     bodyBuffer.get(bytes);
     String bodyString = new String(bytes, StandardCharsets.UTF_8);
     return new JSONObject(bodyString);
   }

   private static ByteBuffer getBody(Object obj) {
     Object current = obj;
     if (current == null) {
       return null;
     }
     try {
       Method method = current.getClass().getMethod("getBody");
       current = method.invoke(current);
     } catch (Exception e) {
       return null;
     }
     return (current instanceof ByteBuffer) ? (ByteBuffer) current : null;
   }
 }
