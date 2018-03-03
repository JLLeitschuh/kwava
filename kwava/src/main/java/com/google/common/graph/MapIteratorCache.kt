/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph

import com.google.common.base.Preconditions.checkNotNull

import com.google.common.collect.UnmodifiableIterator

import java.util.AbstractSet
import kotlin.collections.Map.Entry


/**
 * A map-like data structure that wraps a backing map and caches values while iterating through
 * [.unmodifiableKeySet]. By design, the cache is cleared when this structure is mutated. If
 * this structure is never mutated, it provides a thread-safe view of the backing map.
 *
 *
 * The [MapIteratorCache] assumes ownership of the backing map, and cannot guarantee
 * correctness in the face of external mutations to the backing map. As such, it is **strongly**
 * recommended that the caller does not persist a reference to the backing map (unless the backing
 * map is immutable).
 *
 *
 * This class is tailored toward use cases in common.graph. It is *NOT* a general purpose map.
 *
 * @author James Sexton
 */
internal open class MapIteratorCache<K, V>(backingMap: Map<K, V>) {
    private val backingMap: MutableMap<K, V>

    // Per JDK: "the behavior of a map entry is undefined if the backing map has been modified after
    // the entry was returned by the iterator, except through the setValue operation on the map entry"
    // As such, this field must be cleared before every map mutation.
    @Transient
    private var entrySetCache: Entry<K, V>? = null

    init {
        this.backingMap = checkNotNull(backingMap)
    }


    fun put(key: K, value: V): V {
        clearCache()
        return backingMap.put(key, value)
    }


    fun remove(key: Any): V {
        clearCache()
        return backingMap.remove(key)
    }

    fun clear() {
        clearCache()
        backingMap.clear()
    }

    open operator fun get(key: Any): V {
        val value = getIfCached(key)
        return value ?: getWithoutCaching(key)
    }

    fun getWithoutCaching(key: Any): V {
        return backingMap.get(key)
    }

    fun containsKey(key: Any?): Boolean {
        return getIfCached(key) != null || backingMap.containsKey(key)
    }

    fun unmodifiableKeySet(): Set<K> {
        return object : AbstractSet<K>() {
            override fun iterator(): UnmodifiableIterator<K> {
                val entryIterator = backingMap.entries.iterator()

                return object : UnmodifiableIterator<K>() {
                    override fun hasNext(): Boolean {
                        return entryIterator.hasNext()
                    }

                    override fun next(): K {
                        val entry = entryIterator.next() // store local reference for thread-safety
                        entrySetCache = entry
                        return entry.key
                    }
                }
            }

            override fun size(): Int {
                return backingMap.size
            }

            override operator fun contains(key: Any?): Boolean {
                return containsKey(key)
            }
        }
    }

    // Internal methods ('protected' is still package-visible, but treat as only subclass-visible)

    protected open fun getIfCached(key: Any?): V? {
        val entry = entrySetCache // store local reference for thread-safety

        // Check cache. We use == on purpose because it's cheaper and a cache miss is ok.
        return if (entry != null && entry.key === key) {
            entry.value
        } else null
    }

    protected open fun clearCache() {
        entrySetCache = null
    }
}
