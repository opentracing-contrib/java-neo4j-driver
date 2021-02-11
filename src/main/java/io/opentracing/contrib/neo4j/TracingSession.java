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

import java.util.Map;

import static io.opentracing.contrib.neo4j.TracingHelper.*;

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
  public Transaction beginTransaction(TransactionConfig config) {
    Span span = TracingHelper.build("beginTransaction", tracer);
    span.setTag("config", config.toString());
    return new TracingTransaction(session.beginTransaction(config), span, tracer, true);
  }

  @Override
  public <T> T readTransaction(TransactionWork<T> work) {
    Span span = TracingHelper.build("readTransaction", tracer);
    return decorate(() -> session.readTransaction(
            new TracingTransactionWork<>(work, span, tracer)), span, tracer);
  }

  @Override
  public <T> T readTransaction(TransactionWork<T> work, TransactionConfig config) {
    Span span = TracingHelper.build("readTransaction", tracer);
    span.setTag("config", config.toString());
    return decorate(() -> session.readTransaction(
            new TracingTransactionWork<>(work, span, tracer), config), span, tracer);
  }

  @Override
  public <T> T writeTransaction(TransactionWork<T> work) {
    Span span = TracingHelper.build("writeTransaction", tracer);
    return decorate(() -> session.writeTransaction(
            new TracingTransactionWork<>(work, span, tracer)), span, tracer);
  }

  @Override
  public <T> T writeTransaction(TransactionWork<T> work, TransactionConfig config) {
    Span span = TracingHelper.build("writeTransaction", tracer);
    span.setTag("config", config.toString());
    return decorate(() -> session.writeTransaction(
            new TracingTransactionWork<>(work, span, tracer), config), span, tracer);
  }

  @Override
  public Result run(String statement, TransactionConfig config) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statement);
    span.setTag("config", config.toString());
    return decorate(() -> session.run(statement, config), span, tracer);
  }

  @Override
  public Result run(String statement, Map<String, Object> parameters, TransactionConfig config) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statement);
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    span.setTag("config", config.toString());
    return decorate(() -> session.run(statement, parameters, config), span, tracer);
  }

  @Override
  public Result run(Query query, TransactionConfig config) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    Map<String, Object> parameters = query.parameters().asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    span.setTag("config", config.toString());
    return decorate(() -> session.run(query, config), span, tracer);
  }

  @Override
  public Bookmark lastBookmark() {
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
  public boolean isOpen() {
    return session.isOpen();
  }

  @Override
  public Result run(String statementTemplate, Value parametersValue) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    Map<String, Object> parameters = parametersValue.asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(() -> session.run(statementTemplate, parametersValue), span, tracer);
  }

  @Override
  public Result run(String statementTemplate, Map<String, Object> parameters) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(() -> session.run(statementTemplate, parameters), span, tracer);
  }

  @Override
  public Result run(String statementTemplate, Record statementParameters) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    Map<String, Object> parameters = statementParameters.asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(() -> session.run(statementTemplate, statementParameters), span, tracer);
  }

  @Override
  public Result run(String statementTemplate) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), statementTemplate);
    return decorate(() -> session.run(statementTemplate), span, tracer);
  }

  @Override
  public Result run(Query query) {
    Span span = TracingHelper.build("run", tracer);
    span.setTag(Tags.DB_STATEMENT.getKey(), query.text());
    Map<String, Object> parameters = query.parameters().asMap();
    if (isNotEmpty(parameters)) {
      span.setTag("parameters", mapToString(parameters));
    }
    return decorate(() -> session.run(query), span, tracer);
  }
}
