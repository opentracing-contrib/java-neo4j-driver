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

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.neo4j.driver.*;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Flux;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Mono;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.driver.reactive.RxTransactionWork;
import org.reactivestreams.Publisher;

import java.util.Map;

import static io.opentracing.contrib.neo4j.TracingHelper.*;

public class TracingRxSession implements RxSession {

  private final RxSession rxSession;
  private final Tracer tracer;

  public TracingRxSession(RxSession rxSession, Tracer tracer) {
    this.rxSession = rxSession;
    this.tracer = tracer;
  }

  @Override
  public Publisher<RxTransaction> beginTransaction() {
    Span span = TracingHelper.build("transactionRx", tracer);
    Mono<RxTransaction> transaction = Mono.from(rxSession.beginTransaction());
    return transaction.map(tr -> new TracingRxTransaction(tr, span, tracer));
  }

  @Override
  public Publisher<RxTransaction> beginTransaction(TransactionConfig transactionConfig) {
    Span span = TracingHelper.build("transactionRx", tracer);
    Mono<RxTransaction> transaction = Mono.from(rxSession.beginTransaction(transactionConfig));
    return transaction.map(tr -> new TracingRxTransaction(tr, span, tracer));
  }

  @Override
  public <T> Publisher<T> readTransaction(RxTransactionWork<? extends Publisher<T>> rxTransactionWork) {
    Span span = TracingHelper.build("readTransactionRx", tracer);

    return Flux.from(rxSession.readTransaction(new TracingRxTransactionWork<>(rxTransactionWork, span, tracer)))
            .doOnComplete(() -> span.finish())
            .doOnError(throwable -> {
              onError(throwable, span);
              span.finish();
            });
  }

  @Override
  public <T> Publisher<T> readTransaction(RxTransactionWork<? extends Publisher<T>> rxTransactionWork, TransactionConfig transactionConfig) {
    Span span = TracingHelper.build("readTransactionRx", tracer);

    return Flux.from(rxSession.readTransaction(new TracingRxTransactionWork<>(rxTransactionWork, span, tracer), transactionConfig))
            .doOnComplete(() -> span.finish())
            .doOnError(throwable -> {
              onError(throwable, span);
              span.finish();
            });
  }

  @Override
  public <T> Publisher<T> writeTransaction(RxTransactionWork<? extends Publisher<T>> rxTransactionWork) {
    Span span = TracingHelper.build("writeTransactionRx", tracer);

    return Flux.from(rxSession.writeTransaction(new TracingRxTransactionWork<>(rxTransactionWork, span, tracer)))
            .doOnComplete(() -> span.finish())
            .doOnError(throwable -> {
              onError(throwable, span);
              span.finish();
            });
  }

  @Override
  public <T> Publisher<T> writeTransaction(RxTransactionWork<? extends Publisher<T>> rxTransactionWork, TransactionConfig transactionConfig) {
    Span span = TracingHelper.build("writeTransactionRx", tracer);

    return Flux.from(rxSession.writeTransaction(new TracingRxTransactionWork<>(rxTransactionWork, span, tracer), transactionConfig))
            .doOnComplete(() -> span.finish())
            .doOnError(throwable -> {
              onError(throwable, span);
              span.finish();
            });
  }

  @Override
  public Bookmark lastBookmark() {
    return rxSession.lastBookmark();
  }

  @Override
  public <T> Publisher<T> close() {
    return rxSession.close();
  }

  @Override
  public RxResult run(String query, TransactionConfig transactionConfig) {
    Span span = TracingHelper.build("runRx", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    span.setTag("config", transactionConfig.toString());

    return decorate(rxSession.run(query, transactionConfig), span);
  }

  @Override
  public RxResult run(String query, Map<String, Object> parameters, TransactionConfig transactionConfig) {
    Span span = TracingHelper.build("runRx", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    span.setTag("config", transactionConfig.toString());
    if (parameters != null) {
      span.setTag("parameters", mapToString(parameters));
    }

    return decorate(rxSession.run(query, parameters, transactionConfig), span);
  }

  @Override
  public RxResult run(Query query, TransactionConfig transactionConfig) {
    Span span = TracingHelper.build("runRx", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    span.setTag("config", transactionConfig.toString());
    span.setTag("parameters", mapToString(query.parameters().asMap()));

    return decorate(rxSession.run(query, transactionConfig), span);
  }

  @Override
  public RxResult run(String query, Value parameters) {
    Span span = TracingHelper.build("runRx", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    if (parameters != null) {
      span.setTag("parameters", parameters.toString());
    }

    return decorate(rxSession.run(query, parameters), span);
  }

  @Override
  public RxResult run(String query, Map<String, Object> parameters) {
    Span span = TracingHelper.build("runRx", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    if (parameters != null) {
      span.setTag("parameters", mapToString(parameters));
    }

    return decorate(rxSession.run(query, parameters), span);
  }

  @Override
  public RxResult run(String query, Record parameters) {
    Span span = TracingHelper.build("runRx", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    if (parameters != null) {
      span.setTag("parameters", parameters.toString());
    }

    return decorate(rxSession.run(query, parameters), span);
  }

  @Override
  public RxResult run(String query) {
    Span span = TracingHelper.build("runRx", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);

    return decorate(rxSession.run(query), span);
  }

  @Override
  public RxResult run(Query query) {
    Span span = TracingHelper.build("runRx", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    span.setTag("parameters", mapToString(query.parameters().asMap()));

    return decorate(rxSession.run(query), span);
  }
}
