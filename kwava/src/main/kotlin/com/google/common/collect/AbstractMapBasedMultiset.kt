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

import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.collect.CollectPreconditions.checkNonnegative
import com.google.common.collect.CollectPreconditions.checkRemove

import com.google.common.annotations.GwtCompatible
import com.google.common.annotations.GwtIncompatible
import com.google.common.primitives.Ints

import java.io.InvalidObjectException
import java.io.ObjectStreamException
import java.io.Serializable
import java.util.ConcurrentModificationException
import java.util.function.ObjIntConsumer


/**
 * Basic implementation of `Multiset<E>` backed by an instance of `Map<E, Count>`.
 *
 *
 * For serialization to work, the subclass must specify explicit `readObject` and `writeObject` methods.
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
internal abstract class AbstractMapBasedMultiset<E>
/** Standard constructor.  */
protected constructor(// TODO(lowasser): consider overhauling this back to Map<E, Integer>
        @field:Transient private var backingMap: MutableMap<E, Count>?) : AbstractMultiset<E>(), Serializable {

    /*
   * Cache the size for efficiency. Using a long lets us avoid the need for
   * overflow checking and ensures that size() will function correctly even if
   * the multiset had once been larger than Integer.MAX_VALUE.
   */
    @Transient
    private var size: Long = 0

    init {
        checkArgument(backingMap.isEmpty())
    }

    /** Used during deserialization only. The backing map must be empty.  */
    fun setBackingMap(backingMap: MutableMap<E, Count>) {
        this.backingMap = backingMap
    }

    // Required Implementations

    /**
     * {@inheritDoc}
     *
     *
     * Invoking [Multiset.Entry.getCount] on an entry in the returned set always returns the
     * current count of that element in the multiset, as opposed to the count at the time the entry
     * was retrieved.
     */
    override fun entrySet(): Set<Multiset.Entry<E>> {
        return super.entrySet()
    }

    internal override fun elementIterator(): Iterator<E> {
        val backingEntries = backingMap!!.entries.iterator()
        return object : Iterator<E> {
            internal var toRemove: Entry<E, Count>? = null

            override fun hasNext(): Boolean {
                return backingEntries.hasNext()
            }

            override fun next(): E {
                val mapEntry = backingEntries.next()
                toRemove = mapEntry
                return mapEntry.key
            }

            override fun remove() {
                checkRemove(toRemove != null)
                size -= toRemove!!.value.getAndSet(0).toLong()
                backingEntries.remove()
                toRemove = null
            }
        }
    }

    internal override fun entryIterator(): Iterator<Multiset.Entry<E>> {
        val backingEntries = backingMap!!.entries.iterator()
        return object : Iterator<Multiset.Entry<E>> {
            internal var toRemove: Entry<E, Count>? = null

            override fun hasNext(): Boolean {
                return backingEntries.hasNext()
            }

            override fun next(): Multiset.Entry<E> {
                val mapEntry = backingEntries.next()
                toRemove = mapEntry
                return object : Multisets.AbstractEntry<E>() {
                    override fun getElement(): E {
                        return mapEntry.key
                    }

                    override fun getCount(): Int {
                        val count = mapEntry.value
                        if (count == null || count.get() == 0) {
                            val frequency = backingMap!![element]
                            if (frequency != null) {
                                return frequency.get()
                            }
                        }
                        return count?.get() ?: 0
                    }
                }
            }

            override fun remove() {
                checkRemove(toRemove != null)
                size -= toRemove!!.value.getAndSet(0).toLong()
                backingEntries.remove()
                toRemove = null
            }
        }
    }

    override fun forEachEntry(action: ObjIntConsumer<in E>) {
        checkNotNull(action)
        backingMap!!.forEach { element, count -> action.accept(element, count.get()) }
    }

    override fun clear() {
        for (frequency in backingMap!!.values) {
            frequency.set(0)
        }
        backingMap!!.clear()
        size = 0L
    }

    internal override fun distinctElements(): Int {
        return backingMap!!.size
    }

    // Optimizations - Query Operations

    override fun size(): Int {
        return Ints.saturatedCast(size)
    }

    override fun iterator(): Iterator<E> {
        return MapBasedMultisetIterator()
    }

    /*
   * Not subclassing AbstractMultiset$MultisetIterator because next() needs to
   * retrieve the Map.Entry<E, Count> entry, which can then be used for
   * a more efficient remove() call.
   */
    private inner class MapBasedMultisetIterator internal constructor() : Iterator<E> {
        internal val entryIterator: MutableIterator<Entry<E, Count>>

        internal var currentEntry: Entry<E, Count>
        internal var occurrencesLeft: Int = 0
        internal var canRemove: Boolean = false

        init {
            this.entryIterator = backingMap!!.entries.iterator()
        }

        override fun hasNext(): Boolean {
            return occurrencesLeft > 0 || entryIterator.hasNext()
        }

        override fun next(): E {
            if (occurrencesLeft == 0) {
                currentEntry = entryIterator.next()
                occurrencesLeft = currentEntry.value.get()
            }
            occurrencesLeft--
            canRemove = true
            return currentEntry.key
        }

        override fun remove() {
            checkRemove(canRemove)
            val frequency = currentEntry.value.get()
            if (frequency <= 0) {
                throw ConcurrentModificationException()
            }
            if (currentEntry.value.addAndGet(-1) == 0) {
                entryIterator.remove()
            }
            size--
            canRemove = false
        }
    }

    override fun count(element: Any?): Int {
        val frequency = Maps.safeGet(backingMap, element)
        return frequency?.get() ?: 0
    }

    // Optional Operations - Modification Operations

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the call would result in more than [     ][Integer.MAX_VALUE] occurrences of `element` in this multiset.
     */

    override fun add(element: E?, occurrences: Int): Int {
        if (occurrences == 0) {
            return count(element)
        }
        checkArgument(occurrences > 0, "occurrences cannot be negative: %s", occurrences)
        val frequency = backingMap!![element]
        val oldCount: Int
        if (frequency == null) {
            oldCount = 0
            backingMap!![element] = Count(occurrences)
        } else {
            oldCount = frequency.get()
            val newCount = oldCount.toLong() + occurrences.toLong()
            checkArgument(newCount <= Integer.MAX_VALUE, "too many occurrences: %s", newCount)
            frequency.add(occurrences)
        }
        size += occurrences.toLong()
        return oldCount
    }


    override fun remove(element: Any?, occurrences: Int): Int {
        if (occurrences == 0) {
            return count(element)
        }
        checkArgument(occurrences > 0, "occurrences cannot be negative: %s", occurrences)
        val frequency = backingMap!![element] ?: return 0

        val oldCount = frequency.get()

        val numberRemoved: Int
        if (oldCount > occurrences) {
            numberRemoved = occurrences
        } else {
            numberRemoved = oldCount
            backingMap!!.remove(element)
        }

        frequency.add(-numberRemoved)
        size -= numberRemoved.toLong()
        return oldCount
    }

    // Roughly a 33% performance improvement over AbstractMultiset.setCount().

    override fun setCount(element: E, count: Int): Int {
        checkNonnegative(count, "count")

        val existingCounter: Count?
        val oldCount: Int
        if (count == 0) {
            existingCounter = backingMap!!.remove(element)
            oldCount = getAndSet(existingCounter, count)
        } else {
            existingCounter = backingMap!![element]
            oldCount = getAndSet(existingCounter, count)

            if (existingCounter == null) {
                backingMap!![element] = Count(count)
            }
        }

        size += (count - oldCount).toLong()
        return oldCount
    }

    // Don't allow default serialization.
    @GwtIncompatible // java.io.ObjectStreamException
    @Throws(ObjectStreamException::class)
    private fun readObjectNoData() {
        throw InvalidObjectException("Stream data required")
    }

    companion object {

        private fun getAndSet(i: Count?, count: Int): Int {
            return i?.getAndSet(count) ?: 0

        }

        @GwtIncompatible // not needed in emulated source.
        private const val serialVersionUID = -2250766705698539974L
    }
}
