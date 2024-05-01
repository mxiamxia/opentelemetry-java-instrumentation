plugins {
  id("otel.java-conventions")
  id("org.springframework.boot") version "3.2.5"
  id("org.graalvm.buildtools.native")
}

description = "smoke-tests-otel-starter-spring-boot-3"

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_17)
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
  runtimeOnly("com.h2database:h2")
  implementation("org.apache.commons:commons-dbcp2")
  implementation(project(":instrumentation:jdbc:library"))
  implementation("org.springframework.kafka:spring-kafka") // not tested here, just make sure there are no warnings when it's included
  implementation("io.opentelemetry:opentelemetry-extension-trace-propagators")
  implementation(project(":instrumentation:spring:starters:spring-boot-starter"))
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(project(":smoke-tests-otel-starter:spring-smoke-testing"))
}

tasks {
  test {
    // suppress warning about byte-buddy-agent being loaded dynamically
    jvmArgs("-XX:+EnableDynamicAgentLoading")
  }
  compileAotJava {
    with(options) {
      compilerArgs.add("-Xlint:-deprecation,-unchecked,none")
      // To disable warnings/failure coming from the Java compiler during the Spring AOT processing
      // -deprecation,-unchecked and none are required (none is not enough)
    }
  }
  compileAotTestJava {
    with(options) {
      compilerArgs.add("-Xlint:-deprecation,-unchecked,none")
      // To disable warnings/failure coming from the Java compiler during the Spring AOT processing
      // -deprecation,-unchecked and none are required (none is not enough)
    }
  }
  checkstyleAot {
    isEnabled = false
  }
  checkstyleAotTest {
    isEnabled = false
  }
}

// To be able to execute the tests as GraalVM native executables
configurations.configureEach {
  exclude("org.apache.groovy", "groovy")
  exclude("org.apache.groovy", "groovy-json")
  exclude("org.spockframework", "spock-core")
}

graalvmNative {
  binaries.all {
    // Workaround for https://github.com/junit-team/junit5/issues/3405
    buildArgs.add("--initialize-at-build-time=org.junit.platform.launcher.core.LauncherConfig")
    buildArgs.add("--initialize-at-build-time=org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter")
  }

  // See https://github.com/graalvm/native-build-tools/issues/572
  metadataRepository {
    enabled.set(false)
  }

  tasks.test {
    useJUnitPlatform()
    setForkEvery(1)
  }
}
