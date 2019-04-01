/*
 * Copyright 2018-2019 The OpenTracing Authors
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
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.StatementResultCursor;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.TypeSystem;
import org.neo4j.driver.v1.util.Experimental;

public class TracingTransaction implements Transaction {

  private final Transaction transaction;
  private final Span parent;
  private final Tracer tracer;
  private final boolean finishSpan;


  public TracingTransaction(Transaction transaction, Span parent,
      Tracer tracer) {
    this(transaction, parent, tracer, false);
  }

  public TracingTransaction(Transaction transaction, Span parent, Tracer tracer,
      boolean finishSpan) {
    this.transaction = transaction;
    this.tracer = tracer;
    this.parent = parent;
    this.finishSpan = finishSpan;
  }

  @Override
  public void success() {
    parent.log("success");
    transaction.success();
  }

  @Override
  public void failure() {
    parent.log("failure");
    transaction.failure();
  }

  @Override
  public void close() {
    try {
      transaction.close();
    } finally {
      if (finishSpan) {
        parent.finish();
      }
    }
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
  public boolean isOpen() {
    return transaction.isOpen();
  }

  @Override
  public StatementResult run(String statementTemplate,
      Value parameters) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    span.setTag("parameters", parameters.toString());
    return decorate(() -> transaction.run(statementTemplate, parameters), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      String statementTemplate, Value parameters) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    span.setTag("parameters", parameters.toString());
    return decorate(transaction.runAsync(statementTemplate, parameters), span);
  }

  @Override
  public StatementResult run(String statementTemplate,
      Map<String, Object> statementParameters) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (statementParameters != null) {
      span.setTag("parameters", mapToString(statementParameters));
    }
    return decorate(() -> transaction.run(statementTemplate, statementParameters), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      String statementTemplate, Map<String, Object> statementParameters) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (statementParameters != null) {
      span.setTag("parameters", mapToString(statementParameters));
    }
    return decorate(transaction.runAsync(statementTemplate, statementParameters), span);
  }

  @Override
  public StatementResult run(String statementTemplate,
      Record statementParameters) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (statementParameters != null) {
      span.setTag("parameters", mapToString(statementParameters.asMap()));
    }
    return decorate(() -> transaction.run(statementTemplate, statementParameters), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      String statementTemplate, Record statementParameters) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (statementParameters != null) {
      span.setTag("parameters", TracingHelper.mapToString(statementParameters.asMap()));
    }

    return decorate(transaction.runAsync(statementTemplate, statementParameters), span);
  }

  @Override
  public StatementResult run(String statementTemplate) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    return decorate(() -> transaction.run(statementTemplate), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      String statementTemplate) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    return decorate(transaction.runAsync(statementTemplate), span);
  }

  @Override
  public StatementResult run(Statement statement) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statement.toString());
    return decorate(() -> transaction.run(statement), span, tracer);
  }

  @Override
  public CompletionStage<StatementResultCursor> runAsync(
      Statement statement) {
    Span span = TracingHelper.build("runAsync", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statement.toString());
    return decorate(transaction.runAsync(statement), span);
  }

  @Override
  @Experimental
  public TypeSystem typeSystem() {
    return transaction.typeSystem();
  }

}
