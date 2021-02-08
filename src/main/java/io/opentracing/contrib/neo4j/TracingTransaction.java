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
import static io.opentracing.contrib.neo4j.TracingHelper.mapToString;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.Map;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;

public class TracingTransaction implements Transaction {

  private final Transaction transaction;
  private final Span parent;
  private final Tracer tracer;
  private final boolean finishSpan;

  public TracingTransaction(
      Transaction transaction, Span parent,
      Tracer tracer) {
    this(transaction, parent, tracer, false);
  }

  public TracingTransaction(
      Transaction transaction, Span parent, Tracer tracer,
      boolean finishSpan) {
    this.transaction = transaction;
    this.tracer = tracer;
    this.parent = parent;
    this.finishSpan = finishSpan;
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
  public void commit() {
    transaction.commit();
    parent.finish();
  }

  @Override
  public void rollback() {
    transaction.rollback();
    parent.finish();
  }

  @Override
  public boolean isOpen() {
    return transaction.isOpen();
  }

  @Override
  public Result run(String statementTemplate, Value parameters) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    span.setTag("parameters", parameters.toString());
    return decorate(() -> transaction.run(statementTemplate, parameters), span, tracer);
  }

  @Override
  public Result run(
      String statementTemplate,
      Map<String, Object> statementParameters) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (statementParameters != null) {
      span.setTag("parameters", mapToString(statementParameters));
    }
    return decorate(() -> transaction.run(statementTemplate, statementParameters), span, tracer);
  }

  @Override
  public Result run(
      String statementTemplate,
      Record statementParameters) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (statementParameters != null) {
      span.setTag("parameters", mapToString(statementParameters.asMap()));
    }
    return decorate(() -> transaction.run(statementTemplate, statementParameters), span, tracer);
  }

  @Override
  public Result run(String statementTemplate) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    return decorate(() -> transaction.run(statementTemplate), span, tracer);
  }

  @Override
  public Result run(Query query) {
    Span span = TracingHelper.build("run", parent, tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    span.setTag("parameters", TracingHelper.mapToString(query.parameters().asMap()));
    return decorate(() -> transaction.run(query), span, tracer);
  }
}
