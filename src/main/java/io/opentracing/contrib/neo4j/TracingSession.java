/*
 * Copyright 2018 The OpenTracing Authors
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
import static io.opentracing.contrib.neo4j.TracingHelper.mapToString;
import static io.opentracing.contrib.neo4j.TracingHelper.onError;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.StatementResultCursor;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.TypeSystem;
import org.neo4j.driver.v1.util.Experimental;

public class TracingSession implements Session {

  private final Session session;
  private final Tracer tracer;

  public TracingSession(Session session, Tracer tracer) {
    this.session = session;
    this.tracer = tracer;
  }

  @Override
  public Transaction beginTransaction() {
    Span span = TracingHelper.build("beginTransaction", tracer);
    return new TracingTransaction(session.beginTransaction(), span, tracer, true);
  }

  @Override
  @Deprecated
  public Transaction beginTransaction(String bookmark) {
    Span span = TracingHelper.build("beginTransaction", tracer);
    span.setTag("bookmark", bookmark);
    return new TracingTransaction(session.beginTransaction(bookmark), span, tracer, true);
  }

  @Override
  public CompletionStage<Transaction> beginTransactionAsync() {
    // TODO
    return session.beginTransactionAsync();
  }

  @Override
  public <T> T readTransaction(TransactionWork<T> work) {
    Span span = TracingHelper.build("readTransaction", tracer);
    return decorate(() -> session.readTransaction(
        new TracingTransactionWork<>(work, span, tracer)), span, tracer);
  }

  @Override
  public <T> CompletionStage<T> readTransactionAsync(
      TransactionWork<CompletionStage<T>> work) {
    Span span = TracingHelper.build("readTransactionAsync", tracer);
    try {
      return session.readTransactionAsync(new TracingTransactionWork<>(work, span, tracer))
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
  public <T> T writeTransaction(TransactionWork<T> work) {
    Span span = TracingHelper.build("writeTransaction", tracer);
    return decorate(() -> session.writeTransaction(
        new TracingTransactionWork<>(work, span, tracer)), span, tracer);
  }

  @Override
  public <T> CompletionStage<T> writeTransactionAsync(
      TransactionWork<CompletionStage<T>> work) {
    Span span = TracingHelper.build("writeTransactionAsync", tracer);
    try {
      return session.writeTransactionAsync(new TracingTransactionWork<>(work, span, tracer))
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
  public String lastBookmark() {
    return session.lastBookmark();
  }

  @Override
  @Deprecated
  public void reset() {
    session.reset();
  }

  @Override
  public void close() {
    session.close();
  }

  @Override
  public CompletionStage<Void> closeAsync() {
    return session.closeAsync();
  }

  @Override
  public boolean isOpen() {
    return session.isOpen();
  }

  @Override
  public StatementResult run(String statementTemplate,
      Value parameters) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (parameters != null) {
      span.setTag("parameters", parameters.toString());
    }
    return decorate(() -> session.run(statementTemplate, parameters), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      String statementTemplate, Value parameters) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (parameters != null) {
      span.setTag("parameters", parameters.toString());
    }
    return decorate(session.runAsync(statementTemplate, parameters), span);
  }

  @Override
  public StatementResult run(String statementTemplate,
      Map<String, Object> statementParameters) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    span.setTag("parameters", mapToString(statementParameters));
    return decorate(() -> session.run(statementTemplate, statementParameters), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      String statementTemplate, Map<String, Object> statementParameters) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    span.setTag("parameters", mapToString(statementParameters));
    return decorate(session.runAsync(statementTemplate, statementParameters), span);
  }

  @Override
  public StatementResult run(String statementTemplate,
      Record statementParameters) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (statementParameters != null) {
      span.setTag("parameters", mapToString(statementParameters.asMap()));
    }
    return decorate(() -> session.run(statementTemplate, statementParameters), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      String statementTemplate, Record statementParameters) {
    Span span = TracingHelper.build("runAsync", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (statementParameters != null) {
      span.setTag("parameters", mapToString(statementParameters.asMap()));
    }
    return decorate(session.runAsync(statementTemplate, statementParameters), span);
  }

  @Override
  public StatementResult run(String statementTemplate) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    return decorate(() -> session.run(statementTemplate), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      String statementTemplate) {
    Span span = TracingHelper.build("runAsync", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    return decorate(session.runAsync(statementTemplate), span);
  }

  @Override
  public StatementResult run(Statement statement) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statement.toString());
    return decorate(() -> session.run(statement), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      Statement statement) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statement.toString());
    return decorate(session.runAsync(statement), span);
  }

  @Override
  @Experimental
  public TypeSystem typeSystem() {
    return session.typeSystem();
  }


}
