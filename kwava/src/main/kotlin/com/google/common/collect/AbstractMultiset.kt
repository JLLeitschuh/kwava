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

import com.google.common.collect.Multisets.setCountImpl

import com.google.common.annotations.GwtCompatible

import com.google.j2objc.annotations.WeakOuter
import java.util.AbstractCollection


/**
 * This class provides a skeletal implementation of the [Multiset] interface. A new multiset
 * implementation can be created easily by extending this class and implementing the [ ][Multiset.entrySet] method, plus optionally overriding [.add] and [ ][.remove] to enable modifications to the multiset.
 *
 *
 * The [.count] and [.size] implementations all iterate across the set returned by
 * [Multiset.entrySet], as do many methods acting on the set returned by [ ][.elementSet]. Override those methods for better performance.
 *
 * @author Kevin Bourrillion
 * @author Louis Wasserman
 */
@GwtCompatible
internal abstract class AbstractMultiset<E> : AbstractCollection<E>(), Multiset<E> {

    // Views


    @Transient
    private var elementSet: Set<E>? = null


    @Transient
    private var entrySet: Set<Multiset.Entry<E>>? = null
    // Query Operations

    override fun isEmpty(): Boolean {
        return entrySet().isEmpty()
    }

    override operator fun contains(element: Any?): Boolean {
        return count(element) > 0
    }

    // Modification Operations

    override fun add(element: E?): Boolean {
        add(element, 1)
        return true
    }


    override fun add(element: E?, occurrences: Int): Int {
        throw UnsupportedOperationException()
    }


    override fun remove(element: Any?): Boolean {
        return remove(element, 1) > 0
    }


    override fun remove(element: Any?, occurrences: Int): Int {
        throw UnsupportedOperationException()
    }


    override fun setCount(element: E, count: Int): Int {
        return setCountImpl(this, element, count)
    }


    override fun setCount(element: E, oldCount: Int, newCount: Int): Boolean {
        return setCountImpl(this, element, oldCount, newCount)
    }

    // Bulk Operations

    /**
     * {@inheritDoc}
     *
     *
     * This implementation is highly efficient when `elementsToAdd` is itself a [ ].
     */

    override fun addAll(elementsToAdd: Collection<E>): Boolean {
        return Multisets.addAllImpl(this, elementsToAdd)
    }


    override fun removeAll(elementsToRemove: Collection<*>): Boolean {
        return Multisets.removeAllImpl(this, elementsToRemove)
    }


    override fun retainAll(elementsToRetain: Collection<*>): Boolean {
        return Multisets.retainAllImpl(this, elementsToRetain)
    }

    abstract override fun clear()

    override fun elementSet(): Set<E> {
        var result = elementSet
        if (result == null) {
            result = createElementSet()
            elementSet = result
        }
        return result
    }

    /**
     * Creates a new instance of this multiset's element set, which will be returned by [ ][.elementSet].
     */
    internal open fun createElementSet(): Set<E> {
        return ElementSet()
    }

    @WeakOuter
    internal inner class ElementSet : Multisets.ElementSet<E>() {
        internal override fun multiset(): Multiset<E> {
            return this@AbstractMultiset
        }

        override fun iterator(): Iterator<E> {
            return elementIterator()
        }
    }

    internal abstract fun elementIterator(): Iterator<E>

    override fun entrySet(): Set<Multiset.Entry<E>> {
        var result: Set<Multiset.Entry<E>>? = entrySet
        if (result == null) {
            result = createEntrySet()
            entrySet = result
        }
        return result
    }

    @WeakOuter
    internal open inner class EntrySet : Multisets.EntrySet<E>() {
        internal override fun multiset(): Multiset<E> {
            return this@AbstractMultiset
        }

        override fun iterator(): Iterator<Multiset.Entry<E>> {
            return entryIterator()
        }

        override fun size(): Int {
            return distinctElements()
        }
    }

    internal open fun createEntrySet(): Set<Multiset.Entry<E>> {
        return EntrySet()
    }

    internal abstract fun entryIterator(): Iterator<Multiset.Entry<E>>

    internal abstract fun distinctElements(): Int

    // Object methods

    /**
     * {@inheritDoc}
     *
     *
     * This implementation returns `true` if `object` is a multiset of the same size
     * and if, for each element, the two multisets have the same count.
     */
    override fun equals(`object`: Any?): Boolean {
        return Multisets.equalsImpl(this, `object`)
    }

    /**
     * {@inheritDoc}
     *
     *
     * This implementation returns the hash code of [Multiset.entrySet].
     */
    override fun hashCode(): Int {
        return entrySet().hashCode()
    }

    /**
     * {@inheritDoc}
     *
     *
     * This implementation returns the result of invoking `toString` on [ ][Multiset.entrySet].
     */
    override fun toString(): String {
        return entrySet().toString()
    }
}
