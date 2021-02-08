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
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Flux;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.summary.ResultSummary;
import org.reactivestreams.Publisher;

import java.util.List;

import static io.opentracing.contrib.neo4j.TracingHelper.onError;

public class TracingRxResult implements RxResult {
  private final RxResult rxResult;
  private final Span parent;

  public TracingRxResult(RxResult rxResult, Span parent) {
    this.rxResult = rxResult;
    this.parent = parent;
  }

  @Override
  public Publisher<List<String>> keys() {
    return rxResult.keys();
  }

  @Override
  public Publisher<Record> records() {
    return Flux.from(rxResult.records())
            .doOnComplete(parent::finish)
            .doOnError(throwable -> {
              onError(throwable, parent);
              parent.finish();
            });
  }

  @Override
  public Publisher<ResultSummary> consume() {
    return rxResult.consume();
  }
}
