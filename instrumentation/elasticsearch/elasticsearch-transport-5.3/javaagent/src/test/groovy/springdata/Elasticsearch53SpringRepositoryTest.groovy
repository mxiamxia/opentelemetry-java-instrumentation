/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package springdata

import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes
import org.junit.jupiter.api.Assumptions
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Shared
import spock.util.environment.Jvm

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

import static io.opentelemetry.api.trace.SpanKind.CLIENT
import static io.opentelemetry.api.trace.SpanKind.INTERNAL

class Elasticsearch53SpringRepositoryTest extends AgentInstrumentationSpecification {
  // Setting up appContext & repo with @Shared doesn't allow
  // spring-data instrumentation to applied.
  // To change the timing without adding ugly checks everywhere -
  // use a dynamic proxy.  There's probably a more "groovy" way to do this.

  @Shared
  LazyProxyInvoker lazyProxyInvoker = new LazyProxyInvoker()

  @Shared
  DocRepository repo = Proxy.newProxyInstance(
    getClass().getClassLoader(),
    [DocRepository] as Class[],
    lazyProxyInvoker)

  static class LazyProxyInvoker implements InvocationHandler {
    def repo
    def applicationContext

    DocRepository getOrCreateRepository() {
      if (repo != null) {
        return repo
      }

      applicationContext = new AnnotationConfigApplicationContext(Config)
      repo = applicationContext.getBean(DocRepository)

      return repo
    }

    void close() {
      applicationContext?.close()
    }

    @Override
    Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      return method.invoke(getOrCreateRepository(), args)
    }
  }

  def setup() {
    // when running on jdk 21 this test occasionally fails with timeout
    Assumptions.assumeTrue(Boolean.getBoolean("testLatestDeps") || !Jvm.getCurrent().isJava21Compatible())

    repo.refresh()
    clearExportedData()
    runWithSpan("delete") {
      repo.deleteAll()
    }
    ignoreTracesAndClear(1)
  }

  def cleanupSpec() {
    lazyProxyInvoker.close()
  }

  def "test empty repo"() {
    when:
    def result = repo.findAll()

    then:
    !result.iterator().hasNext()

    and:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "DocRepository.findAll"
          kind INTERNAL
          attributes {
            "$CodeIncubatingAttributes.CODE_NAMESPACE" DocRepository.name
            "$CodeIncubatingAttributes.CODE_FUNCTION" "findAll"
          }
        }
        span(1) {
          name "SearchAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "SearchAction"
            "elasticsearch.action" "SearchAction"
            "elasticsearch.request" "SearchRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.request.search.types" "doc"
          }
        }
      }
    }

    where:
    indexName = "test-index"
  }

  def "test CRUD"() {
    when:
    def doc = new Doc()

    then:
    repo.index(doc) == doc

    and:
    assertTraces(1) {
      trace(0, 3) {
        span(0) {
          name "DocRepository.index"
          kind INTERNAL
          attributes {
            "$CodeIncubatingAttributes.CODE_NAMESPACE" DocRepository.name
            "$CodeIncubatingAttributes.CODE_FUNCTION" "index"
          }
        }
        span(1) {
          name "IndexAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "IndexAction"
            "elasticsearch.action" "IndexAction"
            "elasticsearch.request" "IndexRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.request.write.type" "doc"
            "elasticsearch.request.write.version"(-3)
            "elasticsearch.response.status" 201
            "elasticsearch.shard.replication.failed" 0
            "elasticsearch.shard.replication.successful" 1
            "elasticsearch.shard.replication.total" 2
          }
        }
        span(2) {
          name "RefreshAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "RefreshAction"
            "elasticsearch.action" "RefreshAction"
            "elasticsearch.request" "RefreshRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.shard.broadcast.failed" 0
            "elasticsearch.shard.broadcast.successful" 5
            "elasticsearch.shard.broadcast.total" 10
          }
        }
      }
    }
    clearExportedData()

    and:
    repo.findById("1").get() == doc

    and:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "DocRepository.findById"
          kind INTERNAL
          attributes {
            "$CodeIncubatingAttributes.CODE_NAMESPACE" DocRepository.name
            "$CodeIncubatingAttributes.CODE_FUNCTION" "findById"
          }
        }
        span(1) {
          name "GetAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "GetAction"
            "elasticsearch.action" "GetAction"
            "elasticsearch.request" "GetRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.type" "doc"
            "elasticsearch.id" "1"
            "elasticsearch.version" Number
          }
        }
      }
    }
    clearExportedData()

    when:
    doc.data = "other data"

    then:
    repo.index(doc) == doc
    repo.findById("1").get() == doc

    and:
    assertTraces(2) {
      trace(0, 3) {
        span(0) {
          name "DocRepository.index"
          kind INTERNAL
          attributes {
            "$CodeIncubatingAttributes.CODE_NAMESPACE" DocRepository.name
            "$CodeIncubatingAttributes.CODE_FUNCTION" "index"
          }
        }
        span(1) {
          name "IndexAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "IndexAction"
            "elasticsearch.action" "IndexAction"
            "elasticsearch.request" "IndexRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.request.write.type" "doc"
            "elasticsearch.request.write.version"(-3)
            "elasticsearch.response.status" 200
            "elasticsearch.shard.replication.failed" 0
            "elasticsearch.shard.replication.successful" 1
            "elasticsearch.shard.replication.total" 2
          }
        }
        span(2) {
          name "RefreshAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "RefreshAction"
            "elasticsearch.action" "RefreshAction"
            "elasticsearch.request" "RefreshRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.shard.broadcast.failed" 0
            "elasticsearch.shard.broadcast.successful" 5
            "elasticsearch.shard.broadcast.total" 10
          }
        }
      }
      trace(1, 2) {
        span(0) {
          name "DocRepository.findById"
          kind INTERNAL
          attributes {
            "$CodeIncubatingAttributes.CODE_NAMESPACE" DocRepository.name
            "$CodeIncubatingAttributes.CODE_FUNCTION" "findById"
          }
        }
        span(1) {
          name "GetAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "GetAction"
            "elasticsearch.action" "GetAction"
            "elasticsearch.request" "GetRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.type" "doc"
            "elasticsearch.id" "1"
            "elasticsearch.version" Number
          }
        }
      }
    }
    clearExportedData()

    when:
    repo.deleteById("1")

    then:
    !repo.findAll().iterator().hasNext()

    and:
    assertTraces(2) {
      trace(0, 3) {
        span(0) {
          name "DocRepository.deleteById"
          kind INTERNAL
          attributes {
            "$CodeIncubatingAttributes.CODE_NAMESPACE" DocRepository.name
            "$CodeIncubatingAttributes.CODE_FUNCTION" "deleteById"
          }
        }
        span(1) {
          name "DeleteAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "DeleteAction"
            "elasticsearch.action" "DeleteAction"
            "elasticsearch.request" "DeleteRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.request.write.type" "doc"
            "elasticsearch.request.write.version"(-3)
            "elasticsearch.shard.replication.failed" 0
            "elasticsearch.shard.replication.successful" 1
            "elasticsearch.shard.replication.total" 2
          }
        }
        span(2) {
          name "RefreshAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "RefreshAction"
            "elasticsearch.action" "RefreshAction"
            "elasticsearch.request" "RefreshRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.shard.broadcast.failed" 0
            "elasticsearch.shard.broadcast.successful" 5
            "elasticsearch.shard.broadcast.total" 10
          }
        }
      }

      trace(1, 2) {
        span(0) {
          name "DocRepository.findAll"
          kind INTERNAL
          attributes {
            "$CodeIncubatingAttributes.CODE_NAMESPACE" DocRepository.name
            "$CodeIncubatingAttributes.CODE_FUNCTION" "findAll"
          }
        }
        span(1) {
          name "SearchAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "$DbIncubatingAttributes.DB_SYSTEM" "elasticsearch"
            "$DbIncubatingAttributes.DB_OPERATION" "SearchAction"
            "elasticsearch.action" "SearchAction"
            "elasticsearch.request" "SearchRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.request.search.types" "doc"
          }
        }
      }
    }

    where:
    indexName = "test-index"
  }
}
