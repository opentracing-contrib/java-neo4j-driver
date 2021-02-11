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

import static io.opentracing.contrib.neo4j.TracingHelper.decorate;
import static io.opentracing.contrib.neo4j.TracingHelper.isNotEmpty;
import static io.opentracing.contrib.neo4j.TracingHelper.mapToString;
import static io.opentracing.contrib.neo4j.TracingHelper.onError;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.async.AsyncTransaction;
import org.neo4j.driver.async.ResultCursor;

public class TracingAsyncTransaction implements AsyncTransaction {

  private final AsyncTransaction transaction;
  private final Span parent;
  private final Tracer tracer;

  public TracingAsyncTransaction(AsyncTransaction transaction, Span parent, Tracer tracer) {
    this.transaction = transaction;
    this.tracer = tracer;
    this.parent = parent;
  }

  @Override
  public CompletionStage<Void> commitAsync() {
    return transaction.commitAsync().whenComplete((aVoid, throwable) -> {
      if (throwable != null) {
        onError(throwable, parent);
      }
      parent.finish();
    });
  }

  @Override
  public CompletionStage<Void> rollbackAsync() {
    return transaction.rollbackAsync().whenComplete((aVoid, throwable) -> {
      if (throwable != null) {
        onError(throwable, parent);
      }
      parent.finish();
    });
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(String query, Value parametersValue) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    Map<String, Object> parameters = parametersValue.asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(transaction.runAsync(query, parametersValue), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(String query, Map<String, Object> parameters) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(transaction.runAsync(query, parameters), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(String query, Record parametersRecord) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    Map<String, Object> parameters = parametersRecord.asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(transaction.runAsync(query, parametersRecord), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(String query) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    return decorate(transaction.runAsync(query), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(Query query) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    Map<String, Object> parameters = query.parameters().asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(transaction.runAsync(query), span);
  }
}
