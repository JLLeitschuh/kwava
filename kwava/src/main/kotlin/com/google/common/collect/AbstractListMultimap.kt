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


/**
 * Basic implementation of the [ListMultimap] interface. It's a wrapper around [ ] that converts the returned collections into `Lists`. The [ ][.createCollection] method must return a `List`.
 *
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible
internal abstract class AbstractListMultimap<K, V>
/**
 * Creates a new multimap that uses the provided map.
 *
 * @param map place to store the mapping from each key to its corresponding values
 */
protected constructor(map: MutableMap<K, Collection<V>>) : AbstractMapBasedMultimap<K, V>(map), ListMultimap<K, V> {

    internal abstract override fun createCollection(): MutableList<V>

    internal override fun createUnmodifiableEmptyCollection(): List<V> {
        return emptyList()
    }

    internal override fun <E> unmodifiableCollectionSubclass(collection: Collection<E>): Collection<E> {
        return Collections.unmodifiableList(collection as List<E>)
    }

    internal override fun wrapCollection(key: K, collection: MutableCollection<V>): Collection<V> {
        return wrapList(key, collection as List<V>, null)
    }

    // Following Javadoc copied from ListMultimap.

    /**
     * {@inheritDoc}
     *
     *
     * Because the values for a given key may have duplicates and follow the insertion ordering,
     * this method returns a [List], instead of the [Collection] specified in the [ ] interface.
     */
    override fun get(key: K): List<V> {
        return super.get(key) as List<V>
    }

    /**
     * {@inheritDoc}
     *
     *
     * Because the values for a given key may have duplicates and follow the insertion ordering,
     * this method returns a [List], instead of the [Collection] specified in the [ ] interface.
     */

    override fun removeAll(key: Any): List<V> {
        return super.removeAll(key) as List<V>
    }

    /**
     * {@inheritDoc}
     *
     *
     * Because the values for a given key may have duplicates and follow the insertion ordering,
     * this method returns a [List], instead of the [Collection] specified in the [ ] interface.
     */

    override fun replaceValues(key: K, values: Iterable<V>): List<V> {
        return super.replaceValues(key, values) as List<V>
    }

    /**
     * Stores a key-value pair in the multimap.
     *
     * @param key key to store in the multimap
     * @param value value to store in the multimap
     * @return `true` always
     */

    override fun put(key: K, value: V): Boolean {
        return super.put(key, value)
    }

    /**
     * {@inheritDoc}
     *
     *
     * Though the method signature doesn't say so explicitly, the returned map has [List]
     * values.
     */
    override fun asMap(): Map<K, Collection<V>> {
        return super.asMap()
    }

    /**
     * Compares the specified object to this multimap for equality.
     *
     *
     * Two `ListMultimap` instances are equal if, for each key, they contain the same values
     * in the same order. If the value orderings disagree, the multimaps will not be considered equal.
     */
    override fun equals(`object`: Any?): Boolean {
        return super.equals(`object`)
    }

    companion object {

        private val serialVersionUID = 6588350623831699109L
    }
}
