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
package me.j360.trace.storage.elasticsearch;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import me.j360.trace.core.Codec;
import me.j360.trace.core.Span;
import me.j360.trace.core.internal.ApplyTimestampAndDuration;
import me.j360.trace.storage.core.guava.GuavaSpanConsumer;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Futures.transform;
import static me.j360.trace.storage.elasticsearch.ElasticFutures.toGuava;

final class ElasticsearchSpanConsumer implements GuavaSpanConsumer {
  private static final byte[] TIMESTAMP_MILLIS_PREFIX = "{\"timestamp_millis\":".getBytes();
  private static final Function<Object, Void> TO_VOID = Functions.<Void>constant(null);

  private final Client client;
  private final IndexNameFormatter indexNameFormatter;

  ElasticsearchSpanConsumer(Client client, IndexNameFormatter indexNameFormatter) {
    this.client = client;
    this.indexNameFormatter = indexNameFormatter;
  }

  @Override
  public ListenableFuture<Void> accept(List<Span> spans) {
    if (spans.isEmpty()) return Futures.immediateFuture(null);

    // Create a bulk request when there is more than one span to store
    ListenableFuture<?> future;
    if (spans.size() == 1) {
      future = toGuava(createSpanIndexRequest(spans.get(0)).execute());
    } else {
      BulkRequestBuilder request = client.prepareBulk();
      for (Span span : spans) {
        request.add(createSpanIndexRequest(span));
      }
      future = toGuava(request.execute());
    }

    if (ElasticsearchStorage.FLUSH_ON_WRITES) {
      future = transform(future, new AsyncFunction() {
        @Override public ListenableFuture apply(Object input) {
          return toGuava(client.admin().indices()
              .prepareFlush(indexNameFormatter.catchAll())
              .execute());
        }
      });
    }

    return transform(future, TO_VOID);
  }

  private IndexRequestBuilder createSpanIndexRequest(Span input) {
    Span span = ApplyTimestampAndDuration.apply(input);
    long timestampMillis;
    final byte[] spanBytes;
    if (span.timestamp != null) {
      timestampMillis = TimeUnit.MICROSECONDS.toMillis(span.timestamp);
      spanBytes = prefixWithTimestampMillis(Codec.JSON.writeSpan(span), timestampMillis);
    } else {
      timestampMillis = System.currentTimeMillis();
      spanBytes = Codec.JSON.writeSpan(span);
    }
    String spanIndex = indexNameFormatter.indexNameForTimestamp(timestampMillis);
    return client.prepareIndex(spanIndex, ElasticsearchConstants.SPAN)
        .setSource(spanBytes);
  }

  /**
   * In order to allow systems like Kibana to search by timestamp, we add a field "timestamp_millis"
   * when storing. The cheapest way to do this without changing the codec is prefixing it to the
   * json. For example. {"traceId":"... becomes {"timestamp_millis":12345,"traceId":"...
   */
  @VisibleForTesting
  static byte[] prefixWithTimestampMillis(byte[] input, long timestampMillis) {
    String dateAsString = Long.toString(timestampMillis);
    byte[] newSpanBytes =
        new byte[TIMESTAMP_MILLIS_PREFIX.length + dateAsString.length() + input.length];
    int pos = 0;
    System.arraycopy(TIMESTAMP_MILLIS_PREFIX, 0, newSpanBytes, pos, TIMESTAMP_MILLIS_PREFIX.length);
    pos += TIMESTAMP_MILLIS_PREFIX.length;
    for (int i = 0, length = dateAsString.length(); i < length; i++) {
      newSpanBytes[pos++] = (byte) dateAsString.charAt(i);
    }
    newSpanBytes[pos++] = ',';
    // starting at position 1 discards the old head of '{'
    System.arraycopy(input, 1, newSpanBytes, pos, input.length - 1);
    return newSpanBytes;
  }
}
