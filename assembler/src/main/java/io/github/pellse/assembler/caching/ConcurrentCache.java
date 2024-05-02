/*
 * Copyright 2023 Sebastien Pelletier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pellse.assembler.caching;

import io.github.pellse.concurrent.ConcurrentExecutor;
import io.github.pellse.concurrent.ConcurrentExecutor.ConcurrencyStrategy;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.RetryBackoffSpec;
import reactor.util.retry.RetrySpec;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

import static io.github.pellse.concurrent.ConcurrentExecutor.ConcurrencyStrategy.READ;
import static io.github.pellse.concurrent.ConcurrentExecutor.ConcurrencyStrategy.WRITE;
import static io.github.pellse.concurrent.ConcurrentExecutor.concurrentExecutor;
import static java.util.Map.of;
import static reactor.core.publisher.Mono.just;
import static reactor.util.retry.Retry.indefinitely;

public interface ConcurrentCache<ID, RRC> extends Cache<ID, RRC> {

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache) {
        return concurrentCache(delegateCache, WRITE);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, ConcurrencyStrategy concurrencyStrategy) {
        return concurrentCache(delegateCache, indefinitely(), concurrencyStrategy);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, long maxAttempts) {
        return concurrentCache(delegateCache, maxAttempts, WRITE);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, long maxAttempts, ConcurrencyStrategy concurrencyStrategy) {
        return concurrentCache(delegateCache, () -> concurrentExecutor(maxAttempts), concurrencyStrategy);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, long maxAttempts, Duration minBackoff) {
        return concurrentCache(delegateCache, maxAttempts, minBackoff, WRITE);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, long maxAttempts, Duration minBackoff, ConcurrencyStrategy concurrencyStrategy) {
        return concurrentCache(delegateCache, () -> concurrentExecutor(maxAttempts, minBackoff), concurrencyStrategy);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, RetrySpec retrySpec) {
        return concurrentCache(delegateCache, retrySpec, WRITE);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, RetrySpec retrySpec, ConcurrencyStrategy concurrencyStrategy) {
        return concurrentCache(delegateCache, () -> concurrentExecutor(retrySpec), concurrencyStrategy);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, RetryBackoffSpec retrySpec) {
        return concurrentCache(delegateCache, retrySpec, WRITE);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, RetryBackoffSpec retrySpec, ConcurrencyStrategy concurrencyStrategy) {
        return concurrentCache(delegateCache, () -> concurrentExecutor(retrySpec), concurrencyStrategy);
    }

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, RetryBackoffSpec retrySpec, ConcurrencyStrategy concurrencyStrategy, Scheduler retryScheduler) {
        return concurrentCache(delegateCache, () -> concurrentExecutor(retrySpec, retryScheduler), concurrencyStrategy);
    }

    private static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache, Supplier<ConcurrentExecutor> executorSupplier, ConcurrencyStrategy concurrencyStrategy) {

        if (delegateCache instanceof ConcurrentCache<ID, RRC> concurrentCache) {
            return concurrentCache;
        }

        final var executor = executorSupplier.get();

        return new ConcurrentCache<>() {

            @Override
            public Mono<Map<ID, RRC>> getAll(Iterable<ID> ids) {
                return executor.execute(delegateCache.getAll(ids), READ, just(of()));
            }

            @Override
            public Mono<Map<ID, RRC>> computeAll(Iterable<ID> ids, FetchFunction<ID, RRC> fetchFunction) {
                return executor.execute(delegateCache.computeAll(ids, fetchFunction), concurrencyStrategy, just(of()));
            }

            @Override
            public Mono<?> putAll(Map<ID, RRC> map) {
                return executor.execute(delegateCache.putAll(map));
            }

            @Override
            public Mono<?> removeAll(Map<ID, RRC> map) {
                return executor.execute(delegateCache.removeAll(map));
            }

            @Override
            public Mono<?> updateAll(Map<ID, RRC> mapToAdd, Map<ID, RRC> mapToRemove) {
                return executor.execute(delegateCache.updateAll(mapToAdd, mapToRemove));
            }
        };
    }
}
