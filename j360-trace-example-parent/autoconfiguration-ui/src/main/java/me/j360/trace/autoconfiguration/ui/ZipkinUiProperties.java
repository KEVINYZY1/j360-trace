/**
 * Copyright 2015-2016 The OpenZipkin Authors
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
package me.j360.trace.autoconfiguration.ui;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

@ConfigurationProperties("zipkin.ui")
public class ZipkinUiProperties {
  private String environment;
  private int queryLimit = 10;
  private int defaultLookback = (int) TimeUnit.DAYS.toMillis(7);
  private String instrumented = ".*";

  public int getDefaultLookback() {
    return defaultLookback;
  }

  public void setDefaultLookback(int defaultLookback) {
    this.defaultLookback = defaultLookback;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public int getQueryLimit() {
    return queryLimit;
  }

  public void setQueryLimit(int queryLimit) {
    this.queryLimit = queryLimit;
  }

  public String getInstrumented() {
    return instrumented;
  }

  public void setInstrumented(String instrumented) {
    this.instrumented = instrumented;
  }
}
