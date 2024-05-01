/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package test

import io.opentelemetry.instrumentation.test.base.HttpServerTest
import jakarta.ws.rs.ApplicationPath
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.Suspended
import jakarta.ws.rs.core.Application
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import jakarta.ws.rs.ext.ExceptionMapper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CyclicBarrier

import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.CAPTURE_HEADERS
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.ERROR
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.EXCEPTION
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.INDEXED_CHILD
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.PATH_PARAM
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.QUERY_PARAM
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.REDIRECT
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.SUCCESS
import static java.util.concurrent.TimeUnit.SECONDS

@Path("")
class JaxRsTestResource {
  @Path("/success")
  @GET
  String success() {
    HttpServerTest.controller(SUCCESS) {
      SUCCESS.body
    }
  }

  @Path("query")
  @GET
  String query_param(@QueryParam("some") String param) {
    HttpServerTest.controller(QUERY_PARAM) {
      "some=$param"
    }
  }

  @Path("redirect")
  @GET
  Response redirect(@Context UriInfo uriInfo) {
    HttpServerTest.controller(REDIRECT) {
      Response.status(Response.Status.FOUND)
        .location(uriInfo.relativize(new URI(REDIRECT.body)))
        .build()
    }
  }

  @Path("error-status")
  @GET
  Response error() {
    HttpServerTest.controller(ERROR) {
      Response.status(ERROR.status)
        .entity(ERROR.body)
        .build()
    }
  }

  @Path("exception")
  @GET
  Object exception() {
    HttpServerTest.controller(EXCEPTION) {
      throw new Exception(EXCEPTION.body)
    }
  }

  @Path("path/{id}/param")
  @GET
  String path_param(@PathParam("id") int id) {
    HttpServerTest.controller(PATH_PARAM) {
      id
    }
  }

  @GET
  @Path("captureHeaders")
  Response capture_headers(@HeaderParam("X-Test-Request") String header) {
    HttpServerTest.controller(CAPTURE_HEADERS) {
      Response.status(CAPTURE_HEADERS.status)
        .header("X-Test-Response", header)
        .entity(CAPTURE_HEADERS.body)
        .build()
    }
  }

  @Path("/child")
  @GET
  void indexed_child(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {
    def parameters = uriInfo.queryParameters

    CompletableFuture.runAsync({
      HttpServerTest.controller(INDEXED_CHILD) {
        INDEXED_CHILD.collectSpanAttributes { parameters.getFirst(it) }
        response.resume("")
      }
    })
  }

  static final BARRIER = new CyclicBarrier(2)

  @Path("async")
  @GET
  void asyncOp(@Suspended AsyncResponse response, @QueryParam("action") String action) {
    CompletableFuture.runAsync({
      // await for the test method to verify that there are no spans yet
      BARRIER.await(10, SECONDS)

      switch (action) {
        case "succeed":
          response.resume("success")
          break
        case "throw":
          response.resume(new Exception("failure"))
          break
        case "cancel":
          response.cancel()
          break
        default:
          response.resume(new AssertionError((Object) ("invalid action value: " + action)))
          break
      }
    })
  }

  @Path("async-completion-stage")
  @GET
  CompletionStage<String> jaxRs21Async(@QueryParam("action") String action) {
    def result = new CompletableFuture<String>()
    CompletableFuture.runAsync({
      // await for the test method to verify that there are no spans yet
      BARRIER.await(10, SECONDS)

      switch (action) {
        case "succeed":
          result.complete("success")
          break
        case "throw":
          result.completeExceptionally(new Exception("failure"))
          break
        default:
          result.completeExceptionally(new AssertionError((Object) ("invalid action value: " + action)))
          break
      }
    })
    result
  }
}

@Path("test-resource-super")
class JaxRsSuperClassTestResource extends JaxRsSuperClassTestResourceSuper {
}

class JaxRsSuperClassTestResourceSuper {
  @GET
  Object call() {
    HttpServerTest.controller(SUCCESS) {
      SUCCESS.body
    }
  }
}

class JaxRsInterfaceClassTestResource extends JaxRsInterfaceClassTestResourceSuper implements JaxRsInterface {
}

@Path("test-resource-interface")
interface JaxRsInterface {
  @Path("call")
  @GET
  Object call()
}

class JaxRsInterfaceClassTestResourceSuper {
  Object call() {
    HttpServerTest.controller(SUCCESS) {
      SUCCESS.body
    }
  }
}

@Path("test-sub-resource-locator")
class JaxRsSubResourceLocatorTestResource {
  @Path("call")
  Object call() {
    HttpServerTest.controller(SUCCESS) {
      return new SubResource()
    }
  }
}

class SubResource {
  @Path("sub")
  @GET
  String call() {
    HttpServerTest.controller(SUCCESS) {
      SUCCESS.body
    }
  }
}

class JaxRsTestExceptionMapper implements ExceptionMapper<Exception> {
  @Override
  Response toResponse(Exception exception) {
    return Response.status(500)
      .entity(exception.message)
      .build()
  }
}

class JaxRsTestApplication extends Application {
  @Override
  Set<Class<?>> getClasses() {
    def classes = new HashSet()
    classes.add(JaxRsTestResource)
    classes.add(JaxRsSuperClassTestResource)
    classes.add(JaxRsInterfaceClassTestResource)
    classes.add(JaxRsSubResourceLocatorTestResource)
    classes.add(JaxRsTestExceptionMapper)
    return classes
  }
}

@ApplicationPath("/rest-app")
class JaxRsApplicationPathTestApplication extends JaxRsTestApplication {
}
