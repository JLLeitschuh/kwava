/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect

import com.google.common.base.Preconditions.checkNotNull

import com.google.common.annotations.GwtCompatible
import com.google.j2objc.annotations.WeakOuter
import java.util.Comparator
import java.util.NavigableSet


/**
 * This class provides a skeletal implementation of the [SortedMultiset] interface.
 *
 *
 * The [.count] and [.size] implementations all iterate across the set returned by
 * [Multiset.entrySet], as do many methods acting on the set returned by [ ][.elementSet]. Override those methods for better performance.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
internal abstract class AbstractSortedMultiset<E>(comparator: Comparator<in E>) : AbstractMultiset<E>(), SortedMultiset<E> {
    @GwtTransient
    val comparator: Comparator<in E>


    @Transient
    private var descendingMultiset: SortedMultiset<E>? = null

    // needed for serialization
    constructor() : this(Ordering.natural<Comparable>() as Comparator<*>) {}

    init {
        this.comparator = checkNotNull(comparator)
    }

    override fun elementSet(): NavigableSet<E> {
        return super.elementSet() as NavigableSet<E>
    }

    internal override fun createElementSet(): NavigableSet<E> {
        return SortedMultisets.NavigableElementSet(this)
    }

    override fun comparator(): Comparator<in E> {
        return comparator
    }

    override fun firstEntry(): Multiset.Entry<E>? {
        val entryIterator = entryIterator()
        return if (entryIterator.hasNext()) entryIterator.next() else null
    }

    override fun lastEntry(): Multiset.Entry<E>? {
        val entryIterator = descendingEntryIterator()
        return if (entryIterator.hasNext()) entryIterator.next() else null
    }

    override fun pollFirstEntry(): Multiset.Entry<E>? {
        val entryIterator = entryIterator()
        if (entryIterator.hasNext()) {
            var result: Multiset.Entry<E> = entryIterator.next()
            result = Multisets.immutableEntry(result.element, result.count)
            entryIterator.remove()
            return result
        }
        return null
    }

    override fun pollLastEntry(): Multiset.Entry<E>? {
        val entryIterator = descendingEntryIterator()
        if (entryIterator.hasNext()) {
            var result: Multiset.Entry<E> = entryIterator.next()
            result = Multisets.immutableEntry(result.element, result.count)
            entryIterator.remove()
            return result
        }
        return null
    }

    override fun subMultiset(
            fromElement: E,
            fromBoundType: BoundType,
            toElement: E,
            toBoundType: BoundType): SortedMultiset<E> {
        // These are checked elsewhere, but NullPointerTester wants them checked eagerly.
        checkNotNull(fromBoundType)
        checkNotNull(toBoundType)
        return tailMultiset(fromElement, fromBoundType).headMultiset(toElement, toBoundType)
    }

    internal abstract fun descendingEntryIterator(): MutableIterator<Multiset.Entry<E>>

    fun descendingIterator(): Iterator<E> {
        return Multisets.iteratorImpl(descendingMultiset())
    }

    override fun descendingMultiset(): SortedMultiset<E> {
        val result = descendingMultiset
        return result ?: (descendingMultiset = createDescendingMultiset())
    }

    fun createDescendingMultiset(): SortedMultiset<E> {
        @WeakOuter
        class DescendingMultisetImpl : DescendingMultiset<E>() {
            internal override fun forwardMultiset(): SortedMultiset<E> {
                return this@AbstractSortedMultiset
            }

            internal override fun entryIterator(): Iterator<Multiset.Entry<E>> {
                return descendingEntryIterator()
            }

            override fun iterator(): Iterator<E> {
                return descendingIterator()
            }
        }
        return DescendingMultisetImpl()
    }
}
