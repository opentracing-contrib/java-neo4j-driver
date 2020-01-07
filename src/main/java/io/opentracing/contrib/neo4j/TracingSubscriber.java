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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class TracingSubscriber<T> implements Subscriber<T> {

  private final Span span;

  public TracingSubscriber(Span span) {
    this.span = span;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    subscription.request(Long.MAX_VALUE);
    System.out.println(subscription);
  }

  @Override
  public void onNext(T t) {
    System.out.println(t);
  }

  @Override
  public void onError(Throwable throwable) {
    TracingHelper.onError(throwable, span);
  }

  @Override
  public void onComplete() {
    span.finish();
  }
}
