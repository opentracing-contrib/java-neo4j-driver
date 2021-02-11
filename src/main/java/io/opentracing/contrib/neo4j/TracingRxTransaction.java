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
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Mono;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxTransaction;
import org.reactivestreams.Publisher;

import java.util.Map;

import static io.opentracing.contrib.neo4j.TracingHelper.*;

public class TracingRxTransaction implements RxTransaction {

  private final RxTransaction transaction;
  private final Span parent;
  private final Tracer tracer;

  public TracingRxTransaction(RxTransaction transaction, Span parent, Tracer tracer) {
    this.transaction = transaction;
    this.tracer = tracer;
    this.parent = parent;
  }

  @Override
  public <T> Publisher<T> commit() {
    return Mono.<T>from(transaction.commit())
            .doOnSuccess(t -> parent.finish())
            .doOnError(throwable -> {
              onError(throwable, parent);
              parent.finish();
            });
  }

  @Override
  public <T> Publisher<T> rollback() {
    return Mono.<T>from(transaction.rollback())
            .doOnSuccess(t -> parent.finish())
            .doOnError(throwable -> {
              onError(throwable, parent);
              parent.finish();
            });
  }

  @Override
  public RxResult run(String query, Value parametersValue) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    Map<String, Object> parameters = parametersValue.asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return new TracingRxResult(transaction.run(query, parametersValue), span);
  }

  @Override
  public RxResult run(String query, Map<String, Object> parameters) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return new TracingRxResult(transaction.run(query, parameters), span);
  }

  @Override
  public RxResult run(String query, Record parametersRecord) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    Map<String, Object> parameters = parametersRecord.asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return new TracingRxResult(transaction.run(query, parametersRecord), span);
  }

  @Override
  public RxResult run(String query) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    return new TracingRxResult(transaction.run(query), span);
  }

  @Override
  public RxResult run(Query query) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    Map<String, Object> parameters = query.parameters().asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return new TracingRxResult(transaction.run(query), span);
  }
}
