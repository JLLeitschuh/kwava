/*
 * Copyright (C) 2007 The Guava Authors
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
import java.util.Collections
import java.util.NavigableSet
import java.util.SortedSet


/**
 * Basic implementation of the [SortedSetMultimap] interface. It's a wrapper around [ ] that converts the returned collections into sorted sets. The [ ][.createCollection] method must return a `SortedSet`.
 *
 * @author Jared Levy
 */
@GwtCompatible
internal abstract class AbstractSortedSetMultimap<K, V>
/**
 * Creates a new multimap that uses the provided map.
 *
 * @param map place to store the mapping from each key to its corresponding values
 */
protected constructor(map: MutableMap<K, Collection<V>>) : AbstractSetMultimap<K, V>(map), SortedSetMultimap<K, V> {

    internal abstract override fun createCollection(): SortedSet<V>

    internal override fun createUnmodifiableEmptyCollection(): SortedSet<V> {
        return unmodifiableCollectionSubclass(createCollection())
    }

    internal override fun <E> unmodifiableCollectionSubclass(collection: Collection<E>): SortedSet<E> {
        return if (collection is NavigableSet<*>) {
            Sets.unmodifiableNavigableSet(collection as NavigableSet<E>)
        } else {
            Collections.unmodifiableSortedSet(collection as SortedSet<E>)
        }
    }

    internal override fun wrapCollection(key: K, collection: MutableCollection<V>): Collection<V> {
        return if (collection is NavigableSet<*>) {
            AbstractMapBasedMultimap.WrappedNavigableSet(key, collection as NavigableSet<V>, null)
        } else {
            AbstractMapBasedMultimap.WrappedSortedSet(key, collection as SortedSet<V>, null)
        }
    }

    // Following Javadoc copied from Multimap and SortedSetMultimap.

    /**
     * Returns a collection view of all values associated with a key. If no mappings in the multimap
     * have the provided key, an empty collection is returned.
     *
     *
     * Changes to the returned collection will update the underlying multimap, and vice versa.
     *
     *
     * Because a `SortedSetMultimap` has unique sorted values for a given key, this method
     * returns a [SortedSet], instead of the [Collection] specified in the [ ] interface.
     */
    override fun get(key: K): SortedSet<V> {
        return super.get(key) as SortedSet<V>
    }

    /**
     * Removes all values associated with a given key. The returned collection is immutable.
     *
     *
     * Because a `SortedSetMultimap` has unique sorted values for a given key, this method
     * returns a [SortedSet], instead of the [Collection] specified in the [ ] interface.
     */

    override fun removeAll(key: Any): SortedSet<V> {
        return super.removeAll(key) as SortedSet<V>
    }

    /**
     * Stores a collection of values with the same key, replacing any existing values for that key.
     * The returned collection is immutable.
     *
     *
     * Because a `SortedSetMultimap` has unique sorted values for a given key, this method
     * returns a [SortedSet], instead of the [Collection] specified in the [ ] interface.
     *
     *
     * Any duplicates in `values` will be stored in the multimap once.
     */

    override fun replaceValues(key: K, values: Iterable<V>): SortedSet<V> {
        return super.replaceValues(key, values) as SortedSet<V>
    }

    /**
     * Returns a map view that associates each key with the corresponding values in the multimap.
     * Changes to the returned map, such as element removal, will update the underlying multimap. The
     * map does not support `setValue` on its entries, `put`, or `putAll`.
     *
     *
     * When passed a key that is present in the map, `asMap().get(Object)` has the same
     * behavior as [.get], returning a live collection. When passed a key that is not present,
     * however, `asMap().get(Object)` returns `null` instead of an empty collection.
     *
     *
     * Though the method signature doesn't say so explicitly, the returned map has [ ] values.
     */
    override fun asMap(): Map<K, Collection<V>> {
        return super.asMap()
    }

    /**
     * {@inheritDoc}
     *
     *
     * Consequently, the values do not follow their natural ordering or the ordering of the value
     * comparator.
     */
    override fun values(): Collection<V> {
        return super.values()
    }

    companion object {

        private val serialVersionUID = 430848587173315748L
    }
}
