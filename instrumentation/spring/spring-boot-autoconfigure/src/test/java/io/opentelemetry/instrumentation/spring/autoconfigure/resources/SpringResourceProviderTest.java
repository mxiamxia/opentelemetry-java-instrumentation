/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.resources;

import io.opentelemetry.instrumentation.spring.autoconfigure.OpenTelemetryAutoConfiguration;
import io.opentelemetry.instrumentation.spring.autoconfigure.properties.OtelResourceProperties;
import io.opentelemetry.instrumentation.spring.autoconfigure.properties.OtlpExporterProperties;
import io.opentelemetry.instrumentation.spring.autoconfigure.properties.PropagationProperties;
import io.opentelemetry.instrumentation.spring.autoconfigure.properties.SpringConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.testing.assertj.AttributesAssert;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.semconv.ServiceAttributes;
import java.util.Collections;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

public class SpringResourceProviderTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withPropertyValues(
              "otel.traces.exporter=none", "otel.metrics.exporter=none", "otel.logs.exporter=none")
          .withConfiguration(AutoConfigurations.of(OpenTelemetryAutoConfiguration.class));

  @Test
  @DisplayName(
      "when spring.application.name is set value should be passed to service name attribute")
  void shouldDetermineServiceNameBySpringApplicationName() {
    this.contextRunner
        .withPropertyValues("spring.application.name=myapp-backend")
        .run(
            context ->
                assertResourceAttributes(context)
                    .containsEntry(ServiceAttributes.SERVICE_NAME, "myapp-backend"));
  }

  @Test
  @DisplayName(
      "when spring.application.name is set value should be passed to service name attribute")
  void shouldDetermineServiceNameAndVersionBySpringApplicationVersion() {
    Properties properties = new Properties();
    properties.put("name", "demo");
    properties.put("version", "0.3");
    this.contextRunner
        .withBean("buildProperties", BuildProperties.class, () -> new BuildProperties(properties))
        .run(
            context ->
                assertResourceAttributes(context)
                    .containsEntry(ServiceAttributes.SERVICE_NAME, "demo")
                    .containsEntry(ServiceAttributes.SERVICE_VERSION, "0.3"));
  }

  private static AttributesAssert assertResourceAttributes(AssertableApplicationContext context) {
    ConfigProperties configProperties =
        SpringConfigProperties.create(
            context.getBean(Environment.class),
            new OtlpExporterProperties(),
            new OtelResourceProperties(),
            new PropagationProperties(),
            DefaultConfigProperties.createFromMap(Collections.emptyMap()));

    return OpenTelemetryAssertions.assertThat(
        context
            .getBean(SpringResourceProvider.class)
            .createResource(configProperties)
            .getAttributes());
  }
}
