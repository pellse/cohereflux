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

import io.github.pellse.concurrent.ReentrantExecutor;
import reactor.core.publisher.Mono;

import java.util.Map;

import static java.util.Map.of;
import static reactor.core.publisher.Mono.just;

public interface ConcurrentCache<ID, RRC> extends Cache<ID, RRC> {

    static <ID, RRC> ConcurrentCache<ID, RRC> concurrentCache(Cache<ID, RRC> delegateCache) {

        if (delegateCache instanceof ConcurrentCache<ID, RRC> concurrentCache) {
            return concurrentCache;
        }

        final var executor = ReentrantExecutor.create();

        return new ConcurrentCache<>() {

            @Override
            public Mono<Map<ID, RRC>> getAll(Iterable<ID> ids) {
                return executor.withReadLock(delegateCache.getAll(ids), just(of()));
            }

            @Override
            public Mono<Map<ID, RRC>> computeAll(Iterable<ID> ids, FetchFunction<ID, RRC> fetchFunction) {
                return executor.withReadLock(writeLockExecutor -> delegateCache.computeAll(ids, IdsToFetch -> writeLockExecutor.withWriteLock(fetchFunction.apply(IdsToFetch))), just(of()));
            }

            @Override
            public Mono<?> putAll(Map<ID, RRC> map) {
                return executor.withWriteLock(delegateCache.putAll(map));
            }

            @Override
            public Mono<?> removeAll(Map<ID, RRC> map) {
                return executor.withWriteLock(delegateCache.removeAll(map));
            }

            @Override
            public Mono<?> updateAll(Map<ID, RRC> mapToAdd, Map<ID, RRC> mapToRemove) {
                return executor.withWriteLock(delegateCache.updateAll(mapToAdd, mapToRemove));
            }
        };
    }
}
