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

import com.google.common.annotations.Beta
import com.google.common.annotations.GwtCompatible
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.collect.CollectPreconditions.checkNonnegative
import com.google.common.math.IntMath
import com.google.common.primitives.Ints
import java.io.Serializable
import java.math.RoundingMode
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collector


/**
 * A [Set] whose contents will never change, with many other important properties detailed at
 * [ImmutableCollection].
 *
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
abstract// we're overriding default serialization
class ImmutableSet<E> internal constructor() : ImmutableCollection<E>(), Set<E> {

    /** Returns `true` if the `hashCode()` method runs quickly.  */
    internal open val isHashCodeFast: Boolean
        get() = false

    @Transient
    private var asList: ImmutableList<E>? = null

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        } else if (other is ImmutableSet<*>
                && isHashCodeFast
                && other.isHashCodeFast
                && hashCode() != other.hashCode()) {
            return false
        }
        return Sets.equalsImpl(this, other)
    }

    override fun hashCode(): Int {
        return Sets.hashCodeImpl(this)
    }

    // This declaration is needed to make Set.iterator() and
    // ImmutableCollection.iterator() consistent.
    abstract override fun iterator(): UnmodifiableIterator<E>

    override fun asList(): ImmutableList<E> {
        val result = asList
        return if (result == null) {
            val theList = createAsList()
            asList = theList
            theList
        } else {
            result
        }
    }

    internal open fun createAsList(): ImmutableList<E> {
        return RegularImmutableAsList(this, toArray())
    }

    internal abstract class Indexed<E> : ImmutableSet<E>() {
        internal abstract operator fun get(index: Int): E

        override fun iterator(): UnmodifiableIterator<E> {
            return asList().iterator()
        }

        override fun forEach(consumer: Consumer<in E>) {
            checkNotNull(consumer)
            val n = size
            for (i in 0 until n) {
                consumer.accept(get(i))
            }
        }


        override fun createAsList(): ImmutableList<E> {
            return object : ImmutableAsList<E>() {
                override fun get(index: Int): E {
                    return this@Indexed[index]
                }

                override fun delegateCollection(): Indexed<E> {
                    return this@Indexed
                }
            }
        }
    }

    /*
   * This class is used to serialize all ImmutableSet instances, except for
   * ImmutableEnumSet/ImmutableSortedSet, regardless of implementation type. It
   * captures their "logical contents" and they are reconstructed using public
   * static factories. This is necessary to ensure that the existence of a
   * particular implementation type is an implementation detail.
   */
    private class SerializedForm
    internal constructor(internal val elements: Array<Any?>) : Serializable {

        internal fun readResolve(): Any {
            return copyOf(elements)
        }

        companion object {

            private const val serialVersionUID: Long = 0
        }
    }

    internal override fun writeReplace(): Any {
        return SerializedForm(toTypedArray())
    }

    /**
     * A builder for creating `ImmutableSet` instances. Example:
     *
     * <pre>`static final ImmutableSet<Color> GOOGLE_COLORS =
     * ImmutableSet.<Color>builder()
     * .addAll(WEBSAFE_COLORS)
     * .add(new Color(0, 191, 255))
     * .build();
    `</pre> *
     *
     *
     * Elements appear in the resulting set in the same order they were first added to the builder.
     *
     *
     * Building does not change the state of the builder, so it is still possible to add more
     * elements and to build again.
     *
     * @since 2.0
     */
    open class Builder<E> : ImmutableCollection.Builder<E> {
        private var impl: SetBuilderImpl<E>? = null
        internal var forceCopy: Boolean = false

        constructor() : this(ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY) {}

        internal constructor(capacity: Int) {
            impl = RegularSetBuilderImpl(capacity)
        }

        internal constructor(subclass: Boolean) {
            this.impl = null // unused
        }

        @VisibleForTesting
        internal fun forceJdk() {
            this.impl = JdkBackedSetBuilderImpl(impl!!)
        }

        internal fun copyIfNecessary() {
            if (forceCopy) {
                copy()
                forceCopy = false
            }
        }

        internal open fun copy() {
            impl = impl!!.copy()
        }

        override fun add(element: E): Builder<E> {
            checkNotNull(element)
            copyIfNecessary()
            impl = impl!!.add(element)
            return this
        }

        override fun add(vararg elements: E): Builder<E> {
            super.add(*elements)
            return this
        }

        override
                /**
                 * Adds each element of `elements` to the `ImmutableSet`, ignoring duplicate
                 * elements (only the first duplicate element is added).
                 *
                 * @param elements the elements to add
                 * @return this `Builder` object
                 * @throws NullPointerException if `elements` is null or contains a null element
                 */
        fun addAll(elements: Iterable<E>): Builder<E> {
            super.addAll(elements)
            return this
        }

        override fun addAll(elements: Iterator<E>): Builder<E> {
            super.addAll(elements)
            return this
        }

        internal open fun combine(other: Builder<E>): Builder<E> {
            copyIfNecessary()
            this.impl = this.impl!!.combine(other.impl!!)
            return this
        }

        override fun build(): ImmutableSet<E> {
            forceCopy = true
            impl = impl!!.review()
            return impl!!.build()
        }
    }

    /** Swappable internal implementation of an ImmutableSet.Builder.  */
    private abstract class SetBuilderImpl<E> {
        internal var dedupedElements: Array<E>
        internal var distinct: Int = 0

        internal constructor(expectedCapacity: Int) {
            this.dedupedElements = arrayOfNulls<Any>(expectedCapacity) as Array<E>
            this.distinct = 0
        }

        /** Initializes this SetBuilderImpl with a copy of the deduped elements array from toCopy.  */
        internal constructor(toCopy: SetBuilderImpl<E>) {
            this.dedupedElements = Arrays.copyOf(toCopy.dedupedElements, toCopy.dedupedElements.size)
            this.distinct = toCopy.distinct
        }

        /**
         * Resizes internal data structures if necessary to store the specified number of distinct
         * elements.
         */
        private fun ensureCapacity(minCapacity: Int) {
            if (minCapacity > dedupedElements.size) {
                val newCapacity = ImmutableCollection.Builder.expandedCapacity(dedupedElements.size, minCapacity)
                dedupedElements = Arrays.copyOf(dedupedElements, newCapacity)
            }
        }

        /** Adds e to the insertion-order array of deduplicated elements. Calls ensureCapacity.  */
        internal fun addDedupedElement(e: E) {
            ensureCapacity(distinct + 1)
            dedupedElements[distinct++] = e
        }

        /**
         * Adds e to this SetBuilderImpl, returning the updated result. Only use the returned
         * SetBuilderImpl, since we may switch implementations if e.g. hash flooding is detected.
         */
        internal abstract fun add(e: E): SetBuilderImpl<E>

        /** Adds all the elements from the specified SetBuilderImpl to this SetBuilderImpl.  */
        internal fun combine(other: SetBuilderImpl<E>): SetBuilderImpl<E> {
            var result = this
            for (i in 0 until other.distinct) {
                result = result.add(other.dedupedElements[i])
            }
            return result
        }

        /**
         * Creates a new copy of this SetBuilderImpl. Modifications to that SetBuilderImpl will not
         * affect this SetBuilderImpl or sets constructed from this SetBuilderImpl via build().
         */
        internal abstract fun copy(): SetBuilderImpl<E>

        /**
         * Call this before build(). Does a final check on the internal data structures, e.g. shrinking
         * unnecessarily large structures or detecting previously unnoticed hash flooding.
         */
        internal open fun review(): SetBuilderImpl<E> {
            return this
        }

        internal abstract fun build(): ImmutableSet<E>
    }

    /**
     * Default implementation of the guts of ImmutableSet.Builder, creating an open-addressed hash
     * table and deduplicating elements as they come, so it only allocates O(max(distinct,
     * expectedCapacity)) rather than O(calls to add).
     *
     *
     * This implementation attempts to detect hash flooding, and if it's identified, falls back to
     * JdkBackedSetBuilderImpl.
     */
    private class RegularSetBuilderImpl<E> : SetBuilderImpl<E> {
        private lateinit var hashTable: Array<Any?>
        private var maxRunBeforeFallback: Int = 0
        private var expandTableThreshold: Int = 0
        private var hashCode: Int = 0

        internal constructor(expectedCapacity: Int) : super(expectedCapacity) {
            val tableSize = chooseTableSize(expectedCapacity)
            this.hashTable = arrayOfNulls(tableSize)
            this.maxRunBeforeFallback = maxRunBeforeFallback(tableSize)
            this.expandTableThreshold = (DESIRED_LOAD_FACTOR * tableSize).toInt()
        }

        internal constructor(toCopy: RegularSetBuilderImpl<E>) : super(toCopy) {
            this.hashTable = Arrays.copyOf(toCopy.hashTable!!, toCopy.hashTable!!.size)
            this.maxRunBeforeFallback = toCopy.maxRunBeforeFallback
            this.expandTableThreshold = toCopy.expandTableThreshold
            this.hashCode = toCopy.hashCode
        }

        internal fun ensureTableCapacity(minCapacity: Int) {
            if (minCapacity > expandTableThreshold && hashTable!!.size < MAX_TABLE_SIZE) {
                val newTableSize = hashTable!!.size * 2
                hashTable = rebuildHashTable(newTableSize, dedupedElements, distinct)
                maxRunBeforeFallback = maxRunBeforeFallback(newTableSize)
                expandTableThreshold = (DESIRED_LOAD_FACTOR * newTableSize).toInt()
            }
        }

        override fun add(e: E): SetBuilderImpl<E> {
            val eHash = e?.hashCode() ?: 0
            val i0 = Hashing.smear(eHash)
            val mask = hashTable!!.size - 1
            var i = i0
            while (i - i0 < maxRunBeforeFallback) {
                val index = i and mask
                val tableEntry = hashTable!![index]
                if (tableEntry == null) {
                    addDedupedElement(e)
                    hashTable[index] = e
                    hashCode += eHash
                    ensureTableCapacity(distinct) // rebuilds table if necessary
                    return this
                } else if (tableEntry == e) { // not a new element, ignore
                    return this
                }
                i++
            }
            // we fell out of the loop due to a long run; fall back to JDK impl
            return JdkBackedSetBuilderImpl(this).add(e)
        }

        override fun copy(): SetBuilderImpl<E> {
            return RegularSetBuilderImpl(this)
        }

        override fun review(): SetBuilderImpl<E> {
            val targetTableSize = chooseTableSize(distinct)
            if (targetTableSize * 2 < hashTable!!.size) {
                hashTable = rebuildHashTable(targetTableSize, dedupedElements, distinct)
            }
            return if (hashFloodingDetected(hashTable!!)) JdkBackedSetBuilderImpl(this) else this
        }

        override fun build(): ImmutableSet<E> {
            when (distinct) {
                0 -> return of()
                1 -> return of(dedupedElements[0])
                else -> {
                    val elements = if (distinct == dedupedElements.size)
                        dedupedElements
                    else Arrays.copyOf(dedupedElements, distinct)
                    return RegularImmutableSet(elements as Array<Any?>, hashCode, hashTable, hashTable!!.size - 1)
                }
            }
        }
    }

    /**
     * SetBuilderImpl version that uses a JDK HashSet, which has built in hash flooding protection.
     */
    private class JdkBackedSetBuilderImpl<E>
    internal constructor(toCopy: SetBuilderImpl<E>) : SetBuilderImpl<E>(toCopy) {
        private val delegate: MutableSet<Any?>

        init {
            delegate = Sets.newHashSetWithExpectedSize(distinct)
            for (i in 0 until distinct) {
                delegate.add(dedupedElements[i])
            }
        }// initializes dedupedElements and distinct

        override fun add(e: E): SetBuilderImpl<E> {
            checkNotNull(e)
            if (delegate.add(e)) {
                addDedupedElement(e)
            }
            return this
        }

        override fun copy(): SetBuilderImpl<E> {
            return JdkBackedSetBuilderImpl(this)
        }

        override fun build(): ImmutableSet<E> {
            when (distinct) {
                0 -> return of()
                1 -> return of(dedupedElements[0])
                else -> return JdkBackedImmutableSet(
                        delegate, ImmutableList.asImmutableList(dedupedElements as Array<Any?>, distinct))
            }
        }
    }

    companion object {

        /**
         * Returns a `Collector` that accumulates the input elements into a new `ImmutableSet`. Elements appear in the resulting set in the encounter order of the stream; if
         * the stream contains duplicates (according to [Object.equals]), only the first
         * duplicate in encounter order will appear in the result.
         *
         * @since 21.0
         */
        @Beta
        fun <E> toImmutableSet(): Collector<E, *, ImmutableSet<E>> {
            return CollectCollectors.toImmutableSet()
        }

        /**
         * Returns the empty immutable set. Preferred over [Collections.emptySet] for code
         * consistency, and because the return type conveys the immutability guarantee.
         */
        // fully variant implementation (never actually produces any Es)
        fun <E> of(): ImmutableSet<E> {
            return RegularImmutableSet.EMPTY as ImmutableSet<E>
        }

        /**
         * Returns an immutable set containing `element`. Preferred over [ ][Collections.singleton] for code consistency, `null` rejection, and because the return
         * type conveys the immutability guarantee.
         */
        fun <E> of(element: E): ImmutableSet<E> {
            return SingletonImmutableSet(element)
        }

        /**
         * Returns an immutable set containing the given elements, minus duplicates, in the order each was
         * first specified. That is, if multiple elements are [equal][Object.equals], all except
         * the first are ignored.
         */
        fun <E> of(e1: E, e2: E): ImmutableSet<E> {
            return construct(2, e1, e2)
        }

        /**
         * Returns an immutable set containing the given elements, minus duplicates, in the order each was
         * first specified. That is, if multiple elements are [equal][Object.equals], all except
         * the first are ignored.
         */
        fun <E> of(e1: E, e2: E, e3: E): ImmutableSet<E> {
            return construct(3, e1, e2, e3)
        }

        /**
         * Returns an immutable set containing the given elements, minus duplicates, in the order each was
         * first specified. That is, if multiple elements are [equal][Object.equals], all except
         * the first are ignored.
         */
        fun <E> of(e1: E, e2: E, e3: E, e4: E): ImmutableSet<E> {
            return construct(4, e1, e2, e3, e4)
        }

        /**
         * Returns an immutable set containing the given elements, minus duplicates, in the order each was
         * first specified. That is, if multiple elements are [equal][Object.equals], all except
         * the first are ignored.
         */
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E): ImmutableSet<E> {
            return construct(5, e1, e2, e3, e4, e5)
        }

        /**
         * Returns an immutable set containing the given elements, minus duplicates, in the order each was
         * first specified. That is, if multiple elements are [equal][Object.equals], all except
         * the first are ignored.
         *
         *
         * The array `others` must not be longer than `Integer.MAX_VALUE - 6`.
         *
         * @since 3.0 (source-compatible since 2.0)
         */
        @SafeVarargs // For Eclipse. For internal javac we have disabled this pointless type of warning.
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, vararg others: E): ImmutableSet<E> {
            checkArgument(
                    others.size <= Integer.MAX_VALUE - 6,
                    "the total number of elements must fit in an int")
            val paramCount = 6
            val elements = arrayOfNulls<Any>(paramCount + others.size)
            elements[0] = e1
            elements[1] = e2
            elements[2] = e3
            elements[3] = e4
            elements[4] = e5
            elements[5] = e6
            System.arraycopy(others, 0, elements, paramCount, others.size)
            return construct(elements.size, *elements)
        }

        /**
         * Constructs an `ImmutableSet` from the first `n` elements of the specified array. If
         * `k` is the size of the returned `ImmutableSet`, then the unique elements of `elements` will be in the first `k` positions, and `elements[i] == null` for `k <= i < n`.
         *
         *
         * This may modify `elements`. Additionally, if `n == elements.length` and `elements` contains no duplicates, `elements` may be used without copying in the returned
         * `ImmutableSet`, in which case it may no longer be modified.
         *
         *
         * `elements` may contain only values of type `E`.
         *
         * @throws NullPointerException if any of the first `n` elements of `elements` is null
         */
        private fun <E> construct(n: Int, vararg elements: Any?): ImmutableSet<E> {
            when (n) {
                0 -> return of()
                1 -> {
                    val elem = elements[0] as E// safe; elements contains only E's
                    return of(elem)
                }
                else -> {
                    var builder: SetBuilderImpl<E> = RegularSetBuilderImpl(ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY)
                    for (i in 0 until n) {
                        val e = checkNotNull(elements[i]) as E
                        builder = builder.add(e)
                    }
                    return builder.review().build()
                }
            }
        }

        /**
         * Returns an immutable set containing each of `elements`, minus duplicates, in the order
         * each appears first in the source collection.
         *
         *
         * **Performance note:** This method will sometimes recognize that the actual copy operation
         * is unnecessary; for example, `copyOf(copyOf(anArrayList))` will copy the data only once.
         * This reduces the expense of habitually making defensive copies at API boundaries. However, the
         * precise conditions for skipping the copy operation are undefined.
         *
         * @throws NullPointerException if any of `elements` is null
         * @since 7.0 (source-compatible since 2.0)
         */
        fun <E> copyOf(elements: Collection<E>): ImmutableSet<E> {
            /*
             * TODO(lowasser): consider checking for ImmutableAsList here
             * TODO(lowasser): consider checking for Multiset here
             */
            // Don't refer to ImmutableSortedSet by name so it won't pull in all that code
            if (elements is ImmutableSet<*> && elements !is SortedSet<*>) {
                val set = elements as ImmutableSet<E>// all supported methods are covariant
                if (!set.isPartialView) {
                    return set
                }
            } else if (elements is EnumSet<*>) {
                return copyOfEnumSet(elements as EnumSet<*>) as ImmutableSet<E>
            }
            val array = elements.toTypedArray<Any?>()
            return construct(array.size, *array)
        }

        /**
         * Returns an immutable set containing each of `elements`, minus duplicates, in the order
         * each appears first in the source iterable. This method iterates over `elements` only
         * once.
         *
         *
         * **Performance note:** This method will sometimes recognize that the actual copy operation
         * is unnecessary; for example, `copyOf(copyOf(anArrayList))` should copy the data only
         * once. This reduces the expense of habitually making defensive copies at API boundaries.
         * However, the precise conditions for skipping the copy operation are undefined.
         *
         * @throws NullPointerException if any of `elements` is null
         */
        fun <E> copyOf(elements: Iterable<E>): ImmutableSet<E> {
            return if (elements is Collection<*>)
                copyOf(elements as Collection<E>)
            else
                copyOf(elements.iterator())
        }

        /**
         * Returns an immutable set containing each of `elements`, minus duplicates, in the order
         * each appears first in the source iterator.
         *
         * @throws NullPointerException if any of `elements` is null
         */
        fun <E> copyOf(elements: Iterator<E>): ImmutableSet<E> {
            // We special-case for 0 or 1 elements, but anything further is madness.
            if (!elements.hasNext()) {
                return of()
            }
            val first = elements.next()
            return if (!elements.hasNext()) {
                of(first)
            } else {
                ImmutableSet.Builder<E>().add(first).addAll(elements).build()
            }
        }

        /**
         * Returns an immutable set containing each of `elements`, minus duplicates, in the order
         * each appears first in the source array.
         *
         * @throws NullPointerException if any of `elements` is null
         * @since 3.0
         */
        fun <E> copyOf(elements: Array<E>): ImmutableSet<E> {
            when (elements.size) {
                0 -> return of()
                1 -> return of(elements[0])
                else -> return construct(elements.size, *elements.clone())
            }
        }

        private// necessary to compile against Java 8
        fun copyOfEnumSet(enumSet: EnumSet<*>): ImmutableSet<*> {
            return ImmutableEnumSet.asImmutable(enumSet.clone())
        }

        /**
         * Returns a new builder. The generated builder is equivalent to the builder created by the [ ] constructor.
         */
        fun <E> builder(): Builder<E> {
            return Builder()
        }

        /**
         * Returns a new builder, expecting the specified number of distinct elements to be added.
         *
         *
         * If `expectedSize` is exactly the number of distinct elements added to the builder
         * before [Builder.build] is called, the builder is likely to perform better than an unsized
         * [.builder] would have.
         *
         *
         * It is not specified if any performance benefits apply if `expectedSize` is close to,
         * but not exactly, the number of distinct elements added to the builder.
         *
         * @since 23.1
         */
        @Beta
        fun <E> builderWithExpectedSize(expectedSize: Int): Builder<E> {
            checkNonnegative(expectedSize, "expectedSize")
            return Builder(expectedSize)
        }

        /** Builds a new open-addressed hash table from the first n objects in elements.  */
        internal fun rebuildHashTable(newTableSize: Int, elements: Array<*>, n: Int): Array<Any?> {
            val hashTable = arrayOfNulls<Any>(newTableSize)
            val mask = hashTable.size - 1
            for (i in 0 until n) {
                val e = elements[i]
                val j0 = Hashing.smear(e?.hashCode() ?: 0)
                var j = j0
                while (true) {
                    val index = j and mask
                    if (hashTable[index] == null) {
                        hashTable[index] = e
                        break
                    }
                    j++
                }
            }
            return hashTable
        }

        // We use power-of-2 tables, and this is the highest int that's a power of 2
        internal val MAX_TABLE_SIZE = Ints.MAX_POWER_OF_TWO

        // Represents how tightly we can pack things, as a maximum.
        private val DESIRED_LOAD_FACTOR = 0.7

        // If the set has this many elements, it will "max out" the table size
        private val CUTOFF = (MAX_TABLE_SIZE * DESIRED_LOAD_FACTOR).toInt()

        /**
         * Returns an array size suitable for the backing array of a hash table that uses open addressing
         * with linear probing in its implementation. The returned size is the smallest power of two that
         * can hold setSize elements with the desired load factor. Always returns at least setSize + 2.
         */
        @VisibleForTesting
        internal fun chooseTableSize(setSize: Int): Int {
            var setSize = setSize
            setSize = Math.max(setSize, 2)
            // Correct the size for open addressing to match desired load factor.
            if (setSize < CUTOFF) {
                // Round up to the next highest power of 2.
                var tableSize = Integer.highestOneBit(setSize - 1) shl 1
                while (tableSize * DESIRED_LOAD_FACTOR < setSize) {
                    tableSize = tableSize shl 1
                }
                return tableSize
            }

            // The table can't be completely full or we'll get infinite reprobes
            checkArgument(setSize < MAX_TABLE_SIZE, "collection too large")
            return MAX_TABLE_SIZE
        }

        /**
         * We attempt to detect deliberate hash flooding attempts, and if one is detected, fall back to a
         * wrapper around j.u.HashSet, which has built in flooding protection. HASH_FLOODING_FPP is the
         * maximum allowed probability of falsely detecting a hash flooding attack if the input is
         * randomly generated.
         *
         *
         * MAX_RUN_MULTIPLIER was determined experimentally to match this FPP.
         */
        internal val HASH_FLOODING_FPP = 0.001

        // NB: yes, this is surprisingly high, but that's what the experiments said was necessary
        internal val MAX_RUN_MULTIPLIER = 12

        /**
         * Checks the whole hash table for poor hash distribution. Takes O(n).
         *
         *
         * The online hash flooding detecting in RegularSetBuilderImpl.add can detect e.g. many exactly
         * matching hash codes, which would cause construction to take O(n^2), but can't detect e.g. hash
         * codes adversarially designed to go into ascending table locations, which keeps construction
         * O(n) (as desired) but then can have O(n) queries later.
         *
         *
         * If this returns false, then no query can take more than O(log n).
         *
         *
         * Note that for a RegularImmutableSet with elements with truly random hash codes, contains
         * operations take expected O(1) time but with high probability take O(log n) for at least some
         * element. (https://en.wikipedia.org/wiki/Linear_probing#Analysis)
         */
        internal fun hashFloodingDetected(hashTable: Array<Any?>): Boolean {
            val maxRunBeforeFallback = maxRunBeforeFallback(hashTable.size)

            // Test for a run wrapping around the end of the table, then check for runs in the middle.
            var endOfStartRun: Int
            endOfStartRun = 0
            while (endOfStartRun < hashTable.size) {
                if (hashTable[endOfStartRun] == null) {
                    break
                }
                endOfStartRun++
                if (endOfStartRun > maxRunBeforeFallback) {
                    return true
                }
            }
            var startOfEndRun: Int
            startOfEndRun = hashTable.size - 1
            while (startOfEndRun > endOfStartRun) {
                if (hashTable[startOfEndRun] == null) {
                    break
                }
                if (endOfStartRun + (hashTable.size - 1 - startOfEndRun) > maxRunBeforeFallback) {
                    return true
                }
                startOfEndRun--
            }
            var i = endOfStartRun + 1
            while (i < startOfEndRun) {
                var runLength = 0
                while (i < startOfEndRun && hashTable[i] != null) {
                    runLength++
                    if (runLength > maxRunBeforeFallback) {
                        return true
                    }
                    i++
                }
                i++
            }
            return false
        }

        /**
         * If more than this many consecutive positions are filled in a table of the specified size,
         * report probable hash flooding.
         */
        internal fun maxRunBeforeFallback(tableSize: Int): Int {
            return MAX_RUN_MULTIPLIER * IntMath.log2(tableSize, RoundingMode.UNNECESSARY)
        }
    }
}
