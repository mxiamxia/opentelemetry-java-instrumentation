/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes
import io.opentelemetry.semconv.ErrorAttributes
import io.opentelemetry.semconv.HttpAttributes
import spock.lang.Unroll

import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HEAD
import javax.ws.rs.OPTIONS
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path

import static io.opentelemetry.api.trace.SpanKind.SERVER
import static io.opentelemetry.instrumentation.test.utils.ClassUtils.getClassName

class JaxRsAnnotations1InstrumentationTest extends AgentInstrumentationSpecification {

  @Unroll
  def "span named '#paramName' from annotations on class '#className' when is not root span"() {
    setup:
    runWithHttpServerSpan {
      obj.call()
    }

    expect:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "GET " + paramName
          kind SERVER
          hasNoParent()
          attributes {
            "$HttpAttributes.HTTP_REQUEST_METHOD" "GET"
            "$HttpAttributes.HTTP_ROUTE" paramName
            "$ErrorAttributes.ERROR_TYPE" "_OTHER"
          }
        }
        span(1) {
          name "${className}.call"
          childOf span(0)
          attributes {
            "$CodeIncubatingAttributes.CODE_NAMESPACE" obj.getClass().getName()
            "$CodeIncubatingAttributes.CODE_FUNCTION" "call"
          }
        }
      }
    }

    when: "multiple calls to the same method"
    runWithHttpServerSpan {
      (1..10).each {
        obj.call()
      }
    }
    then: "doesn't increase the cache size"

    where:
    paramName      | obj
    "/a"           | new Jax() {
      @Path("/a")
      void call() {
      }
    }
    "/b"           | new Jax() {
      @GET
      @Path("/b")
      void call() {
      }
    }
    "/interface/c" | new InterfaceWithPath() {
      @POST
      @Path("/c")
      void call() {
      }
    }
    "/interface"   | new InterfaceWithPath() {
      @HEAD
      void call() {
      }
    }
    "/abstract/d"  | new AbstractClassWithPath() {
      @POST
      @Path("/d")
      void call() {
      }
    }
    "/abstract"    | new AbstractClassWithPath() {
      @PUT
      void call() {
      }
    }
    "/child/e"     | new ChildClassWithPath() {
      @OPTIONS
      @Path("/e")
      void call() {
      }
    }
    "/child/call"  | new ChildClassWithPath() {
      @DELETE
      void call() {
      }
    }
    "/child/call"  | new ChildClassWithPath()
    "/child/call"  | new JavaInterfaces.ChildClassOnInterface()
    "/child/call"  | new JavaInterfaces.DefaultChildClassOnInterface()

    className = getClassName(obj.class)
  }

  def "no annotations has no effect"() {
    setup:
    runWithHttpServerSpan {
      obj.call()
    }

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "GET"
          kind SERVER
          attributes {
            "$HttpAttributes.HTTP_REQUEST_METHOD" "GET"
            "$ErrorAttributes.ERROR_TYPE" "_OTHER"
          }
        }
      }
    }

    where:
    obj | _
    new Jax() {
      void call() {
      }
    }   | _
  }

  interface Jax {
    void call()
  }

  @Path("/interface")
  interface InterfaceWithPath extends Jax {
    @GET
    void call()
  }

  @Path("/abstract")
  static abstract class AbstractClassWithPath implements Jax {
    @PUT
    abstract void call()
  }

  @Path("child")
  static class ChildClassWithPath extends AbstractClassWithPath {
    @Path("call")
    @POST
    void call() {
    }
  }
}
