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
package me.j360.trace.core.storage;


import me.j360.trace.core.Span;

import java.util.List;
import java.util.concurrent.Executor;

import static me.j360.trace.core.internal.Util.checkNotNull;


final class InternalBlockingToAsyncSpanStoreAdapter implements AsyncSpanStore {
  final SpanStore delegate;
  final Executor executor;

  InternalBlockingToAsyncSpanStoreAdapter(SpanStore delegate, Executor executor) {
    this.delegate = checkNotNull(delegate, "delegate");
    this.executor = checkNotNull(executor, "executor");
  }

  @Override public void getTraces(final QueryRequest request, Callback<List<List<Span>>> callback) {
    executor.execute(new InternalCallbackRunnable<List<List<Span>>>(callback) {
      @Override List<List<Span>> complete() {
        return delegate.getTraces(request);
      }

      @Override public String toString() {
        return "GetTraces(" + request + ")";
      }
    });
  }

  @Override public void getTrace(final long id, Callback<List<Span>> callback) {
    executor.execute(new InternalCallbackRunnable<List<Span>>(callback) {
      @Override List<Span> complete() {
        return delegate.getTrace(id);
      }

      @Override public String toString() {
        return "getTrace(" + id + ")";
      }
    });
  }

  @Override public void getRawTrace(final long traceId, Callback<List<Span>> callback) {
    executor.execute(new InternalCallbackRunnable<List<Span>>(callback) {
      @Override List<Span> complete() {
        return delegate.getRawTrace(traceId);
      }

      @Override public String toString() {
        return "getRawTrace(" + traceId + ")";
      }
    });
  }

  @Override public void getServiceNames(Callback<List<String>> callback) {
    executor.execute(new InternalCallbackRunnable<List<String>>(callback) {
      @Override List<String> complete() {
        return delegate.getServiceNames();
      }

      @Override public String toString() {
        return "getServiceNames()";
      }
    });
  }

  @Override public void getSpanNames(final String serviceName, Callback<List<String>> callback) {
    executor.execute(new InternalCallbackRunnable<List<String>>(callback) {
      @Override List<String> complete() {
        return delegate.getSpanNames(serviceName);
      }

      @Override public String toString() {
        return "getSpanNames(" + serviceName + ")";
      }
    });
  }

  /*@Override public void getDependencies(final long endTs, final @Nullable Long lookback,
      Callback<List<DependencyLink>> callback) {
    executor.execute(new InternalCallbackRunnable<List<DependencyLink>>(callback) {
      @Override List<DependencyLink> complete() {
        return delegate.getDependencies(endTs, lookback);
      }

      @Override public String toString() {
        return "getDependencies(" + endTs + lookback == null ? ")" : ", " + lookback + ")";
      }
    });
  }*/

  @Override public String toString() {
    return delegate.toString();
  }
}
