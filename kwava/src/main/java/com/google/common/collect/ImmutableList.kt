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
import com.google.common.base.Preconditions.checkElementIndex
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.base.Preconditions.checkPositionIndexes
import com.google.common.collect.CollectPreconditions.checkNonnegative
import com.google.common.collect.RegularImmutableList.EMPTY
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collector

/**
 * A {@link List} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained"> immutable collections</a>.
 *
 * @see ImmutableMap
 * @see ImmutableSet
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
abstract// we're overriding default serialization
class ImmutableList<E>
internal constructor() : ImmutableCollection<E>(), List<E>, RandomAccess {
    // This declaration is needed to make List.iterator() and
    // ImmutableCollection.iterator() consistent.
    override fun iterator(): UnmodifiableIterator<E> {
        return listIterator()
    }

    override fun listIterator(): UnmodifiableListIterator<E> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): UnmodifiableListIterator<E> {
        return object : AbstractIndexedListIterator<E>(size, index) {
            override fun get(index: Int): E {
                return this@ImmutableList.get(index)
            }
        }
    }

    override fun forEach(consumer: Consumer<in E>) {
        checkNotNull(consumer)
        val n = size
        for (i in 0 until n) {
            consumer.accept(get(i))
        }
    }

    override fun indexOf(element: E): Int {
        return if ((element == null)) -1 else Lists.indexOfImpl(this, element)
    }

    override fun lastIndexOf(element: E): Int {
        return if ((element == null)) -1 else Lists.lastIndexOfImpl(this, element)
    }

    override fun contains(element: E): Boolean {
        return indexOf(element) >= 0
    }
    // constrain the return type to ImmutableList<E>
    /**
     * Returns an immutable list of the elements between the specified {@code fromIndex}, inclusive,
     * and {@code toIndex}, exclusive. (If {@code fromIndex} and {@code toIndex} are equal, the empty
     * immutable list is returned.)
     */
    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> {
        checkPositionIndexes(fromIndex, toIndex, size)
        val length = toIndex - fromIndex
        if (length == size) {
            return this
        } else if (length == 0) {
            return of()
        } else if (length == 1) {
            return of(get(fromIndex))
        } else {
            return subListUnchecked(fromIndex, toIndex)
        }
    }

    /**
     * Called by the default implementation of {@link #subList} when {@code toIndex - fromIndex > 1},
     * after index validation has already been performed.
     */
    internal fun subListUnchecked(fromIndex: Int, toIndex: Int): ImmutableList<E> {
        return SubList(fromIndex, toIndex - fromIndex)
    }

    internal inner class SubList
    constructor(
            private val offset: Int,
            private val length: Int) : ImmutableList<E>() {

        override val isPartialView: Boolean
            get() {
                return true
            }

        override val size: Int
            get() = length

        override fun get(index: Int): E {
            checkElementIndex(index, length)
            return this@ImmutableList.get(index + offset)
        }

        override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> {
            checkPositionIndexes(fromIndex, toIndex, length)
            return this@ImmutableList.subList(fromIndex + offset, toIndex + offset)
        }
    }

    /**
     * Returns this list instance.
     *
     * @since 2.0
     */
    override fun asList(): ImmutableList<E> {
        return this
    }

    override fun copyIntoArray(dst: Array<Any?>, offset: Int): Int {
        // this loop is faster for RandomAccess instances, which ImmutableLists are
        val size = size
        for (i in 0 until size) {
            dst[offset + i] = get(i)
        }
        return offset + size
    }

    /**
     * Returns a view of this immutable list in reverse order. For example, {@code ImmutableList.of(1,
     * 2, 3).reverse()} is equivalent to {@code ImmutableList.of(3, 2, 1)}.
     *
     * @return a view of this immutable list in reverse order
     * @since 7.0
     */
    open fun reverse(): ImmutableList<E> {
        return if ((size <= 1)) this else ReverseImmutableList(this)
    }

    private class ReverseImmutableList<E> internal constructor(backingList: ImmutableList<E>) : ImmutableList<E>() {
        @Transient
        private val forwardList: ImmutableList<E>
        internal override val isPartialView: Boolean
            get() {
                return forwardList.isPartialView
            }

        init {
            this.forwardList = backingList
        }

        private fun reverseIndex(index: Int): Int {
            return (size - 1) - index
        }

        private fun reversePosition(index: Int): Int {
            return size - index
        }

        override fun reverse(): ImmutableList<E> {
            return forwardList
        }

        override fun contains(element: E): Boolean {
            return forwardList.contains(element)
        }

        override fun indexOf(element: E): Int {
            val index = forwardList.lastIndexOf(element)
            return if ((index >= 0)) reverseIndex(index) else -1
        }

        override fun lastIndexOf(element: E): Int {
            val index = forwardList.indexOf(element)
            return if ((index >= 0)) reverseIndex(index) else -1
        }

        override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> {
            checkPositionIndexes(fromIndex, toIndex, size)
            return forwardList.subList(reversePosition(toIndex), reversePosition(fromIndex)).reverse()
        }

        override fun get(index: Int): E {
            checkElementIndex(index, size)
            return forwardList.get(reverseIndex(index))
        }

        override val size: Int
            get() = forwardList.size
    }

    override fun equals(obj: Any?): Boolean {
        return Lists.equalsImpl(this, obj)
    }

    override fun hashCode(): Int {
        var hashCode = 1
        val n = size
        this.forEach {
            hashCode *= 31
            if (it != null) {
                hashCode += it.hashCode()
            }
            hashCode = hashCode.inv().inv()
        }
        return hashCode
    }

    /**
     * Serializes ImmutableLists as their logical contents. This ensures that
     * implementation types do not leak into the serialized representation.
     */
    internal class SerializedForm(val elements: Array<Any?>) : Serializable {

        fun readResolve(): Any {
            return copyOf(elements)
        }

        companion object {
            private const val serialVersionUID: Long = 0
        }
    }

    @Throws(InvalidObjectException::class)
    private fun readObject(stream: ObjectInputStream) {
        throw InvalidObjectException("Use SerializedForm")
    }

    override fun writeReplace(): Any {
        return SerializedForm(toTypedArray())
    }

    /**
     * A builder for creating immutable list instances, especially {@code public static final} lists
     * ("constant lists"). Example:
     *
     * <pre>{@code
     * public static final ImmutableList<Color> GOOGLE_COLORS
     * = new ImmutableList.Builder<Color>()
     * .addAll(WEBSAFE_COLORS)
     * .add(new Color(0, 191, 255))
     * .build();
     * }</pre>
     *
     * <p>Elements appear in the resulting list in the same order they were added to the builder.
     *
     * <p>Builder instances can be reused; it is safe to call {@link #build} multiple times to build
     * multiple lists in series. Each new list contains all the elements of the ones created before
     * it.
     *
     * @since 2.0
     */
    class Builder<E> internal constructor(capacity: Int) : ImmutableCollection.Builder<E>() {
        @VisibleForTesting
        internal var contents: Array<Any?>
        private var size: Int = 0
        private var forceCopy: Boolean = false

        /**
         * Creates a new builder. The returned builder is equivalent to the builder generated by {@link
         * ImmutableList#builder}.
         */
        constructor() : this(DEFAULT_INITIAL_CAPACITY) {}

        init {
            this.contents = arrayOfNulls(capacity)
        }

        private fun getReadyToExpandTo(minCapacity: Int) {
            if (contents.size < minCapacity) {
                this.contents = Arrays.copyOf(contents, expandedCapacity(contents.size, minCapacity))
                forceCopy = false
            } else if (forceCopy) {
                contents = Arrays.copyOf<Any>(contents, contents.size)
                forceCopy = false
            }
        }

        /**
         * Adds {@code element} to the {@code ImmutableList}.
         *
         * @param element the element to add
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code element} is null
         */
        override fun add(element: E): Builder<E> {
            checkNotNull(element)
            getReadyToExpandTo(size + 1)
            contents[size++] = element
            return this
        }

        /**
         * Adds each element of {@code elements} to the {@code ImmutableList}.
         *
         * @param elements the {@code Iterable} to add to the {@code ImmutableList}
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null or contains a null element
         */
        override fun add(vararg elements: E): Builder<E> {
            add(elements, elements.size)
            return this
        }

        private fun add(elements: Array<*>, n: Int) {
            getReadyToExpandTo(size + n)
            System.arraycopy(elements, 0, contents, size, n)
            size += n
        }

        /**
         * Adds each element of {@code elements} to the {@code ImmutableList}.
         *
         * @param elements the {@code Iterable} to add to the {@code ImmutableList}
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null or contains a null element
         */
        override fun addAll(elements: Iterable<out E>): Builder<E> {
            if (elements is Collection<*>) {
                val collection = elements as Collection<*>
                getReadyToExpandTo(size + collection.size)
                if (collection is ImmutableCollection) {
                    val immutableCollection = collection as ImmutableCollection<*>
                    size = immutableCollection.copyIntoArray(contents, size)
                    return this
                }
            }
            super.addAll(elements)
            return this
        }

        /**
         * Adds each element of {@code elements} to the {@code ImmutableList}.
         *
         * @param elements the {@code Iterable} to add to the {@code ImmutableList}
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null or contains a null element
         */
        override fun addAll(elements: Iterator<out E>): Builder<E> {
            super.addAll(elements)
            return this
        }

        internal fun combine(builder: Builder<E>): Builder<E> {
            checkNotNull(builder)
            add(builder.contents, builder.size)
            return this
        }

        /**
         * Returns a newly-created {@code ImmutableList} based on the contents of the {@code Builder}.
         */
        override fun build(): ImmutableList<E> {
            forceCopy = true
            return asImmutableList(contents, size)
        }
    }

    companion object {
        /**
         * Returns a {@code Collector} that accumulates the input elements into a new {@code
         * ImmutableList}, in encounter order.
         *
         * @since 21.0
         */
        @Beta
        @JvmStatic
        fun <E> toImmutableList(): Collector<E, *, ImmutableList<E>> {
            return CollectCollectors.toImmutableList()
        }

        /**
         * Returns the empty immutable list. This list behaves and performs comparably to {@link
         * Collections#emptyList}, and is preferable mainly for consistency and maintainability of your
         * code.
         */
        // Casting to any type is safe because the list will never hold any elements.
        @JvmStatic
        fun <E> of(): ImmutableList<E> {
            return EMPTY as ImmutableList<E>
        }

        /**
         * Returns an immutable list containing a single element. This list behaves and performs
         * comparably to {@link Collections#singleton}, but will not accept a null element. It is
         * preferable mainly for consistency and maintainability of your code.
         *
         * @throws NullPointerException if {@code element} is null
         */
        @JvmStatic
        fun <E> of(element: E): ImmutableList<E> {
            return SingletonImmutableList<E>(element)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(e1: E, e2: E): ImmutableList<E> {
            return construct(e1, e2)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(e1: E, e2: E, e3: E): ImmutableList<E> {
            return construct(e1, e2, e3)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(e1: E, e2: E, e3: E, e4: E): ImmutableList<E> {
            return construct(e1, e2, e3, e4)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E): ImmutableList<E> {
            return construct(e1, e2, e3, e4, e5)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E, e6: E): ImmutableList<E> {
            return construct(e1, e2, e3, e4, e5, e6)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, e7: E): ImmutableList<E> {
            return construct(e1, e2, e3, e4, e5, e6, e7)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, e7: E, e8: E): ImmutableList<E> {
            return construct(e1, e2, e3, e4, e5, e6, e7, e8)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, e7: E, e8: E, e9: E): ImmutableList<E> {
            return construct(e1, e2, e3, e4, e5, e6, e7, e8, e9)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(
                e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, e7: E, e8: E, e9: E, e10: E): ImmutableList<E> {
            return construct(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any element is null
         */
        @JvmStatic
        fun <E> of(
                e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, e7: E, e8: E, e9: E, e10: E, e11: E): ImmutableList<E> {
            return construct(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11)
        }
        // These go up to eleven. After that, you just get the varargs form, and
        // whatever warnings might come along with it. :(
        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * <p>The array {@code others} must not be longer than {@code Integer.MAX_VALUE - 12}.
         *
         * @throws NullPointerException if any element is null
         * @since 3.0 (source-compatible since 2.0)
         */
        @SafeVarargs // For Eclipse. For internal javac we have disabled this pointless type of warning.
        @JvmStatic
        fun <E> of(
                e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, e7: E, e8: E, e9: E, e10: E, e11: E, e12: E, vararg others: E): ImmutableList<E> {
            checkArgument(
                    others.size <= Integer.MAX_VALUE - 12,
                    "the total number of elements must fit in an int")
            val array = arrayOfNulls<Any>(12 + others.size)
            array[0] = e1
            array[1] = e2
            array[2] = e3
            array[3] = e4
            array[4] = e5
            array[5] = e6
            array[6] = e7
            array[7] = e8
            array[8] = e9
            array[9] = e10
            array[10] = e11
            array[11] = e12
            System.arraycopy(others, 0, array, 12, others.size)
            return construct(*array)
        }

        /**
         * Returns an immutable list containing the given elements, in order. If {@code elements} is a
         * {@link Collection}, this method behaves exactly as {@link #copyOf(Collection)}; otherwise, it
         * behaves exactly as {@code copyOf(elements.iterator()}.
         *
         * @throws NullPointerException if any of {@code elements} is null
         */
        @JvmStatic
        fun <E> copyOf(elements: Iterable<out E>): ImmutableList<E> {
            checkNotNull(elements) // TODO(kevinb): is this here only for GWT?
            return if ((elements is Collection<*>))
                copyOf(elements as Collection<out E>)
            else
                copyOf(elements.iterator())
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
         * safe to do so. The exact circumstances under which a copy will or will not be performed are
         * undocumented and subject to change.
         *
         * <p>Note that if {@code list} is a {@code List<String>}, then {@code ImmutableList.copyOf(list)}
         * returns an {@code ImmutableList<String>} containing each of the strings in {@code list}, while
         * ImmutableList.of(list)} returns an {@code ImmutableList<List<String>>} containing one element
         * (the given list itself).
         *
         * <p>This method is safe to use even when {@code elements} is a synchronized or concurrent
         * collection that is currently being modified by another thread.
         *
         * @throws NullPointerException if any of {@code elements} is null
         */
        @JvmStatic
        fun <E> copyOf(elements: Collection<out E>): ImmutableList<E> {
            if (elements is ImmutableCollection) {
                val list = (elements as ImmutableCollection<E>).asList()// all supported methods are covariant
                @Suppress("RemoveExplicitTypeArguments")
                return if (list.isPartialView) ImmutableList.asImmutableList(list.toTypedArray<Any?>()) else list
            }
            return construct(*elements.toTypedArray<Any?>())
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any of {@code elements} is null
         */
        @JvmStatic
        fun <E> copyOf(elements: Iterator<out E>): ImmutableList<E> {
            // We special-case for 0 or 1 elements, but going further is madness.
            if (!elements.hasNext()) {
                return of()
            }
            val first = elements.next()
            if (!elements.hasNext()) {
                return of(first)
            } else {
                return ImmutableList.Builder<E>().add(first).addAll(elements).build()
            }
        }

        /**
         * Returns an immutable list containing the given elements, in order.
         *
         * @throws NullPointerException if any of {@code elements} is null
         * @since 3.0
         */
        @JvmStatic
        fun <E> copyOf(elements: Array<E>): ImmutableList<E> {
            when (elements.size) {
                0 -> return of()
                1 -> return of(elements[0])
                else -> return construct(*elements.clone())
            }
        }

        /**
         * Returns an immutable list containing the given elements, sorted according to their natural
         * order. The sorting algorithm used is stable, so elements that compare as equal will stay in the
         * order in which they appear in the input.
         *
         * <p>If your data has no duplicates, or you wish to deduplicate elements, use {@code
         * ImmutableSortedSet.copyOf(elements)}; if you want a {@code List} you can use its {@code
         * asList()} view.
         *
         * <p><b>Java 8 users:</b> If you want to convert a {@link java.util.stream.Stream} to a sorted
         * {@code ImmutableList}, use {@code stream.sorted().collect(toImmutableList())}.
         *
         * @throws NullPointerException if any element in the input is null
         * @since 21.0
         */
        @JvmStatic
        fun <E : Comparable<in E>> sortedCopyOf(
                elements: Iterable<out E>): ImmutableList<E> {
            val array = Iterables.toArray(elements, arrayOfNulls<Comparable<*>>(0))
            array.sort()
            return asImmutableList(array as Array<Any?>)
        }

        /**
         * Returns an immutable list containing the given elements, in sorted order relative to the
         * specified comparator. The sorting algorithm used is stable, so elements that compare as equal
         * will stay in the order in which they appear in the input.
         *
         * <p>If your data has no duplicates, or you wish to deduplicate elements, use {@code
         * ImmutableSortedSet.copyOf(comparator, elements)}; if you want a {@code List} you can use its
         * {@code asList()} view.
         *
         * <p><b>Java 8 users:</b> If you want to convert a {@link java.util.stream.Stream} to a sorted
         * {@code ImmutableList}, use {@code stream.sorted(comparator).collect(toImmutableList())}.
         *
         * @throws NullPointerException if any element in the input is null
         * @since 21.0
         */
        fun <E> sortedCopyOf(
                comparator: Comparator<in E>, elements: Iterable<out E>): ImmutableList<E> {
            val array = Iterables.toArray(elements) as Array<E>// all supported methods are covariant
            array.sortWith(comparator)
            return asImmutableList(array as Array<Any?>)
        }

        /** Views the array as an immutable list. Checks for nulls; does not copy. */
        private fun <E> construct(vararg elements: Any?): ImmutableList<E> {
            return asImmutableList(elements.clone() as Array<Any?>)
        }

        /**
         * Views the array as an immutable list. Does not check for nulls; does not copy.
         *
         * <p>The array must be internally created.
         */
        internal fun <E> asImmutableList(elements: Array<Any?>): ImmutableList<E> {
            return asImmutableList(elements, elements.size)
        }

        /**
         * Views the array as an immutable list. Copies if the specified range does not cover the complete
         * array. Does not check for nulls.
         */
        internal fun <E> asImmutableList(elements: Array<Any?>, length: Int): ImmutableList<E> {
            when (length) {
                0 -> return of()
                1 -> return of(elements[0] as E)
                else -> {
                    val elementsReasigned = if (length < elements.size) {
                        Arrays.copyOf<Any>(elements, length)
                    } else {
                        elements
                    }
                    return RegularImmutableList<E>(elementsReasigned)
                }
            }
        }

        /**
         * Returns a new builder. The generated builder is equivalent to the builder created by the {@link
         * Builder} constructor.
         */
        fun <E> builder(): Builder<E> {
            return Builder()
        }

        /**
         * Returns a new builder, expecting the specified number of elements to be added.
         *
         * <p>If {@code expectedSize} is exactly the number of elements added to the builder before {@link
         * Builder#build} is called, the builder is likely to perform better than an unsized {@link
         * #builder()} would have.
         *
         * <p>It is not specified if any performance benefits apply if {@code expectedSize} is close to,
         * but not exactly, the number of elements added to the builder.
         *
         * @since 23.1
         */
        @Beta
        fun <E> builderWithExpectedSize(expectedSize: Int): Builder<E> {
            checkNonnegative(expectedSize, "expectedSize")
            return ImmutableList.Builder(expectedSize)
        }
    }
}
