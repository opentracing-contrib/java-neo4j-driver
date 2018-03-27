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

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.neo4j.driver.v1.StatementResultCursor;

class TracingHelper {

  static final String COMPONENT_NAME = "java-neo4j";
  static final String DB_TYPE = "neo4j";

  static Span build(String operationName, Tracer tracer) {
    return build(operationName, null, tracer);
  }

  static Span build(String operationName, Span parent, Tracer tracer) {
    SpanBuilder builder = tracer.buildSpan(operationName)
        .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME)
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
        .withTag(Tags.DB_TYPE.getKey(), DB_TYPE);
    if (parent != null) {
      builder.asChildOf(parent);
    }
    return builder.start();
  }

  static void onError(Throwable throwable, Span span) {
    if (span == null) {
      return;
    }
    Tags.ERROR.set(span, Boolean.TRUE);

    if (throwable != null) {
      span.log(errorLogs(throwable));
    }
  }

  static String mapToString(Map<String, Object> map) {
    if (map == null) {
      return "";
    }
    return map.entrySet()
        .stream()
        .map(entry -> entry.getKey() + " -> " + entry.getValue())
        .collect(Collectors.joining(", "));
  }

  static CompletionStage<StatementResultCursor> decorate(
      CompletionStage<StatementResultCursor> stage,
      Span span) {
    return stage.whenComplete((statementResultCursor, throwable) -> {
      if (throwable != null) {
        onError(throwable, span);
      }
      span.finish();
    });
  }

  private static Map<String, Object> errorLogs(Throwable throwable) {
    Map<String, Object> errorLogs = new HashMap<>(2);
    errorLogs.put("event", Tags.ERROR.getKey());
    errorLogs.put("error.object", throwable);
    return errorLogs;
  }

  public static <T> T decorate(Supplier<T> supplier, Span span, Tracer tracer) {
    try (Scope ignore = tracer.scopeManager().activate(span, false)) {
      return supplier.get();
    } catch (Exception e) {
      onError(e, span);
      throw e;
    } finally {
      span.finish();
    }

  }
}
