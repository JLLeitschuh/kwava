/*
 * Copyright (C) 2010 The Guava Authors
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
import java.util.NoSuchElementException


/**
 * This class provides a skeletal implementation of the `Iterator` interface for sequences
 * whose next element can always be derived from the previous element. Null elements are not
 * supported, nor is the [.remove] method.
 *
 *
 * Example:
 *
 * <pre>`Iterator<Integer> powersOfTwo =
 * new AbstractSequentialIterator<Integer>(1) {
 * protected Integer computeNext(Integer previous) {
 * return (previous == 1 << 30) ? null : previous * 2;
 * }
 * };
`</pre> *
 *
 * @author Chris Povirk
 * @since 12.0 (in Guava as `AbstractLinkedIterator` since 8.0)
 */
@GwtCompatible
abstract class AbstractSequentialIterator<T>
/**
 * Creates a new iterator with the given first element, or, if `firstOrNull` is null,
 * creates a new empty iterator.
 */
protected constructor(private var nextOrNull: T?) : UnmodifiableIterator<T>() {

    /**
     * Returns the element that follows `previous`, or returns `null` if no elements
     * remain. This method is invoked during each call to [.next] in order to compute the
     * result of a *future* call to `next()`.
     */
    protected abstract fun computeNext(previous: T): T

    override fun hasNext(): Boolean {
        return nextOrNull != null
    }

    override fun next(): T? {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        try {
            return nextOrNull
        } finally {
            nextOrNull = computeNext(nextOrNull)
        }
    }
}
