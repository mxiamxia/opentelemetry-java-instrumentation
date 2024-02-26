/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.coralclient;

import com.amazon.coral.service.HttpConstant;
import com.amazon.coral.service.Job;
import com.amazon.coral.service.ServiceConstant;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesGetter;

public class CoralAttributesGetter implements AttributesGetter<Job>, CodeAttributesGetter<Job> {

  @Override
  public Class<?> getCodeClass(Job job) {
    return job.getClass();
  }

  @Override
  public String getMethodName(Job job) {
    String operation = job.getRequest().getAttribute(ServiceConstant.SERVICE_OPERATION_NAME);
//    System.out.println("CoralAttributesGetter - OTel operation:: " + operation);
    return operation;
  }

  @Override
  public String getServerAddress(Job job) {
    String serverAddress = job.getRequest().getAttribute(HttpConstant.HTTP_HOST);
//    System.out.println("CoralAttributesGetter - OTel serverAddress:: " + serverAddress);
    return serverAddress;
  }

  @Override
  public String getUrlPath(Job job) {
    return job.getRequest().getAttribute(HttpConstant.HTTP_TARGET);
  }

  @Override
  public String getClientAddress(Job job) {
    return job.getRequest().getAttribute(HttpConstant.HTTP_CLIENT_IP);
  }

  @Override
  public String getHttpMethod(Job job) {
    return job.getRequest().getAttribute(HttpConstant.HTTP_VERB);
  }

  @Override
  public String getUserAgent(Job job) {
    return job.getRequest().getAttribute(HttpConstant.HTTP_USER_AGENT);
  }

  @Override
  public String getHttpSchema(Job job) {
    return job.getRequest().getAttribute(HttpConstant.HTTP_PROTOCOL);
  }

  @Override
  public String getNetPeerIp(Job job) {
    return job.getRequest().getAttribute(HttpConstant.HTTP_REMOTE_ADDRESS);
  }

  @Override
  public String getServiceName(Job job) {
    return job.getRequest().getAttribute(ServiceConstant.SERVICE_NAME);
  }

  public Integer getHttpResponseStatusCode(Job job) {
    Integer statusCode = job.getReply().getAttribute(HttpConstant.HTTP_STATUS_CODE);
//    System.out.println("CoralAttributesGetter - OTel statusCode:: " + statusCode);
    return statusCode;
  }
}
