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

package io.github.pellse.reactive.assembler.cache.caffeine;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pellse.reactive.assembler.caching.CacheFactory;

import java.util.List;
import java.util.function.Function;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static io.github.pellse.reactive.assembler.caching.Cache.adapterCache;
import static io.github.pellse.reactive.assembler.caching.CacheFactory.toMono;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static reactor.core.publisher.Mono.fromFuture;
import static reactor.core.scheduler.Schedulers.fromExecutor;

public interface CaffeineCacheFactory {

    static <ID, R, RRC> CacheFactory<ID, R, RRC> caffeineCache() {
        return caffeineCache(newBuilder());
    }

    static <ID, R, RRC> CacheFactory<ID, R, RRC> caffeineCache(Function<Caffeine<Object, Object>, Caffeine<Object, Object>> customizer) {
        return caffeineCache(customizer.apply(newBuilder()));
    }

    static <ID, R, RRC> CacheFactory<ID, R, RRC> caffeineCache(Caffeine<Object, Object> caffeine) {

        final AsyncCache<ID, List<R>> delegateCache = caffeine.buildAsync();

        return (fetchFunction, __) -> adapterCache(
                (ids, computeIfAbsent) -> fromFuture(delegateCache.getAll(ids, (keys, executor) ->
                        computeIfAbsent
                                ? fetchFunction.apply(keys).subscribeOn(fromExecutor(executor)).toFuture()
                                : completedFuture(emptyMap()))),
                toMono(map -> delegateCache.synchronous().putAll(map)),
                toMono(map -> delegateCache.synchronous().invalidateAll(map.keySet()))
        );
    }
}
