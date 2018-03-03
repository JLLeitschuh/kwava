/*
 * Copyright (C) 2009 The Guava Authors
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
 * Implementation of [ImmutableList] backed by a simple array.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(serializable = true, emulated = true)
internal // uses writeReplace(), not default serialization
class RegularImmutableList<E>(
        @field:VisibleForTesting
        @field:Transient val array: Array<Any?>) : ImmutableList<E>() {

    override val isPartialView: Boolean
        get() = false

    override val size: Int
        get() = array.size

    fun copyIntoArray(dst: Array<Any>, dstOff: Int): Int {
        System.arraycopy(array, 0, dst, dstOff, array.size)
        return dstOff + array.size
    }

    // The fake cast to E is safe because the creation methods only allow E's
    override fun get(index: Int): E {
        return array[index] as E
    }

    override fun listIterator(index: Int): UnmodifiableListIterator<E> {
        // for performance
        // The fake cast to E is safe because the creation methods only allow E's
        return Iterators.forArray(array, 0, array.size, index) as UnmodifiableListIterator<E>
    }

    companion object {
        val EMPTY: ImmutableList<Any> = RegularImmutableList(arrayOfNulls(0))
    }

    // TODO(lowasser): benchmark optimizations for equals() and see if they're worthwhile
}
