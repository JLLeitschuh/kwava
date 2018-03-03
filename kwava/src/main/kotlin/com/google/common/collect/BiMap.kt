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


/**
 * A bimap (or "bidirectional map") is a map that preserves the uniqueness of its values as well as
 * that of its keys. This constraint enables bimaps to support an "inverse view", which is another
 * bimap containing the same entries as this bimap but with reversed keys and values.
 *
 *
 * See the Guava User Guide article on [ `BiMap`](https://github.com/google/guava/wiki/NewCollectionTypesExplained#bimap).
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible
interface BiMap<K, V> : MutableMap<K, V> {
    // Modification Operations

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the given value is already bound to a different key in this
     * bimap. The bimap will remain unmodified in this event. To avoid this exception, call [     ][.forcePut] instead.
     */

    override fun put(key: K, value: V): V

    /**
     * An alternate form of `put` that silently removes any existing entry with the value `value` before proceeding with the [.put] operation. If the bimap previously contained the
     * provided key-value mapping, this method has no effect.
     *
     *
     * Note that a successful call to this method could cause the size of the bimap to increase by
     * one, stay the same, or even decrease by one.
     *
     *
     * **Warning:** If an existing entry with this value is removed, the key for that entry is
     * discarded and not returned.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the value which was previously associated with the key, which may be `null`, or
     * `null` if there was no previous entry
     */


    fun forcePut(key: K, value: V): V

    // Bulk Operations

    /**
     * {@inheritDoc}
     *
     *
     * **Warning:** the results of calling this method may vary depending on the iteration order
     * of `map`.
     *
     * @throws IllegalArgumentException if an attempt to `put` any entry fails. Note that some
     * map entries may have been added to the bimap before the exception was thrown.
     */
    override fun putAll(map: Map<out K, V>)

    // Views

    /**
     * {@inheritDoc}
     *
     *
     * Because a bimap has unique values, this method returns a [Set], instead of the [Collection] specified in the [Map] interface.
     */
    override val values: MutableSet<V>

    /**
     * Returns the inverse view of this bimap, which maps each of this bimap's values to its
     * associated key. The two bimaps are backed by the same data; any changes to one will appear in
     * the other.
     *
     *
     * **Note:**There is no guaranteed correspondence between the iteration order of a bimap and
     * that of its inverse.
     *
     * @return the inverse view of this bimap
     */
    fun inverse(): BiMap<V, K>
}
