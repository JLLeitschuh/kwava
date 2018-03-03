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


/**
 * A [MapIteratorCache] that adds additional caching. In addition to the caching provided by
 * [MapIteratorCache], this structure caches values for the two most recently retrieved keys.
 *
 * @author James Sexton
 */
internal class MapRetrievalCache<K, V>(backingMap: Map<K, V>) : MapIteratorCache<K, V>(backingMap) {
    @Transient
    private var cacheEntry1: CacheEntry<K, V>? = null
    @Transient
    private var cacheEntry2: CacheEntry<K, V>? = null

    override// Safe because we only cast if key is found in map.
    fun get(key: Any): V? {
        var value = getIfCached(key)
        if (value != null) {
            return value
        }

        value = getWithoutCaching(key)
        if (value != null) {
            addToCache(key as K, value)
        }
        return value
    }

    // Internal methods ('protected' is still package-visible, but treat as only subclass-visible)

    override fun getIfCached(key: Any?): V? {
        val value = super.getIfCached(key)
        if (value != null) {
            return value
        }

        // Store a local reference to the cache entry. If the backing map is immutable, this,
        // in combination with immutable cache entries, will ensure a thread-safe cache.
        var entry: CacheEntry<K, V>?

        // Check cache. We use == on purpose because it's cheaper and a cache miss is ok.
        entry = cacheEntry1
        if (entry != null && entry.key === key) {
            return entry.value
        }
        entry = cacheEntry2
        if (entry != null && entry.key === key) {
            // Promote second cache entry to first so the access pattern
            // [K1, K2, K1, K3, K1, K4...] still hits the cache half the time.
            addToCache(entry)
            return entry.value
        }
        return null
    }

    override fun clearCache() {
        super.clearCache()
        cacheEntry1 = null
        cacheEntry2 = null
    }

    private fun addToCache(key: K, value: V) {
        addToCache(CacheEntry(key, value))
    }

    private fun addToCache(entry: CacheEntry<K, V>) {
        // Slide new entry into first cache position. Drop previous entry in second cache position.
        cacheEntry2 = cacheEntry1
        cacheEntry1 = entry
    }

    private class CacheEntry<K, V> internal constructor(internal val key: K, internal val value: V)
}
