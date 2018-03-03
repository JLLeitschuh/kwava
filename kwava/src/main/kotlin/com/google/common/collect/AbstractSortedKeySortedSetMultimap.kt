/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect

import com.google.common.annotations.GwtCompatible
import java.util.SortedMap
import java.util.SortedSet

/**
 * Basic implementation of a [SortedSetMultimap] with a sorted key set.
 *
 *
 * This superclass allows `TreeMultimap` to override methods to return navigable set and
 * map types in non-GWT only, while GWT code will inherit the SortedMap/SortedSet overrides.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
internal abstract class AbstractSortedKeySortedSetMultimap<K, V>(map: SortedMap<K, Collection<V>>) : AbstractSortedSetMultimap<K, V>(map) {

    override fun asMap(): SortedMap<K, Collection<V>> {
        return super.asMap() as SortedMap<K, Collection<V>>
    }

    internal override fun backingMap(): SortedMap<K, Collection<V>>? {
        return super.backingMap() as SortedMap<K, Collection<V>>
    }

    override fun keySet(): SortedSet<K> {
        return super.keySet() as SortedSet<K>
    }

    internal override fun createKeySet(): Set<K> {
        return createMaybeNavigableKeySet()
    }
}
