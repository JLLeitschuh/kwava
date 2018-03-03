/*
 * Copyright (C) 2008 The Guava Authors
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
import com.google.common.annotations.GwtIncompatible
import com.google.common.base.Preconditions.checkNotNull
import java.io.Serializable
import java.util.function.Consumer


/**
 * `values()` implementation for [ImmutableMap].
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
internal class ImmutableMapValues<K, V>
constructor(private val map: ImmutableMap<K, V>) : ImmutableCollection<V>() {

    override val isPartialView: Boolean
        get() = true

    override val size: Int
        get() = map.size

    override fun iterator(): UnmodifiableIterator<V> {
        return object : UnmodifiableIterator<V>() {
            internal val entryItr = map.entries.iterator()

            override fun hasNext(): Boolean {
                return entryItr.hasNext()
            }

            override fun next(): V {
                return entryItr.next().value
            }
        }
    }

    override operator fun contains(element: V): Boolean {
        return element != null && Iterators.contains(iterator(), element)
    }

    override fun asList(): ImmutableList<V> {
        val entryList = map.entries.toList()
        return object : ImmutableAsList<V>() {
            override fun get(index: Int): V {
                return entryList[index].value
            }

            override fun delegateCollection(): ImmutableCollection<V> {
                return this@ImmutableMapValues
            }
        }
    }

    @GwtIncompatible // serialization
    override fun forEach(action: Consumer<in V>) {
        checkNotNull(action)
        map.forEach { k, v -> action.accept(v) }
    }

    @GwtIncompatible
    override fun writeReplace(): Any {
        return SerializedForm(map)
    }

    @GwtIncompatible // serialization
    private class SerializedForm<V> internal constructor(internal val map: ImmutableMap<*, V>) : Serializable {

        internal fun readResolve(): Any {
            return map.values
        }

        companion object {

            private const val serialVersionUID: Long = 0
        }
    }
}
