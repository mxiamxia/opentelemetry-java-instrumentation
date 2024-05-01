/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import com.mongodb.reactivestreams.client.MongoDatabase
import io.opentelemetry.instrumentation.mongo.testing.AbstractMongoClientTest
import io.opentelemetry.instrumentation.test.AgentTestTrait
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.opentest4j.TestAbortedException
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.lang.Shared

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class Mongo4ReactiveClientTest extends AbstractMongoClientTest<MongoCollection<Document>> implements AgentTestTrait {

  @Shared
  MongoClient client
  @Shared
  List<Closeable> cleanup = []

  def setupSpec() throws Exception {
    client = MongoClients.create("mongodb://$host:$port")
  }

  def cleanupSpec() throws Exception {
    client?.close()
    client = null
    cleanup.forEach {
      it.close()
    }
  }

  @Override
  void createCollection(String dbName, String collectionName) {
    MongoDatabase db = client.getDatabase(dbName)
    def latch = new CountDownLatch(1)
    db.createCollection(collectionName).subscribe(toSubscriber { latch.countDown() })
    latch.await(30, TimeUnit.SECONDS)
  }

  @Override
  void createCollectionNoDescription(String dbName, String collectionName) {
    def tmpClient = MongoClients.create("mongodb://$host:${port}")
    cleanup.add(tmpClient)
    MongoDatabase db = tmpClient.getDatabase(dbName)
    def latch = new CountDownLatch(1)
    db.createCollection(collectionName).subscribe(toSubscriber { latch.countDown() })
    latch.await(30, TimeUnit.SECONDS)
  }

  @Override
  void createCollectionWithAlreadyBuiltClientOptions(String dbName, String collectionName) {
    throw new TestAbortedException("not tested on 4.0")
  }

  @Override
  void createCollectionCallingBuildTwice(String dbName, String collectionName) {
    def settings = MongoClientSettings.builder()
      .applyToClusterSettings({ builder ->
        builder.hosts(Arrays.asList(
          new ServerAddress(host, port)))
      })
    settings.build()
    def tmpClient = MongoClients.create(settings.build())
    cleanup.add(tmpClient)
    MongoDatabase db = tmpClient.getDatabase(dbName)
    def latch = new CountDownLatch(1)
    db.createCollection(collectionName).subscribe(toSubscriber { latch.countDown() })
    latch.await(30, TimeUnit.SECONDS)
  }

  @Override
  int getCollection(String dbName, String collectionName) {
    MongoDatabase db = client.getDatabase(dbName)
    def count = new CompletableFuture<Integer>()
    db.getCollection(collectionName).estimatedDocumentCount().subscribe(toSubscriber { count.complete(it) })
    return count.get(30, TimeUnit.SECONDS)
  }

  @Override
  MongoCollection<Document> setupInsert(String dbName, String collectionName) {
    MongoCollection<Document> collection = runWithSpan("setup") {
      MongoDatabase db = client.getDatabase(dbName)
      def latch1 = new CountDownLatch(1)
      db.createCollection(collectionName).subscribe(toSubscriber { latch1.countDown() })
      latch1.await(30, TimeUnit.SECONDS)
      return db.getCollection(collectionName)
    }
    ignoreTracesAndClear(1)
    return collection
  }

  @Override
  int insert(MongoCollection<Document> collection) {
    def count = new CompletableFuture<Integer>()
    collection.insertOne(new Document("password", "SECRET")).subscribe(toSubscriber {
      collection.estimatedDocumentCount().subscribe(toSubscriber { count.complete(it) })
    })
    return count.get(30, TimeUnit.SECONDS)
  }

  @Override
  MongoCollection<Document> setupUpdate(String dbName, String collectionName) {
    MongoCollection<Document> collection = runWithSpan("setup") {
      MongoDatabase db = client.getDatabase(dbName)
      def latch1 = new CountDownLatch(1)
      db.createCollection(collectionName).subscribe(toSubscriber { latch1.countDown() })
      latch1.await(30, TimeUnit.SECONDS)
      def coll = db.getCollection(collectionName)
      def latch2 = new CountDownLatch(1)
      coll.insertOne(new Document("password", "OLDPW")).subscribe(toSubscriber { latch2.countDown() })
      latch2.await(30, TimeUnit.SECONDS)
      return coll
    }
    ignoreTracesAndClear(1)
    return collection
  }

  @Override
  int update(MongoCollection<Document> collection) {
    def result = new CompletableFuture<UpdateResult>()
    def count = new CompletableFuture()
    collection.updateOne(
      new BsonDocument("password", new BsonString("OLDPW")),
      new BsonDocument('$set', new BsonDocument("password", new BsonString("NEWPW")))).subscribe(toSubscriber {
      result.complete(it)
      collection.estimatedDocumentCount().subscribe(toSubscriber { count.complete(it) })
    })
    return result.get(30, TimeUnit.SECONDS).modifiedCount
  }

  @Override
  MongoCollection<Document> setupDelete(String dbName, String collectionName) {
    MongoCollection<Document> collection = runWithSpan("setup") {
      MongoDatabase db = client.getDatabase(dbName)
      def latch1 = new CountDownLatch(1)
      db.createCollection(collectionName).subscribe(toSubscriber { latch1.countDown() })
      latch1.await(30, TimeUnit.SECONDS)
      def coll = db.getCollection(collectionName)
      def latch2 = new CountDownLatch(1)
      coll.insertOne(new Document("password", "SECRET")).subscribe(toSubscriber { latch2.countDown() })
      latch2.await(30, TimeUnit.SECONDS)
      return coll
    }
    ignoreTracesAndClear(1)
    return collection
  }

  @Override
  int delete(MongoCollection<Document> collection) {
    def result = new CompletableFuture<DeleteResult>()
    def count = new CompletableFuture()
    collection.deleteOne(new BsonDocument("password", new BsonString("SECRET"))).subscribe(toSubscriber {
      result.complete(it)
      collection.estimatedDocumentCount().subscribe(toSubscriber { count.complete(it) })
    })
    return result.get(30, TimeUnit.SECONDS).deletedCount
  }

  @Override
  MongoCollection<Document> setupGetMore(String dbName, String collectionName) {
    throw new TestAbortedException("not tested on reactive")
  }

  @Override
  void getMore(MongoCollection<Document> collection) {
    throw new TestAbortedException("not tested on reactive")
  }

  @Override
  void error(String dbName, String collectionName) {
    MongoCollection<Document> collection = runWithSpan("setup") {
      MongoDatabase db = client.getDatabase(dbName)
      def latch = new CountDownLatch(1)
      db.createCollection(collectionName).subscribe(toSubscriber {
        latch.countDown()
      })
      latch.await(30, TimeUnit.SECONDS)
      return db.getCollection(collectionName)
    }
    ignoreTracesAndClear(1)
    def result = new CompletableFuture<Throwable>()
    collection.updateOne(new BsonDocument(), new BsonDocument()).subscribe(toSubscriber {
      result.complete(it)
    })
    throw result.get(30, TimeUnit.SECONDS)
  }

  Subscriber<?> toSubscriber(Closure closure) {
    return new Subscriber() {
      boolean hasResult

      @Override
      void onSubscribe(Subscription s) {
        s.request(1) // must request 1 value to trigger async call
      }

      @Override
      void onNext(Object o) { hasResult = true; closure.call(o) }

      @Override
      void onError(Throwable t) { hasResult = true; closure.call(t) }

      @Override
      void onComplete() {
        if (!hasResult) {
          hasResult = true
          closure.call()
        }
      }
    }
  }
}
