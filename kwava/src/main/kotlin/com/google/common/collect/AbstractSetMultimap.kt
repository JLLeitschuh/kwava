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
import kotlin.collections.Map.Entry


/**
 * Basic implementation of the [SetMultimap] interface. It's a wrapper around [ ] that converts the returned collections into `Sets`. The [ ][.createCollection] method must return a `Set`.
 *
 * @author Jared Levy
 */
@GwtCompatible
internal abstract class AbstractSetMultimap<K, V>
/**
 * Creates a new multimap that uses the provided map.
 *
 * @param map place to store the mapping from each key to its corresponding values
 */
protected constructor(map: MutableMap<K, Collection<V>>) : AbstractMapBasedMultimap<K, V>(map), SetMultimap<K, V> {

    internal abstract override fun createCollection(): MutableSet<V>

    internal override fun createUnmodifiableEmptyCollection(): Set<V> {
        return emptySet()
    }

    internal override fun <E> unmodifiableCollectionSubclass(collection: Collection<E>): Collection<E> {
        return Collections.unmodifiableSet(collection as Set<E>)
    }

    internal override fun wrapCollection(key: K, collection: MutableCollection<V>): Collection<V> {
        return AbstractMapBasedMultimap.WrappedSet(key, collection as Set<V>)
    }

    // Following Javadoc copied from SetMultimap.

    /**
     * {@inheritDoc}
     *
     *
     * Because a `SetMultimap` has unique values for a given key, this method returns a
     * [Set], instead of the [Collection] specified in the [Multimap] interface.
     */
    override fun get(key: K): Set<V> {
        return super.get(key) as Set<V>
    }

    /**
     * {@inheritDoc}
     *
     *
     * Because a `SetMultimap` has unique values for a given key, this method returns a
     * [Set], instead of the [Collection] specified in the [Multimap] interface.
     */
    override fun entries(): Set<Entry<K, V>> {
        return super.entries() as Set<Entry<K, V>>
    }

    /**
     * {@inheritDoc}
     *
     *
     * Because a `SetMultimap` has unique values for a given key, this method returns a
     * [Set], instead of the [Collection] specified in the [Multimap] interface.
     */

    override fun removeAll(key: Any): Set<V> {
        return super.removeAll(key) as Set<V>
    }

    /**
     * {@inheritDoc}
     *
     *
     * Because a `SetMultimap` has unique values for a given key, this method returns a
     * [Set], instead of the [Collection] specified in the [Multimap] interface.
     *
     *
     * Any duplicates in `values` will be stored in the multimap once.
     */

    override fun replaceValues(key: K, values: Iterable<V>): Set<V> {
        return super.replaceValues(key, values) as Set<V>
    }

    /**
     * {@inheritDoc}
     *
     *
     * Though the method signature doesn't say so explicitly, the returned map has [Set]
     * values.
     */
    override fun asMap(): Map<K, Collection<V>> {
        return super.asMap()
    }

    /**
     * Stores a key-value pair in the multimap.
     *
     * @param key key to store in the multimap
     * @param value value to store in the multimap
     * @return `true` if the method increased the size of the multimap, or `false` if the
     * multimap already contained the key-value pair
     */

    override fun put(key: K, value: V): Boolean {
        return super.put(key, value)
    }

    /**
     * Compares the specified object to this multimap for equality.
     *
     *
     * Two `SetMultimap` instances are equal if, for each key, they contain the same values.
     * Equality does not depend on the ordering of keys or values.
     */
    override fun equals(`object`: Any?): Boolean {
        return super.equals(`object`)
    }

    companion object {

        private val serialVersionUID = 7431625294878419160L
    }
}
