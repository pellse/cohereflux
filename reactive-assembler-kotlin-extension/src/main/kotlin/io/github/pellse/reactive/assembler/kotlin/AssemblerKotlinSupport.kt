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

package io.github.pellse.reactive.assembler.kotlin

import io.github.pellse.reactive.assembler.AssemblerBuilder.WithCorrelationIdExtractorBuilder
import io.github.pellse.reactive.assembler.AssemblerBuilder.assemblerOf
import io.github.pellse.reactive.assembler.RuleMapper
import io.github.pellse.reactive.assembler.RuleMapper.oneToMany
import io.github.pellse.reactive.assembler.RuleMapper.oneToOne
import io.github.pellse.reactive.assembler.RuleMapperSource
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux.fromIterable

inline fun <reified T> assembler(): WithCorrelationIdExtractorBuilder<T> = assemblerOf(T::class.java)

fun <ID, IDC : Collection<ID>, R> ((IDC) -> Iterable<R>).toPublisher(): (IDC) -> Publisher<R> = { ids -> fromIterable(this(ids)) }

fun <ID, IDC : Collection<ID>, R> ((IDC) -> Publisher<R>).oneToOne(defaultResultProvider: (ID) -> R): RuleMapper<ID, IDC, R, R> =
    oneToOne(this, defaultResultProvider)

fun <ID, IDC : Collection<ID>, R> RuleMapperSource<ID, ID, IDC, R, R>.oneToOne(defaultResultProvider: (ID) -> R): RuleMapper<ID, IDC, R, R> =
    oneToOne(this, defaultResultProvider)

fun <ID, EID, IDC : Collection<ID>, R> ((IDC) -> Publisher<R>).oneToMany(idExtractor: (R) -> EID): RuleMapper<ID, IDC, R, List<R>> =
    oneToMany(idExtractor, this)

fun <ID, EID, IDC : Collection<ID>, R> RuleMapperSource<ID, EID, IDC, R, List<R>>.oneToMany(idExtractor: (R) -> EID): RuleMapper<ID, IDC, R, List<R>> =
    oneToMany(idExtractor, this)

fun <ID, EID, IDC : Collection<ID>, R, RC : Collection<R>> ((IDC) -> Publisher<R>).oneToMany(idExtractor: (R) -> EID, collectionFactory: () -> RC): RuleMapper<ID, IDC, R, RC> =
    oneToMany(idExtractor, this, collectionFactory)

fun <ID, EID, IDC : Collection<ID>, R, RC : Collection<R>> RuleMapperSource<ID, EID, IDC, R, RC>.oneToMany(idExtractor: (R) -> EID, collectionFactory: () -> RC): RuleMapper<ID, IDC, R, RC> =
    oneToMany(idExtractor, this, collectionFactory)
