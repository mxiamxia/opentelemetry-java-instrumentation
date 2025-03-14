/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.lettuce.v5_1;

import io.lettuce.core.RedisClient;
import io.lettuce.core.resource.ClientResources;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

class LettuceSyncClientTest extends AbstractLettuceSyncClientTest {
  @RegisterExtension
  static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  @Override
  public InstrumentationExtension testing() {
    return testing;
  }

  @Override
  protected RedisClient createClient(String uri) {
    return RedisClient.create(
        ClientResources.builder()
            .tracing(LettuceTelemetry.create(testing().getOpenTelemetry()).newTracing())
            .build(),
        uri);
  }
}
