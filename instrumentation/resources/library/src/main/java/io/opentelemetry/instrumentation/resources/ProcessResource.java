/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.resources;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.SchemaUrls;
import io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Factory of a {@link Resource} which provides information about the current running process. */
public final class ProcessResource {

  // Note: This pattern doesn't support file paths with spaces in them.
  // Important: This is statically used in buildResource, so must be declared/initialized first.
  private static final Pattern JAR_FILE_PATTERN =
      Pattern.compile("^\\S+\\.(jar|war)", Pattern.CASE_INSENSITIVE);

  private static final Resource INSTANCE = buildResource();

  /**
   * Returns a factory for a {@link Resource} which provides information about the current running
   * process.
   */
  public static Resource get() {
    return INSTANCE;
  }

  // Visible for testing
  static Resource buildResource() {
    try {
      return doBuildResource();
    } catch (LinkageError t) {
      // Will only happen on Android, where these attributes generally don't make much sense
      // anyways.
      return Resource.empty();
    }
  }

  private static Resource doBuildResource() {
    AttributesBuilder attributes = Attributes.builder();

    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

    long pid = ProcessPid.getPid();

    if (pid >= 0) {
      attributes.put(ProcessIncubatingAttributes.PROCESS_PID, pid);
    }

    String javaHome = null;
    String osName = null;
    try {
      javaHome = System.getProperty("java.home");
      osName = System.getProperty("os.name");
    } catch (SecurityException e) {
      // Ignore
    }
    if (javaHome != null) {
      StringBuilder executablePath = new StringBuilder(javaHome);
      executablePath
          .append(File.separatorChar)
          .append("bin")
          .append(File.separatorChar)
          .append("java");
      if (osName != null && osName.toLowerCase(Locale.ROOT).startsWith("windows")) {
        executablePath.append(".exe");
      }

      attributes.put(
          ProcessIncubatingAttributes.PROCESS_EXECUTABLE_PATH, executablePath.toString());

      String[] args = ProcessArguments.getProcessArguments();
      // This will only work with Java 9+ but provides everything except the executablePath.
      if (args.length > 0) {
        List<String> commandArgs = new ArrayList<>(args.length + 1);
        commandArgs.add(executablePath.toString());
        commandArgs.addAll(Arrays.asList(args));
        attributes.put(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS, commandArgs);
      } else { // Java 8
        StringBuilder commandLine = new StringBuilder(executablePath);
        for (String arg : runtime.getInputArguments()) {
          commandLine.append(' ').append(arg);
        }
        // sun.java.command isn't well document and may not be available on all systems.
        String javaCommand = System.getProperty("sun.java.command");
        if (javaCommand != null) {
          // This property doesn't include -jar when launching a jar directly.  Try to determine
          // if that's the case and add it back in.
          if (JAR_FILE_PATTERN.matcher(javaCommand).matches()) {
            commandLine.append(" -jar");
          }
          commandLine.append(' ').append(javaCommand);
        }
        attributes.put(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE, commandLine.toString());
      }
    }

    return Resource.create(attributes.build(), SchemaUrls.V1_24_0);
  }

  private ProcessResource() {}
}
