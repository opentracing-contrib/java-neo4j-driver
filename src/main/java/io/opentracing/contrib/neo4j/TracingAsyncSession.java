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
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.async.AsyncTransaction;
import org.neo4j.driver.async.AsyncTransactionWork;
import org.neo4j.driver.async.ResultCursor;

public class TracingAsyncSession implements AsyncSession {

  private final AsyncSession session;
  private final Tracer tracer;

  public TracingAsyncSession(AsyncSession session, Tracer tracer) {
    this.session = session;
    this.tracer = tracer;
  }

  @Override
  public CompletionStage<AsyncTransaction> beginTransactionAsync() {
    Span span = TracingHelper.build("transactionAsync", tracer);
    CompletionStage<AsyncTransaction> transactionAsync = session.beginTransactionAsync();
    return transactionAsync.thenApply(tr -> new TracingAsyncTransaction(tr, span, tracer));
  }

  @Override
  public CompletionStage<AsyncTransaction> beginTransactionAsync(TransactionConfig config) {
    Span span = TracingHelper.build("transactionAsync", tracer);
    CompletionStage<AsyncTransaction> transactionAsync = session.beginTransactionAsync(config);
    return transactionAsync.thenApply(tr -> new TracingAsyncTransaction(tr, span, tracer));
  }

  @Override
  public <T> CompletionStage<T> readTransactionAsync(AsyncTransactionWork<CompletionStage<T>> work) {
    Span span = TracingHelper.build("readTransactionAsync", tracer);
    try {
      return session.readTransactionAsync(new TracingAsyncTransactionWork<>(work, span, tracer))
          .whenComplete((t, throwable) -> {
            if (throwable != null) {
              onError(throwable, span);
            }
            span.finish();
          });
    } catch (Exception e) {
      onError(e, span);
      span.finish();
      throw e;
    }
  }

  @Override
  public <T> CompletionStage<T> readTransactionAsync(AsyncTransactionWork<CompletionStage<T>> work, TransactionConfig config) {
    Span span = TracingHelper.build("readTransactionAsync", tracer);
    span.setTag("config", config.toString());
    try {
      return session
          .readTransactionAsync(new TracingAsyncTransactionWork<>(work, span, tracer), config)
          .whenComplete((t, throwable) -> {
            if (throwable != null) {
              onError(throwable, span);
            }
            span.finish();
          });
    } catch (Exception e) {
      onError(e, span);
      span.finish();
      throw e;
    }
  }

  @Override
  public <T> CompletionStage<T> writeTransactionAsync(
      AsyncTransactionWork<CompletionStage<T>> work) {
    Span span = TracingHelper.build("writeTransactionAsync", tracer);
    try {
      return session.writeTransactionAsync(new TracingAsyncTransactionWork<>(work, span, tracer))
          .whenComplete((t, throwable) -> {
            if (throwable != null) {
              TracingHelper.onError(throwable, span);
            }
            span.finish();
          });
    } catch (Exception e) {
      onError(e, span);
      span.finish();
      throw e;
    }
  }

  @Override
  public <T> CompletionStage<T> writeTransactionAsync(AsyncTransactionWork<CompletionStage<T>> work, TransactionConfig config) {
    Span span = TracingHelper.build("writeTransactionAsync", tracer);
    span.setTag("config", config.toString());
    try {
      return session
          .writeTransactionAsync(new TracingAsyncTransactionWork<>(work, span, tracer), config)
          .whenComplete((t, throwable) -> {
            if (throwable != null) {
              onError(throwable, span);
            }
            span.finish();
          });
    } catch (Exception e) {
      onError(e, span);
      span.finish();
      throw e;
    }
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(String query, TransactionConfig config) {
    Span span = TracingHelper.build("runAsync", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    span.setTag("config", config.toString());
    return decorate(session.runAsync(query, config), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(String query, Map<String, Object> parameters, TransactionConfig config) {
    Span span = TracingHelper.build("runAsync", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    span.setTag("config", config.toString());
    return decorate(session.runAsync(query, parameters, config), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(Query query, TransactionConfig config) {
    Span span = TracingHelper.build("runAsync", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    Map<String, Object> parameters = query.parameters().asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    span.setTag("config", config.toString());
    return decorate(session.runAsync(query, config), span);
  }

  @Override
  public Bookmark lastBookmark() {
    return session.lastBookmark();
  }

  @Override
  public CompletionStage<Void> closeAsync() {
    return session.closeAsync();
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(String query, Value parametersValue) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    Map<String, Object> parameters = parametersValue.asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(session.runAsync(query, parametersValue), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(
      String query, Map<String, Object> parameters) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(session.runAsync(query, parameters), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(String query, Record parametersRecord) {
    Span span = TracingHelper.build("runAsync", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    Map<String, Object> parameters = parametersRecord.asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(session.runAsync(query, parametersRecord), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(String query) {
    Span span = TracingHelper.build("runAsync", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query);
    return decorate(session.runAsync(query), span);
  }

  @Override
  public CompletionStage<ResultCursor> runAsync(Query query) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    Map<String, Object> parameters = query.parameters().asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(session.runAsync(query), span);
  }
}
