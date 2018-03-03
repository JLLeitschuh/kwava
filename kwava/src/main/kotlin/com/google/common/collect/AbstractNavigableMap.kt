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

import com.google.common.annotations.GwtIncompatible
import com.google.common.collect.Maps.IteratorBasedAbstractMap
import java.util.NavigableMap
import java.util.NavigableSet
import java.util.NoSuchElementException
import java.util.SortedMap


/**
 * Skeletal implementation of [NavigableMap].
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
internal abstract class AbstractNavigableMap<K, V> : IteratorBasedAbstractMap<K, V>(), NavigableMap<K, V> {

    abstract override operator fun get(key: Any?): V

    override fun firstEntry(): Entry<K, V>? {
        return Iterators.getNext<Entry<K, V>>(entryIterator(), null)
    }

    override fun lastEntry(): Entry<K, V>? {
        return Iterators.getNext<Entry<K, V>>(descendingEntryIterator(), null)
    }

    override fun pollFirstEntry(): Entry<K, V>? {
        return Iterators.pollNext<Entry<K, V>>(entryIterator())
    }

    override fun pollLastEntry(): Entry<K, V>? {
        return Iterators.pollNext<Entry<K, V>>(descendingEntryIterator())
    }

    override fun firstKey(): K {
        val entry = firstEntry()
        return if (entry == null) {
            throw NoSuchElementException()
        } else {
            entry!!.key
        }
    }

    override fun lastKey(): K {
        val entry = lastEntry()
        return if (entry == null) {
            throw NoSuchElementException()
        } else {
            entry!!.key
        }
    }

    override fun lowerEntry(key: K): Entry<K, V> {
        return headMap(key, false).lastEntry()
    }

    override fun floorEntry(key: K): Entry<K, V> {
        return headMap(key, true).lastEntry()
    }

    override fun ceilingEntry(key: K): Entry<K, V> {
        return tailMap(key, true).firstEntry()
    }

    override fun higherEntry(key: K): Entry<K, V> {
        return tailMap(key, false).firstEntry()
    }

    override fun lowerKey(key: K): K? {
        return Maps.keyOrNull<K>(lowerEntry(key))
    }

    override fun floorKey(key: K): K? {
        return Maps.keyOrNull<K>(floorEntry(key))
    }

    override fun ceilingKey(key: K): K? {
        return Maps.keyOrNull<K>(ceilingEntry(key))
    }

    override fun higherKey(key: K): K? {
        return Maps.keyOrNull<K>(higherEntry(key))
    }

    internal abstract fun descendingEntryIterator(): Iterator<Entry<K, V>>

    override fun subMap(fromKey: K, toKey: K): SortedMap<K, V> {
        return subMap(fromKey, true, toKey, false)
    }

    override fun headMap(toKey: K): SortedMap<K, V> {
        return headMap(toKey, false)
    }

    override fun tailMap(fromKey: K): SortedMap<K, V> {
        return tailMap(fromKey, true)
    }

    override fun navigableKeySet(): NavigableSet<K> {
        return Maps.NavigableKeySet(this)
    }

    override fun keySet(): Set<K> {
        return navigableKeySet()
    }

    override fun descendingKeySet(): NavigableSet<K> {
        return descendingMap().navigableKeySet()
    }

    override fun descendingMap(): NavigableMap<K, V> {
        return DescendingMap()
    }

    private inner class DescendingMap : Maps.DescendingMap<K, V>() {
        internal override fun forward(): NavigableMap<K, V> {
            return this@AbstractNavigableMap
        }

        internal override fun entryIterator(): Iterator<Entry<K, V>> {
            return descendingEntryIterator()
        }
    }
}
