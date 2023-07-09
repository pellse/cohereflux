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

package io.github.pellse.util.reactive;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;

import static io.github.pellse.util.collection.CollectionUtils.toLinkedHashMap;
import static java.lang.Math.toIntExact;
import static java.util.List.copyOf;
import static reactor.core.publisher.Flux.concat;

public interface ReactiveUtils {

    static <ID, R> Mono<Map<ID, List<R>>> resolve(Map<ID, Mono<List<R>>> monoMap) {

        final var monoLinkedMap = toLinkedHashMap(monoMap);
        final var keys = copyOf(monoLinkedMap.keySet());

        return concat(monoLinkedMap.values())
                .index()
                .collectMap(tuple -> keys.get(toIntExact(tuple.getT1())), Tuple2::getT2);
    }
}
