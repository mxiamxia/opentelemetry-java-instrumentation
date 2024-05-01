/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import io.opentelemetry.instrumentation.netty.v4_1.ClientHandler
import io.opentelemetry.instrumentation.test.AgentTestTrait
import io.opentelemetry.instrumentation.test.InstrumentationSpecification
import io.opentelemetry.instrumentation.test.utils.PortUtils
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientTestServer
import io.opentelemetry.semconv.ServerAttributes
import io.opentelemetry.semconv.NetworkAttributes
import spock.lang.Shared

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import static io.opentelemetry.api.trace.SpanKind.CLIENT
import static io.opentelemetry.api.trace.SpanKind.INTERNAL
import static io.opentelemetry.api.trace.SpanKind.SERVER
import static io.opentelemetry.api.trace.StatusCode.ERROR

class Netty41ConnectionSpanTest extends InstrumentationSpecification implements AgentTestTrait {

  @Shared
  private HttpClientTestServer server

  @Shared
  private EventLoopGroup eventLoopGroup = new NioEventLoopGroup()

  @Shared
  private Bootstrap bootstrap = buildBootstrap()

  def setupSpec() {
    server = new HttpClientTestServer(openTelemetry)
    server.start()
  }

  def cleanupSpec() {
    eventLoopGroup.shutdownGracefully()
    server.stop()
  }

  Bootstrap buildBootstrap() {
    Bootstrap bootstrap = new Bootstrap()
    bootstrap.group(eventLoopGroup)
      .channel(NioSocketChannel)
      .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
          ChannelPipeline pipeline = socketChannel.pipeline()
          pipeline.addLast(new HttpClientCodec())
        }
      })

    return bootstrap
  }

  DefaultFullHttpRequest buildRequest(String method, URI uri, Map<String, String> headers) {
    def request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(method), uri.path, Unpooled.EMPTY_BUFFER)
    request.headers().set(HttpHeaderNames.HOST, uri.host + ":" + uri.port)
    headers.each { k, v -> request.headers().set(k, v) }
    return request
  }

  int sendRequest(DefaultFullHttpRequest request, URI uri) {
    def channel = bootstrap.connect(uri.host, uri.port).sync().channel()
    def result = new CompletableFuture<Integer>()
    channel.pipeline().addLast(new ClientHandler(result))
    channel.writeAndFlush(request).get()
    return result.get(20, TimeUnit.SECONDS)
  }

  def "test successful request"() {
    when:
    def uri = URI.create("http://localhost:${server.httpPort()}/success")
    def request = buildRequest("GET", uri, [:])
    def responseCode = runWithSpan("parent") {
      sendRequest(request, uri)
    }

    then:
    responseCode == 200
    assertTraces(1) {
      trace(0, 5) {
        def list = Arrays.asList("RESOLVE", "CONNECT")
        spans.subList(1, 3).sort(Comparator.comparing { item -> list.indexOf(item.name) })
        span(0) {
          name "parent"
          kind INTERNAL
          hasNoParent()
        }
        span(1) {
          name "RESOLVE"
          kind INTERNAL
          childOf span(0)
          attributes {
            "$ServerAttributes.SERVER_ADDRESS" uri.host
            "$ServerAttributes.SERVER_PORT" uri.port
          }
        }
        span(2) {
          name "CONNECT"
          kind INTERNAL
          childOf(span(0))
          attributes {
            "$NetworkAttributes.NETWORK_TRANSPORT" "tcp"
            "$NetworkAttributes.NETWORK_TYPE" "ipv4"
            "$ServerAttributes.SERVER_ADDRESS" uri.host
            "$ServerAttributes.SERVER_PORT" uri.port
            "$NetworkAttributes.NETWORK_PEER_PORT" uri.port
            "$NetworkAttributes.NETWORK_PEER_ADDRESS" "127.0.0.1"
          }
        }
        span(3) {
          name "GET"
          kind CLIENT
          childOf(span(0))
        }
        span(4) {
          name "test-http-server"
          kind SERVER
          childOf(span(3))
        }
      }
    }
  }

  def "test failing request"() {
    when:
    URI uri = URI.create("http://localhost:${PortUtils.UNUSABLE_PORT}")
    def request = buildRequest("GET", uri, [:])
    runWithSpan("parent") {
      sendRequest(request, uri)
    }

    then:
    def thrownException = thrown(Exception)

    and:
    assertTraces(1) {
      trace(0, 3) {
        def list = Arrays.asList("RESOLVE", "CONNECT")
        spans.subList(1, 3).sort(Comparator.comparing { item -> list.indexOf(item.name) })
        span(0) {
          name "parent"
          kind INTERNAL
          hasNoParent()
          status ERROR
          errorEvent(thrownException.class, thrownException.message)
        }
        span(1) {
          name "RESOLVE"
          kind INTERNAL
          childOf span(0)
          attributes {
            "$ServerAttributes.SERVER_ADDRESS" uri.host
            "$ServerAttributes.SERVER_PORT" uri.port
          }
        }
        span(2) {
          name "CONNECT"
          kind INTERNAL
          childOf span(0)
          status ERROR
          errorEvent(thrownException.class, thrownException.message)
          attributes {
            "$NetworkAttributes.NETWORK_TRANSPORT" "tcp"
            "$NetworkAttributes.NETWORK_TYPE" { it == "ipv4" || it == null }
            "$ServerAttributes.SERVER_ADDRESS" uri.host
            "$ServerAttributes.SERVER_PORT" uri.port
            "$NetworkAttributes.NETWORK_PEER_ADDRESS" { it == "127.0.0.1" || it == null }
            "$NetworkAttributes.NETWORK_PEER_PORT" { it == uri.port || it == null }
          }
        }
      }
    }
  }
}
