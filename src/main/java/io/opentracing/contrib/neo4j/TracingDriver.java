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

import io.opentracing.Tracer;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Metrics;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.types.TypeSystem;

import java.util.concurrent.CompletionStage;

public class TracingDriver implements Driver {

  private final Driver driver;
  private final Tracer tracer;

  public TracingDriver(Driver driver, Tracer tracer) {
    this.driver = driver;
    this.tracer = tracer;
  }

  @Override
  public boolean isEncrypted() {
    return driver.isEncrypted();
  }

  @Override
  public Session session() {
    return new TracingSession(driver.session(), tracer);
  }

  @Override
  public Session session(SessionConfig config) {
    return new TracingSession(driver.session(config), tracer);
  }

  @Override
  public RxSession rxSession() {
    return new TracingRxSession(driver.rxSession(), tracer);
  }

  @Override
  public RxSession rxSession(SessionConfig sessionConfig) {
    return new TracingRxSession(driver.rxSession(sessionConfig), tracer);
  }

  @Override
  public AsyncSession asyncSession() {
    return new TracingAsyncSession(driver.asyncSession(), tracer);
  }

  @Override
  public AsyncSession asyncSession(SessionConfig sessionConfig) {
    return new TracingAsyncSession(driver.asyncSession(sessionConfig), tracer);
  }

  @Override
  public void close() {
    driver.close();
  }

  @Override
  public CompletionStage<Void> closeAsync() {
    return driver.closeAsync();
  }

  @Override
  public Metrics metrics() {
    return driver.metrics();
  }

  @Override
  public boolean isMetricsEnabled() {
    return driver.isMetricsEnabled();
  }

  @Override
  public TypeSystem defaultTypeSystem() {
    return driver.defaultTypeSystem();
  }

  @Override
  public void verifyConnectivity() {
    driver.verifyConnectivity();
  }

  @Override
  public CompletionStage<Void> verifyConnectivityAsync() {
    return driver.verifyConnectivityAsync();
  }

  @Override
  public boolean supportsMultiDb() {
    return driver.supportsMultiDb();
  }

  @Override
  public CompletionStage<Boolean> supportsMultiDbAsync() {
    return driver.supportsMultiDbAsync();
  }
}
