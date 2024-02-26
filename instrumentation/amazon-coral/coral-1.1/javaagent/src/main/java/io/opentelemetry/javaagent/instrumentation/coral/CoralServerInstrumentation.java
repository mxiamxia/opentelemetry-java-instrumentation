package io.opentelemetry.javaagent.instrumentation.coral;

import com.amazon.coral.service.HttpConstant;
import com.amazon.coral.service.Job;
import com.amazon.coral.service.ServiceConstant;
//import com.amazon.coral.service.http.HttpHeaders;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static io.opentelemetry.javaagent.instrumentation.coral.CoralSingletons.instrumenter;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

public class CoralServerInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.amazon.coral.service.HttpHandler");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("before"))
            .and(takesArgument(0, named("com.amazon.coral.service.Job"))),
        this.getClass().getName() + "$CoralReqBeforeAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("after"))
            .and(takesArgument(0, named("com.amazon.coral.service.Job"))),
        this.getClass().getName() + "$CoralReqAfterAdvice");
  }

  @SuppressWarnings("unused")
  public static class CoralReqBeforeAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void methodExit(
        @Advice.Argument(0) Job job,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      System.out.println("Coral server instrumentation - OnMethodExit for before()");
      boolean isClientRequest = job.getAttribute(ServiceConstant.CLIENT_REQUEST) != null;
      System.out.println("Coral server instrumentation - isClientRequest: " + isClientRequest);
      String operationName = job.getRequest().getAttribute(
          ServiceConstant.SERVICE_OPERATION_NAME);
      System.out.println("coral exit Before method start:" + operationName);
      if (operationName == null) {
        return;
      }

//      HttpHeaders headers = job.getRequest().getAttribute(HttpConstant.HTTP_HEADERS);
//      System.out.println("DEBUG: HTTP header = " + headers.toString());
//      headers.getHeaderNames().forEach(name -> {
//        System.out.println("DEBUG: HTTP header name = " + name);
//      });


//      Context parentContext = Java8BytecodeBridge.currentContext();
      // TODO: fix the current context cleanup work
      Context parentContext = Context.root();
//      System.out.println("coral exit Before method context start: " + parentContext.toString());
      if (!instrumenter().shouldStart(parentContext, job)) {
        return;
      }
      context = instrumenter().start(parentContext, job);
      scope = context.makeCurrent();
//      System.out.println("coral exit Before method end: " + context.toString());
    }
  }

  @SuppressWarnings("unused")
  public static class CoralReqAfterAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(0) Job job,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      System.out.println("DEBUG: Coral server instrumentation - OnMethodEnter for after()");
      String operationName = job.getRequest().getAttribute(
          ServiceConstant.SERVICE_OPERATION_NAME);
      System.out.println("coral enter After method start:" + operationName);
      if (operationName == null) {
        return;
      }
      Context parentContext = Java8BytecodeBridge.currentContext();
      scope = parentContext.makeCurrent();
      if (scope == null) {
        System.out.println("coral enter After method end : scope is null");
        return;
      }
      Span span = Span.fromContext(parentContext);
      try {
        scope.close();
        instrumenter().end(parentContext, job, job, job.getFailure());
      } catch (Throwable e) {
        System.out.println("End span in error: " + e.getMessage());
      }
      System.out.println("coral trace id: " + span.getSpanContext().getTraceId());
      job.getMetrics().addProperty("AwsXRayTraceId", span.getSpanContext().getTraceId());
      System.out.println("coral enter After method end");
    }
  }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void methodExit(
        @Advice.Argument(0) Job job,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      System.out.println("DEBUG: Coral server instrumentation - OnMethodExit for after()");
      System.out.println("coral exit After method end: " + context.toString());
//      if (scope == null) {
//        System.out.println("coral exit After method end : scope is null");
//        return;
//      }
//      scope.close();
//
//      instrumenter().end(context, job, null, throwable);
//
//      System.out.println("coral exit After method end: " + context.toString());
    }
}
