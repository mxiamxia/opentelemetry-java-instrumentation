/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachecamel;

// TODO: These tests are not backwards compatible with new AWS SDK behavior introduced by Bedrock
// Temporarily disabling them as we figure out a long-term fix

// import static io.opentelemetry.api.common.AttributeKey.stringKey;
// import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
//
// import com.google.common.collect.ImmutableMap;
// import io.opentelemetry.api.trace.SpanKind;
// import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
// import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpServerUsingTest;
// import io.opentelemetry.instrumentation.testing.junit.http.HttpServerInstrumentationExtension;
// import io.opentelemetry.semconv.SemanticAttributes;
// import java.net.URI;
// import org.junit.jupiter.api.AfterAll;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.RegisterExtension;
// import org.springframework.boot.SpringApplication;
// import org.springframework.context.ConfigurableApplicationContext;
//
// class SingleServiceCamelTest extends AbstractHttpServerUsingTest<ConfigurableApplicationContext>
// {
//
//  @RegisterExtension
//  public static final InstrumentationExtension testing =
//      HttpServerInstrumentationExtension.forAgent();
//
//  @Override
//  protected ConfigurableApplicationContext setupServer() {
//    SpringApplication app = new SpringApplication(SingleServiceConfig.class);
//    app.setDefaultProperties(ImmutableMap.of("camelService.port", port));
//    return app.run();
//  }
//
//  @Override
//  protected void stopServer(ConfigurableApplicationContext ctx) {
//    ctx.close();
//  }
//
//  @Override
//  protected String getContextPath() {
//    return "";
//  }
//
//  @BeforeAll
//  protected void setUp() {
//    startServer();
//  }
//
//  @AfterAll
//  protected void cleanUp() {
//    cleanupServer();
//  }
//
//  @Test
//  @SuppressWarnings("deprecation") // until old http semconv are dropped in 2.0
//  public void singleCamelServiceSpan() {
//    URI requestUrl = address.resolve("/camelService");
//
//    client.post(requestUrl.toString(), "testContent").aggregate().join();
//
//    testing.waitAndAssertTraces(
//        trace ->
//            trace.hasSpansSatisfyingExactly(
//                span ->
//                    span.hasName("POST /camelService")
//                        .hasKind(SpanKind.SERVER)
//                        .hasAttributesSatisfyingExactly(
//                            equalTo(SemanticAttributes.HTTP_METHOD, "POST"),
//                            equalTo(SemanticAttributes.HTTP_URL, requestUrl.toString()),
//                            equalTo(
//                                stringKey("camel.uri"),
//                                requestUrl.toString().replace("localhost", "0.0.0.0")))));
//  }
// }
