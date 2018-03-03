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

import com.google.common.base.Preconditions.checkNotNull

import com.google.common.annotations.GwtCompatible

import com.google.j2objc.annotations.WeakOuter
import java.util.AbstractCollection
import kotlin.collections.Map.Entry
import java.util.Spliterator
import java.util.Spliterators


/**
 * A skeleton `Multimap` implementation, not necessarily in terms of a `Map`.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
internal abstract class AbstractMultimap<K, V> : Multimap<K, V> {


    @Transient
    private var entries: Collection<Entry<K, V>>? = null


    @Transient
    private var keySet: Set<K>? = null


    @Transient
    private var keys: Multiset<K>? = null


    @Transient
    private var values: Collection<V>? = null


    @Transient
    private var asMap: Map<K, Collection<V>>? = null

    override fun isEmpty(): Boolean {
        return size() == 0
    }

    override fun containsValue(value: Any?): Boolean {
        for (collection in asMap().values) {
            if (collection.contains(value)) {
                return true
            }
        }

        return false
    }

    override fun containsEntry(key: Any, value: Any): Boolean {
        val collection = asMap().get(key)
        return collection != null && collection.contains(value)
    }


    override fun remove(key: Any, value: Any): Boolean {
        val collection = asMap().get(key)
        return collection != null && collection.remove(value)
    }


    override fun put(key: K, value: V): Boolean {
        return get(key).add(value)
    }


    override fun putAll(key: K, values: Iterable<V>): Boolean {
        checkNotNull(values)
        // make sure we only call values.iterator() once
        // and we only call get(key) if values is nonempty
        if (values is Collection<*>) {
            val valueCollection = values as Collection<V>
            return !valueCollection.isEmpty() && get(key).addAll(valueCollection)
        } else {
            val valueItr = values.iterator()
            return valueItr.hasNext() && Iterators.addAll(get(key), valueItr)
        }
    }


    override fun putAll(multimap: Multimap<out K, out V>): Boolean {
        var changed = false
        for ((key, value) in multimap.entries()) {
            changed = changed or put(key, value)
        }
        return changed
    }


    override fun replaceValues(key: K, values: Iterable<V>): Collection<V> {
        checkNotNull(values)
        val result = removeAll(key)
        putAll(key, values)
        return result
    }

    override fun entries(): Collection<Entry<K, V>> {
        val result = entries
        return result ?: (entries = createEntries())
    }

    internal abstract fun createEntries(): Collection<Entry<K, V>>

    @WeakOuter
    internal open inner class Entries : Multimaps.Entries<K, V>() {
        internal override fun multimap(): Multimap<K, V> {
            return this@AbstractMultimap
        }

        override fun iterator(): Iterator<Entry<K, V>> {
            return entryIterator()
        }

        override fun spliterator(): Spliterator<Entry<K, V>> {
            return entrySpliterator()
        }
    }

    @WeakOuter
    internal inner class EntrySet : Entries(), Set<Entry<K, V>> {
        override fun hashCode(): Int {
            return Sets.hashCodeImpl(this)
        }

        override fun equals(obj: Any?): Boolean {
            return Sets.equalsImpl(this, obj)
        }
    }

    internal abstract fun entryIterator(): Iterator<Entry<K, V>>

    internal open fun entrySpliterator(): Spliterator<Entry<K, V>> {
        return Spliterators.spliterator(
                entryIterator(), size().toLong(), if (this is SetMultimap<*, *>) Spliterator.DISTINCT else 0)
    }

    override fun keySet(): Set<K> {
        val result = keySet
        return result ?: (keySet = createKeySet())
    }

    internal abstract fun createKeySet(): Set<K>

    override fun keys(): Multiset<K> {
        val result = keys
        return result ?: (keys = createKeys())
    }

    internal abstract fun createKeys(): Multiset<K>

    override fun values(): Collection<V> {
        val result = values
        return result ?: (values = createValues())
    }

    internal abstract fun createValues(): Collection<V>

    @WeakOuter
    internal inner class Values : AbstractCollection<V>() {
        override fun iterator(): Iterator<V> {
            return valueIterator()
        }

        override fun spliterator(): Spliterator<V> {
            return valueSpliterator()
        }

        override fun size(): Int {
            return this@AbstractMultimap.size()
        }

        override operator fun contains(o: Any?): Boolean {
            return this@AbstractMultimap.containsValue(o)
        }

        override fun clear() {
            this@AbstractMultimap.clear()
        }
    }

    internal open fun valueIterator(): Iterator<V> {
        return Maps.valueIterator(entries().iterator())
    }

    internal open fun valueSpliterator(): Spliterator<V> {
        return Spliterators.spliterator(valueIterator(), size().toLong(), 0)
    }

    override fun asMap(): Map<K, Collection<V>> {
        val result = asMap
        return result ?: (asMap = createAsMap())
    }

    internal abstract fun createAsMap(): Map<K, Collection<V>>

    // Comparison and hashing

    override fun equals(`object`: Any?): Boolean {
        return Multimaps.equalsImpl(this, `object`)
    }

    /**
     * Returns the hash code for this multimap.
     *
     *
     * The hash code of a multimap is defined as the hash code of the map view, as returned by
     * [Multimap.asMap].
     *
     * @see Map.hashCode
     */
    override fun hashCode(): Int {
        return asMap().hashCode()
    }

    /**
     * Returns a string representation of the multimap, generated by calling `toString` on the
     * map returned by [Multimap.asMap].
     *
     * @return a string representation of the multimap
     */
    override fun toString(): String {
        return asMap().toString()
    }
}
