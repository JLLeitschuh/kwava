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

import com.google.common.collect.CollectPreconditions.checkNonnegative

import com.google.common.annotations.GwtCompatible
import com.google.common.annotations.GwtIncompatible
import com.google.common.annotations.VisibleForTesting
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.ArrayList
import java.util.HashMap

/**
 * Implementation of `Multimap` that uses an `ArrayList` to store the values for a given
 * key. A [HashMap] associates each key with an [ArrayList] of values.
 *
 *
 * When iterating through the collections supplied by this class, the ordering of values for a
 * given key agrees with the order in which the values were added.
 *
 *
 * This multimap allows duplicate key-value pairs. After adding a new key-value pair equal to an
 * existing key-value pair, the `ArrayListMultimap` will contain entries for both the new
 * value and the old value.
 *
 *
 * Keys and values may be null. All optional multimap methods are supported, and all returned
 * views are modifiable.
 *
 *
 * The lists returned by [.get], [.removeAll], and [.replaceValues] all
 * implement [java.util.RandomAccess].
 *
 *
 * This class is not threadsafe when any concurrent operations update the multimap. Concurrent
 * read operations will work correctly. To allow concurrent update operations, wrap your multimap
 * with a call to [Multimaps.synchronizedListMultimap].
 *
 *
 * See the Guava User Guide article on [ `Multimap`](https://github.com/google/guava/wiki/NewCollectionTypesExplained#multimap).
 *
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
class ArrayListMultimap<K, V> private constructor(expectedKeys: Int = 12, @field:VisibleForTesting @field:Transient internal var expectedValuesPerKey: Int = DEFAULT_VALUES_PER_KEY) : ArrayListMultimapGwtSerializationDependencies<K, V>(Platform.newHashMapWithExpectedSize<K, Collection<V>>(expectedKeys)) {

    init {
        checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey")
    }

    private constructor(multimap: Multimap<out K, out V>) : this(
            multimap.keySet().size,
            if (multimap is ArrayListMultimap<*, *>)
                (multimap as ArrayListMultimap<*, *>).expectedValuesPerKey
            else
                DEFAULT_VALUES_PER_KEY) {
        putAll(multimap)
    }

    /**
     * Creates a new, empty `ArrayList` to hold the collection of values for an arbitrary key.
     */
    internal override fun createCollection(): MutableList<V> {
        return ArrayList(expectedValuesPerKey)
    }

    /**
     * Reduces the memory used by this `ArrayListMultimap`, if feasible.
     *
     */
    @Deprecated("For a {@link ListMultimap} that automatically trims to size, use {@link\n" +
            "   *     ImmutableListMultimap}. If you need a mutable collection, remove the {@code trimToSize}\n" +
            "        call, or switch to a {@code HashMap<K, ArrayList<V>>}.")
    fun trimToSize() {
        for (collection in backingMap()!!.values) {
            val arrayList = collection as ArrayList<V>
            arrayList.trimToSize()
        }
    }

    /**
     * @serialData expectedValuesPerKey, number of distinct keys, and then for each distinct key: the
     * key, number of values for that key, and the key's values
     */
    @GwtIncompatible // java.io.ObjectOutputStream
    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        stream.defaultWriteObject()
        Serialization.writeMultimap(this, stream)
    }

    @GwtIncompatible // java.io.ObjectOutputStream
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        stream.defaultReadObject()
        expectedValuesPerKey = DEFAULT_VALUES_PER_KEY
        val distinctKeys = Serialization.readCount(stream)
        val map = Maps.newHashMap<K, Collection<V>>()
        setMap(map)
        Serialization.populateMultimap(this, stream, distinctKeys)
    }

    companion object {
        // Default from ArrayList
        private val DEFAULT_VALUES_PER_KEY = 3

        /**
         * Creates a new, empty `ArrayListMultimap` with the default initial capacities.
         *
         *
         * This method will soon be deprecated in favor of `MultimapBuilder.hashKeys().arrayListValues().build()`.
         */
        fun <K, V> create(): ArrayListMultimap<K, V> {
            return ArrayListMultimap()
        }

        /**
         * Constructs an empty `ArrayListMultimap` with enough capacity to hold the specified
         * numbers of keys and values without resizing.
         *
         *
         * This method will soon be deprecated in favor of `MultimapBuilder.hashKeys(expectedKeys).arrayListValues(expectedValuesPerKey).build()`.
         *
         * @param expectedKeys the expected number of distinct keys
         * @param expectedValuesPerKey the expected average number of values per key
         * @throws IllegalArgumentException if `expectedKeys` or `expectedValuesPerKey` is
         * negative
         */
        fun <K, V> create(expectedKeys: Int, expectedValuesPerKey: Int): ArrayListMultimap<K, V> {
            return ArrayListMultimap(expectedKeys, expectedValuesPerKey)
        }

        /**
         * Constructs an `ArrayListMultimap` with the same mappings as the specified multimap.
         *
         *
         * This method will soon be deprecated in favor of `MultimapBuilder.hashKeys().arrayListValues().build(multimap)`.
         *
         * @param multimap the multimap whose contents are copied to this multimap
         */
        fun <K, V> create(multimap: Multimap<out K, out V>): ArrayListMultimap<K, V> {
            return ArrayListMultimap(multimap)
        }

        @GwtIncompatible // Not needed in emulated source.
        private val serialVersionUID: Long = 0
    }
}
