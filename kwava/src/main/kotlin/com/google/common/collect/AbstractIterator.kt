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

import com.google.common.base.Preconditions.checkState

import com.google.common.annotations.GwtCompatible

import java.util.NoSuchElementException


/**
 * This class provides a skeletal implementation of the `Iterator` interface, to make this
 * interface easier to implement for certain types of data sources.
 *
 *
 * `Iterator` requires its implementations to support querying the end-of-data status
 * without changing the iterator's state, using the [.hasNext] method. But many data sources,
 * such as [java.io.Reader.read], do not expose this information; the only way to discover
 * whether there is any data left is by trying to retrieve it. These types of data sources are
 * ordinarily difficult to write iterators for. But using this class, one must implement only the
 * [.computeNext] method, and invoke the [.endOfData] method when appropriate.
 *
 *
 * Another example is an iterator that skips over null elements in a backing iterator. This could
 * be implemented as:
 *
 * <pre>`public static Iterator<String> skipNulls(final Iterator<String> in) {
 * return new AbstractIterator<String>() {
 * protected String computeNext() {
 * while (in.hasNext()) {
 * String s = in.next();
 * if (s != null) {
 * return s;
 * }
 * }
 * return endOfData();
 * }
 * };
 * }
`</pre> *
 *
 *
 * This class supports iterators that include null elements.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
// When making changes to this class, please also update the copy at
// com.google.common.base.AbstractIterator
@GwtCompatible
abstract class AbstractIterator<T>
/** Constructor for use by subclasses.  */
protected constructor() : UnmodifiableIterator<T>() {
    private var state = State.NOT_READY

    private var next: T? = null

    private enum class State {
        /** We have computed the next element and haven't returned it yet.  */
        READY,

        /** We haven't yet computed or have already returned the element.  */
        NOT_READY,

        /** We have reached the end of the data and are finished.  */
        DONE,

        /** We've suffered an exception and are kaput.  */
        FAILED
    }

    /**
     * Returns the next element. **Note:** the implementation must call [.endOfData] when
     * there are no elements left in the iteration. Failure to do so could result in an infinite loop.
     *
     *
     * The initial invocation of [.hasNext] or [.next] calls this method, as does
     * the first invocation of `hasNext` or `next` following each successful call to
     * `next`. Once the implementation either invokes `endOfData` or throws an exception,
     * `computeNext` is guaranteed to never be called again.
     *
     *
     * If this method throws an exception, it will propagate outward to the `hasNext` or
     * `next` invocation that invoked this method. Any further attempts to use the iterator will
     * result in an [IllegalStateException].
     *
     *
     * The implementation of this method may not invoke the `hasNext`, `next`, or
     * [.peek] methods on this instance; if it does, an `IllegalStateException` will
     * result.
     *
     * @return the next element if there was one. If `endOfData` was called during execution,
     * the return value will be ignored.
     * @throws RuntimeException if any unrecoverable error happens. This exception will propagate
     * outward to the `hasNext()`, `next()`, or `peek()` invocation that invoked
     * this method. Any further attempts to use the iterator will result in an [     ].
     */
    protected abstract fun computeNext(): T

    /**
     * Implementations of [.computeNext] **must** invoke this method when there are no
     * elements left in the iteration.
     *
     * @return `null`; a convenience so your `computeNext` implementation can use the
     * simple statement `return endOfData();`
     */

    protected fun endOfData(): T? {
        state = State.DONE
        return null
    }

    // TODO(kak): Should we remove this? Some people are using it to prefetch?
    override fun hasNext(): Boolean {
        checkState(state != State.FAILED)
        when (state) {
            AbstractIterator.State.DONE -> return false
            AbstractIterator.State.READY -> return true
        }
        return tryToComputeNext()
    }

    private fun tryToComputeNext(): Boolean {
        state = State.FAILED // temporary pessimism
        next = computeNext()
        if (state != State.DONE) {
            state = State.READY
            return true
        }
        return false
    }

    // TODO(kak): Should we remove this?
    override fun next(): T? {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        state = State.NOT_READY
        val result = next
        next = null
        return result
    }

    /**
     * Returns the next element in the iteration without advancing the iteration, according to the
     * contract of [PeekingIterator.peek].
     *
     *
     * Implementations of `AbstractIterator` that wish to expose this functionality should
     * implement `PeekingIterator`.
     */
    fun peek(): T? {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        return next
    }
}
