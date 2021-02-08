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
  private final boolean finishSpan;

  public TracingRxTransaction(RxTransaction transaction, Span parent,
                              Tracer tracer) {
    this(transaction, parent, tracer, false);
  }

  public TracingRxTransaction(RxTransaction transaction, Span parent, Tracer tracer,
                              boolean finishSpan) {
    this.transaction = transaction;
    this.tracer = tracer;
    this.parent = parent;
    this.finishSpan = finishSpan;
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
  public RxResult run(String query, Value parameters) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    span.setTag("parameters", parameters.toString());
    return decorate(transaction.run(query, parameters), span);
  }

  @Override
  public RxResult run(String query, Map<String, Object> parameters) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    if (parameters != null) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(transaction.run(query, parameters), span);
  }

  @Override
  public RxResult run(String query, Record parameters) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    if (parameters != null) {
      span.setTag("parameters", mapToString(parameters.asMap()));
    }
    return decorate(transaction.run(query, parameters), span);
  }

  @Override
  public RxResult run(String query) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    return decorate(transaction.run(query), span);
  }

  @Override
  public RxResult run(Query query) {
    Span span = TracingHelper.build("runRx", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    span.setTag("parameters", mapToString(query.parameters().asMap()));
    return decorate(transaction.run(query), span);
  }
}
