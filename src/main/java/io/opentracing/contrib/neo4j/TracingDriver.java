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

import io.opentracing.Tracer;
import java.util.concurrent.CompletionStage;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;

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
  public Session session(AccessMode accessMode) {
    return new TracingSession(driver.session(accessMode), tracer);
  }

  @Override
  public Session session(String s) {
    return new TracingSession(driver.session(s), tracer);
  }

  @Override
  public Session session(AccessMode accessMode,
      String s) {
    return new TracingSession(driver.session(accessMode, s), tracer);
  }

  @Override
  public Session session(Iterable<String> iterable) {
    return new TracingSession(driver.session(iterable), tracer);
  }

  @Override
  public Session session(AccessMode accessMode,
      Iterable<String> iterable) {
    return new TracingSession(driver.session(accessMode, iterable), tracer);
  }

  @Override
  public void close() {
    driver.close();
  }

  @Override
  public CompletionStage<Void> closeAsync() {
    return driver.closeAsync();
  }
}
