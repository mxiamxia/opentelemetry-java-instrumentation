/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4.common;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Iterator;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public abstract class AbstractNettyChannelPipelineInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("io.netty.channel.ChannelPipeline");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("io.netty.channel.ChannelPipeline"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("remove").or(named("replace")))
            .and(takesArgument(0, named("io.netty.channel.ChannelHandler"))),
        AbstractNettyChannelPipelineInstrumentation.class.getName() + "$RemoveAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(named("remove").or(named("replace"))).and(takesArgument(0, String.class)),
        AbstractNettyChannelPipelineInstrumentation.class.getName() + "$RemoveByNameAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(named("remove").or(named("replace"))).and(takesArgument(0, Class.class)),
        AbstractNettyChannelPipelineInstrumentation.class.getName() + "$RemoveByClassAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(named("removeFirst")).and(returns(named("io.netty.channel.ChannelHandler"))),
        AbstractNettyChannelPipelineInstrumentation.class.getName() + "$RemoveFirstAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(named("removeLast")).and(returns(named("io.netty.channel.ChannelHandler"))),
        AbstractNettyChannelPipelineInstrumentation.class.getName() + "$RemoveLastAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("addAfter"))
            .and(takesArgument(1, String.class))
            .and(takesArguments(4)),
        AbstractNettyChannelPipelineInstrumentation.class.getName() + "$AddAfterAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(named("toMap")).and(takesArguments(0)).and(returns(Map.class)),
        AbstractNettyChannelPipelineInstrumentation.class.getName() + "$ToMapAdvice");
  }

  @SuppressWarnings("unused")
  public static class RemoveAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void removeHandler(
        @Advice.This ChannelPipeline pipeline, @Advice.Argument(0) ChannelHandler handler) {
      VirtualField<ChannelHandler, ChannelHandler> virtualField =
          VirtualField.find(ChannelHandler.class, ChannelHandler.class);
      ChannelHandler ourHandler = virtualField.get(handler);
      if (ourHandler != null) {
        if (pipeline.context(ourHandler) != null) {
          pipeline.remove(ourHandler);
        }
        virtualField.set(handler, null);
      }
    }
  }

  @SuppressWarnings("unused")
  public static class RemoveByNameAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void removeHandler(
        @Advice.This ChannelPipeline pipeline, @Advice.Argument(0) String name) {
      ChannelHandler handler = pipeline.get(name);
      if (handler == null) {
        return;
      }

      VirtualField<ChannelHandler, ChannelHandler> virtualField =
          VirtualField.find(ChannelHandler.class, ChannelHandler.class);
      ChannelHandler ourHandler = virtualField.get(handler);
      if (ourHandler != null) {
        if (pipeline.context(ourHandler) != null) {
          pipeline.remove(ourHandler);
        }
        virtualField.set(handler, null);
      }
    }
  }

  @SuppressWarnings("unused")
  public static class RemoveByClassAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void removeHandler(
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(0) Class<ChannelHandler> handlerClass) {
      ChannelHandler handler = pipeline.get(handlerClass);
      if (handler == null) {
        return;
      }

      VirtualField<ChannelHandler, ChannelHandler> virtualField =
          VirtualField.find(ChannelHandler.class, ChannelHandler.class);
      ChannelHandler ourHandler = virtualField.get(handler);
      if (ourHandler != null) {
        if (pipeline.context(ourHandler) != null) {
          pipeline.remove(ourHandler);
        }
        virtualField.set(handler, null);
      }
    }
  }

  @SuppressWarnings("unused")
  public static class RemoveFirstAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void removeHandler(
        @Advice.This ChannelPipeline pipeline, @Advice.Return ChannelHandler handler) {
      VirtualField<ChannelHandler, ChannelHandler> virtualField =
          VirtualField.find(ChannelHandler.class, ChannelHandler.class);
      ChannelHandler ourHandler = virtualField.get(handler);
      if (ourHandler != null) {
        if (pipeline.context(ourHandler) != null) {
          pipeline.remove(ourHandler);
        }
        virtualField.set(handler, null);
      }
    }
  }

  @SuppressWarnings("unused")
  public static class RemoveLastAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void removeHandler(
        @Advice.This ChannelPipeline pipeline,
        @Advice.Return(readOnly = false) ChannelHandler handler) {
      VirtualField<ChannelHandler, ChannelHandler> virtualField =
          VirtualField.find(ChannelHandler.class, ChannelHandler.class);
      ChannelHandler ourHandler = virtualField.get(handler);
      if (ourHandler != null) {
        // Context is null when our handler has already been removed. This happens when calling
        // removeLast first removed our handler and we called removeLast again to remove the http
        // handler.
        if (pipeline.context(ourHandler) != null) {
          pipeline.remove(ourHandler);
        }
        virtualField.set(handler, null);
      } else {
        String handlerClassName = handler.getClass().getName();
        if (handlerClassName.endsWith("TracingHandler")
            && (handlerClassName.startsWith("io.opentelemetry.javaagent.instrumentation.netty.")
                || handlerClassName.startsWith("io.opentelemetry.instrumentation.netty."))) {
          handler = pipeline.removeLast();
        }
      }
    }
  }

  @SuppressWarnings("unused")
  public static class AddAfterAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void addAfterHandler(
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(value = 1, readOnly = false) String name) {
      ChannelHandler handler = pipeline.get(name);
      if (handler != null) {
        VirtualField<ChannelHandler, ChannelHandler> virtualField =
            VirtualField.find(ChannelHandler.class, ChannelHandler.class);
        ChannelHandler ourHandler = virtualField.get(handler);
        if (ourHandler != null) {
          name = ourHandler.getClass().getName();
        }
      }
    }
  }

  @SuppressWarnings("unused")
  public static class ToMapAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void wrapIterator(@Advice.Return Map<String, ChannelHandler> map) {
      VirtualField<ChannelHandler, ChannelHandler> virtualField =
          VirtualField.find(ChannelHandler.class, ChannelHandler.class);
      for (Iterator<ChannelHandler> iterator = map.values().iterator(); iterator.hasNext(); ) {
        ChannelHandler handler = iterator.next();
        String handlerClassName = handler.getClass().getName();
        if (handlerClassName.endsWith("TracingHandler")
            && (handlerClassName.startsWith("io.opentelemetry.javaagent.instrumentation.netty.")
                || handlerClassName.startsWith("io.opentelemetry.instrumentation.netty."))) {
          iterator.remove();
        }
      }
    }
  }
}
