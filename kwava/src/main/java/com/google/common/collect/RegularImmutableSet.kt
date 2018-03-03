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
import com.google.common.annotations.VisibleForTesting

/**
 * Implementation of {@link ImmutableSet} with two or more elements.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(serializable = true, emulated = true)
internal// uses writeReplace(), not default serialization
class RegularImmutableSet<E>
constructor(
        @Transient private val elements: Array<Any?>,
        @Transient private var hashCode: Int,
        // the same elements in hashed positions (plus nulls)
        @property:VisibleForTesting @Transient val table: Array<Any?>?,
        // 'and' with an int to get a valid table index.
        @Transient private var mask: Int) : ImmutableSet<E>() {

    override val isPartialView: Boolean
        get() = false
    override val isHashCodeFast: Boolean
        get() = true

    override operator fun contains(element: E): Boolean {
        val table = this.table
        if (element == null || table == null) {
            return false
        }
        var i = Hashing.smearedHash(element)
        while (true) {
            i = i and mask
            val candidate = table[i]
            if (candidate == null) {
                return false
            } else if (candidate == element) {
                return true
            }
            i++
        }
    }

    override val size: Int
        get() = elements.size

    override fun iterator(): UnmodifiableIterator<E> {
        return Iterators.forArray(elements) as UnmodifiableIterator<E>
    }

    fun copyIntoArray(dst: Array<Any>, offset: Int): Int {
        System.arraycopy(elements, 0, dst, offset, elements.size)
        return offset + elements.size
    }

    override fun createAsList(): ImmutableList<E> {
        return if ((table == null)) ImmutableList.of() else RegularImmutableAsList<E>(this, elements)
    }

    override fun hashCode(): Int {
        return hashCode
    }

    companion object {
        val EMPTY = RegularImmutableSet<Any>(arrayOfNulls<Any?>(0), 0, null, 0)
    }
}
