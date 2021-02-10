/*
 * Copyright 2018-2020 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.neo4j;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.driver.summary.ResultSummary;
import org.testcontainers.containers.Neo4jContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static io.opentracing.contrib.neo4j.TestConstants.NEO4J_IMAGE;
import static io.opentracing.contrib.neo4j.TestConstants.NEO4J_PASSWORD;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class RxTracingTest {

  private final MockTracer tracer = new MockTracer();

  @ClassRule
  public static Neo4jContainer neo4j = new Neo4jContainer(NEO4J_IMAGE).withAdminPassword(NEO4J_PASSWORD);

  private Driver driver;

  @Before
  public void before() {
    tracer.reset();
    AuthToken authToken = AuthTokens.basic(TestConstants.NEO4J_USER, NEO4J_PASSWORD);
    driver = new TracingDriver(GraphDatabase.driver(neo4j.getBoltUrl(), authToken), tracer);
  }

  @After
  public void after() {
    driver.close();
  }

  @Test
  public void testWriteTransactionRx() {
    String query = "CREATE (n:Person) RETURN n";
    Flux<ResultSummary> resultSummaries = Flux.usingWhen(Mono.fromSupplier(driver::rxSession),
            session -> session.writeTransaction(tx -> {
                      RxResult result = tx.run(query);
                      return Flux.from(result.records())
                              .doOnNext(record -> System.out.println(record.toString())).then(Mono.from(result.consume()));
                    }
            ), RxSession::close);

    resultSummaries.as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(2));

    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(2, spans.size());
    validateSpans(spans, "runRx", "writeTransactionRx");

    assertNull(tracer.activeSpan());
  }

  @Test
  public void testReadTransactionRx() {
    String query = "UNWIND range(1, 10) AS x RETURN x";
    Flux<ResultSummary> resultSummaries = Flux.usingWhen(Mono.fromSupplier(driver::rxSession),
            session -> session.readTransaction(tx -> {
                      RxResult result = tx.run(query);
                      return Flux.from(result.records())
                              .doOnNext(record -> System.out.println(record.get(0).asInt())).then(Mono.from(result.consume()));
                    }
            ), RxSession::close);

    resultSummaries.as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(2));

    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(2, spans.size());
    validateSpans(spans, "runRx", "readTransactionRx");

    assertNull(tracer.activeSpan());
  }

  @Test
  public void testRunRx() {
    String query = "UNWIND range(1, 10) AS x RETURN x";

    Flux<ResultSummary> resultSummaries = Flux.usingWhen(Mono.fromSupplier(driver::rxSession),
            session -> {
              RxResult result = session.run(query);
              return Flux.from(result.records())
                      .doOnNext(record -> System.out.println(record.get(0).asInt())).then(Mono.from(result.consume()));
            },
            RxSession::close);

    resultSummaries.as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(1));

    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(1, spans.size());
    validateSpans(spans, "runRx");

    assertNull(tracer.activeSpan());
  }

  @Test
  public void testRunInTransactionRx() {
    String query = "UNWIND range(1, 10) AS x RETURN x";

    Flux<ResultSummary> resultSummaries = Flux.usingWhen(
            Mono.fromSupplier(driver::rxSession),
            session -> Flux.usingWhen(
                    session.beginTransaction(),
                    tr -> {
                      RxResult result = tr.run(query);
                      return Flux.from(result.records())
                              .doOnNext(record -> System.out.println(record.get(0).asInt())).then(Mono.from(result.consume()));
                    },
                    RxTransaction::commit),
            RxSession::close);

    resultSummaries.as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(2));

    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(2, spans.size());
    validateSpans(spans, "runRx", "transactionRx");

    assertNull(tracer.activeSpan());
  }

  @Test
  public void testRunInTransactionRxWithFailure() {
    String wrongQuery = "UNWIND range(1, 10) AS x";

    Flux<ResultSummary> resultSummaries = Flux.usingWhen(
            Mono.fromSupplier(driver::rxSession),
            session -> Flux.usingWhen(
                    session.beginTransaction(),
                    tr -> {
                      RxResult result = tr.run(wrongQuery);
                      return Flux.from(result.records())
                              .doOnNext(record -> System.out.println(record.get(0).asInt())).then(Mono.from(result.consume()));
                    },
                    RxTransaction::rollback),
            RxSession::close);

    resultSummaries.as(StepVerifier::create)
            .expectErrorSatisfies(error -> {
                      assertTrue(error instanceof ClientException);
                      assertTrue(error.getMessage().contains("Query cannot conclude with UNWIND"));
                    }
            ).verify();

    await().atMost(15, TimeUnit.SECONDS).until(reportedSpansSize(), equalTo(2));

    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(2, spans.size());
    validateSpans(spans, "runRx", "transactionRx");

    assertNull(tracer.activeSpan());
  }

  private void validateSpans(List<MockSpan> spans, String... spanNames) {
    for (int i = 0; i < spans.size(); i++) {
      MockSpan span = spans.get(i);
      assertEquals(spanNames[i], span.operationName());
      assertEquals(span.tags().get(Tags.SPAN_KIND.getKey()), Tags.SPAN_KIND_CLIENT);
      assertEquals(TracingHelper.COMPONENT_NAME, span.tags().get(Tags.COMPONENT.getKey()));
      assertEquals(TracingHelper.DB_TYPE, span.tags().get(Tags.DB_TYPE.getKey()));
      assertEquals(0, span.generatedErrors().size());
    }
  }

  private Callable<Integer> reportedSpansSize() {
    return () -> tracer.finishedSpans().size();
  }

}
