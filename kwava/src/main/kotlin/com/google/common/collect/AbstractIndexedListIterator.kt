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
import com.google.common.base.Preconditions.checkPositionIndex
import java.util.*

/**
 * This class provides a skeletal implementation of the [ListIterator] interface across a
 * fixed number of elements that may be retrieved by position. It does not support [.remove],
 * [.set], or [.add].
 *
 * @author Jared Levy
 */
@GwtCompatible
internal abstract class AbstractIndexedListIterator<E>
/**
 * Constructs an iterator across a sequence of the given size with the given initial position.
 * That is, the first call to [.nextIndex] will return `position`, and the first
 * call to [.next] will return the element at that index, if available. Calls to [ ][.previous] can retrieve the preceding `position` elements.
 *
 * @throws IndexOutOfBoundsException if `position` is negative or is greater than `size`
 * @throws IllegalArgumentException if `size` is negative
 */
@JvmOverloads
protected constructor(private val size: Int, private var position: Int = 0) : UnmodifiableListIterator<E>() {

    /** Returns the element with the specified index. This method is called by [.next].  */
    protected abstract operator fun get(index: Int): E

    init {
        checkPositionIndex(position, size)
    }

    override fun hasNext(): Boolean {
        return position < size
    }

    override fun next(): E {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        return get(position++)
    }

    override fun nextIndex(): Int {
        return position
    }

    override fun hasPrevious(): Boolean {
        return position > 0
    }

    override fun previous(): E {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }
        return get(--position)
    }

    override fun previousIndex(): Int {
        return position - 1
    }
}
/**
 * Constructs an iterator across a sequence of the given size whose initial position is 0. That
 * is, the first call to [.next] will return the first element (or throw [ ] if `size` is zero).
 *
 * @throws IllegalArgumentException if `size` is negative
 */
