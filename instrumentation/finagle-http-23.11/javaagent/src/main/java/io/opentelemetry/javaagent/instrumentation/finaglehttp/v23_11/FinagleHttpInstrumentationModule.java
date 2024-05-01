/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.finaglehttp.v23_11;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Arrays;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class FinagleHttpInstrumentationModule extends InstrumentationModule {

  public FinagleHttpInstrumentationModule() {
    super("finagle-http", "finagle-http-23.11");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(
        new GenStreamingServerDispatcherInstrumentation(),
        new ChannelTransportInstrumentation(),
        new H2StreamChannelInitInstrumentation());
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.equals("com.twitter.finagle.ChannelTransportHelpers")
        || className.equals("io.netty.channel.OpenTelemetryChannelInitializerDelegate");
  }
}
