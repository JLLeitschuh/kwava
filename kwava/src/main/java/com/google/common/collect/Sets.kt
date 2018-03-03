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

import com.google.common.annotations.Beta
import com.google.common.annotations.GwtCompatible
import com.google.common.annotations.GwtIncompatible
import com.google.common.base.Predicate
import com.google.common.base.Predicates
import com.google.common.collect.Collections2.FilteredCollection
import com.google.common.math.IntMath

import java.io.Serializable
import java.util.Arrays
import java.util.BitSet
import java.util.Collections
import java.util.Comparator
import java.util.EnumSet
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.NavigableSet
import java.util.NoSuchElementException
import java.util.SortedSet
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Consumer
import java.util.stream.Collector
import java.util.stream.Stream


/**
 * Static utility methods pertaining to [Set] instances. Also see this class's counterparts
 * [Lists], [Maps] and [Queues].
 *
 *
 * See the Guava User Guide article on [ `Sets`](https://github.com/google/guava/wiki/CollectionUtilitiesExplained#sets).
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Chris Povirk
 * @since 2.0
 */
@GwtCompatible(emulated = true)
object Sets {

    /**
     * [AbstractSet] substitute without the potentially-quadratic `removeAll`
     * implementation.
     */
    internal abstract class ImprovedAbstractSet<E> : AbstractMutableSet<E>() {
        override fun removeAll(c: Collection<E>): Boolean {
            return removeAllImpl(this, c)
        }
    }

    /**
     * Returns an immutable set instance containing the given enum elements. Internally, the returned
     * set will be backed by an [EnumSet].
     *
     *
     * The iteration order of the returned set follows the enum's iteration order, not the order in
     * which the elements are provided to the method.
     *
     * @param anElement one of the elements the set should contain
     * @param otherElements the rest of the elements the set should contain
     * @return an immutable set containing those elements, minus duplicates
     */
    // http://code.google.com/p/google-web-toolkit/issues/detail?id=3028
    @GwtCompatible(serializable = true)
    fun <E : Enum<E>> immutableEnumSet(
            anElement: E, vararg otherElements: E): ImmutableSet<E> {
        return ImmutableEnumSet.asImmutable(EnumSet.of(anElement, *otherElements))
    }

    /**
     * Returns an immutable set instance containing the given enum elements. Internally, the returned
     * set will be backed by an [EnumSet].
     *
     *
     * The iteration order of the returned set follows the enum's iteration order, not the order in
     * which the elements appear in the given collection.
     *
     * @param elements the elements, all of the same `enum` type, that the set should contain
     * @return an immutable set containing those elements, minus duplicates
     */
    // http://code.google.com/p/google-web-toolkit/issues/detail?id=3028
    @GwtCompatible(serializable = true)
    fun <E : Enum<E>> immutableEnumSet(elements: Iterable<E>): ImmutableSet<E> {
        if (elements is ImmutableEnumSet<*>) {
            return elements as ImmutableEnumSet<E>
        } else if (elements is Collection<*>) {
            val collection = elements as Collection<E>
            return if (collection.isEmpty()) {
                ImmutableSet.of()
            } else {
                ImmutableEnumSet.asImmutable(EnumSet.copyOf(collection))
            }
        } else {
            val itr = elements.iterator()
            if (itr.hasNext()) {
                val enumSet = EnumSet.of(itr.next())
                Iterators.addAll(enumSet, itr)
                return ImmutableEnumSet.asImmutable(enumSet)
            } else {
                return ImmutableSet.of()
            }
        }
    }

    private class Accumulator<E : Enum<E>> {


        private var set: EnumSet<E>? = null

        internal fun add(e: E) {
            if (set == null) {
                set = EnumSet.of(e)
            } else {
                set!!.add(e)
            }
        }

        internal fun combine(other: Accumulator<E>): Accumulator<E> {
            if (this.set == null) {
                return other
            } else if (other.set == null) {
                return this
            } else {
                this.set!!.addAll(other.set!!)
                return this
            }
        }

        internal fun toImmutableSet(): ImmutableSet<E> {
            return if (set == null) ImmutableSet.of() else ImmutableEnumSet.asImmutable(set!!)
        }

        companion object {
            internal val TO_IMMUTABLE_ENUM_SET: Collector<Enum<*>, *, ImmutableSet<out Enum<*>>> = Collector.of<Enum<*>, Accumulator<*>, ImmutableSet<*>>(
                    Supplier<Accumulator<*>> { Accumulator() },
                    BiConsumer<Accumulator<*>, Enum<*>> { obj, e -> obj.add(e) },
                    BinaryOperator<Accumulator<*>> { obj, other -> obj.combine(other) },
                    Function<Accumulator<*>, ImmutableSet<*>> { it.toImmutableSet() },
                    Collector.Characteristics.UNORDERED) as Collector<*, *, *>
        }
    }

    /**
     * Returns a `Collector` that accumulates the input elements into a new `ImmutableSet`
     * with an implementation specialized for enums. Unlike [ImmutableSet.toImmutableSet], the
     * resulting set will iterate over elements in their enum definition order, not encounter order.
     *
     * @since 21.0
     */
    @Beta
    fun <E : Enum<E>> toImmutableEnumSet(): Collector<E, *, ImmutableSet<E>> {
        return Accumulator.TO_IMMUTABLE_ENUM_SET
    }

    /**
     * Returns a new, *mutable* `EnumSet` instance containing the given elements in their
     * natural order. This method behaves identically to [EnumSet.copyOf], but also
     * accepts non-`Collection` iterables and empty iterables.
     */
    fun <E : Enum<E>> newEnumSet(
            iterable: Iterable<E>, elementType: Class<E>): EnumSet<E> {
        val set = EnumSet.noneOf(elementType)
        Iterables.addAll(set, iterable)
        return set
    }

    // HashSet

    /**
     * Creates a *mutable*, initially empty `HashSet` instance.
     *
     *
     * **Note:** if mutability is not required, use [ImmutableSet.of] instead. If `E` is an [Enum] type, use [EnumSet.noneOf] instead. Otherwise, strongly consider
     * using a `LinkedHashSet` instead, at the cost of increased memory footprint, to get
     * deterministic iteration behavior.
     *
     *
     * **Note for Java 7 and later:** this method is now unnecessary and should be treated as
     * deprecated. Instead, use the `HashSet` constructor directly, taking advantage of the new
     * ["diamond" syntax](http://goo.gl/iz2Wi).
     */
    fun <E> newHashSet(): HashSet<E> {
        return HashSet()
    }

    /**
     * Creates a *mutable* `HashSet` instance initially containing the given elements.
     *
     *
     * **Note:** if elements are non-null and won't be added or removed after this point, use
     * [ImmutableSet.of] or [ImmutableSet.copyOf] instead. If `E` is an
     * [Enum] type, use [EnumSet.of] instead. Otherwise, strongly consider
     * using a `LinkedHashSet` instead, at the cost of increased memory footprint, to get
     * deterministic iteration behavior.
     *
     *
     * This method is just a small convenience, either for `newHashSet(`[ asList][Arrays.asList]`(...))`, or for creating an empty set then calling [Collections.addAll].
     * This method is not actually very useful and will likely be deprecated in the future.
     */
    fun <E> newHashSet(vararg elements: E): HashSet<E> {
        val set = newHashSetWithExpectedSize<E>(elements.size)
        Collections.addAll(set, *elements)
        return set
    }

    /**
     * Creates a *mutable* `HashSet` instance containing the given elements. A very thin
     * convenience for creating an empty set then calling [Collection.addAll] or [ ][Iterables.addAll].
     *
     *
     * **Note:** if mutability is not required and the elements are non-null, use [ ][ImmutableSet.copyOf] instead. (Or, change `elements` to be a [ ] and call `elements.toSet()`.)
     *
     *
     * **Note:** if `E` is an [Enum] type, use [.newEnumSet]
     * instead.
     *
     *
     * **Note for Java 7 and later:** if `elements` is a [Collection], you don't
     * need this method. Instead, use the `HashSet` constructor directly, taking advantage of
     * the new ["diamond" syntax](http://goo.gl/iz2Wi).
     *
     *
     * Overall, this method is not very useful and will likely be deprecated in the future.
     */
    fun <E> newHashSet(elements: Iterable<E>): HashSet<E> {
        return if (elements is Collection<*>)
            HashSet(Collections2.cast(elements))
        else
            newHashSet(elements.iterator())
    }

    /**
     * Creates a *mutable* `HashSet` instance containing the given elements. A very thin
     * convenience for creating an empty set and then calling [Iterators.addAll].
     *
     *
     * **Note:** if mutability is not required and the elements are non-null, use [ ][ImmutableSet.copyOf] instead.
     *
     *
     * **Note:** if `E` is an [Enum] type, you should create an [EnumSet]
     * instead.
     *
     *
     * Overall, this method is not very useful and will likely be deprecated in the future.
     */
    fun <E> newHashSet(elements: Iterator<E>): HashSet<E> {
        val set = newHashSet<E>()
        Iterators.addAll(set, elements)
        return set
    }

    /**
     * Returns a new hash set using the smallest initial table size that can hold `expectedSize`
     * elements without resizing. Note that this is not what [HashSet.HashSet] does, but it
     * is what most users want and expect it to do.
     *
     *
     * This behavior can't be broadly guaranteed, but has been tested with OpenJDK 1.7 and 1.8.
     *
     * @param expectedSize the number of elements you expect to add to the returned set
     * @return a new, empty hash set with enough capacity to hold `expectedSize` elements
     * without resizing
     * @throws IllegalArgumentException if `expectedSize` is negative
     */
    fun <E> newHashSetWithExpectedSize(expectedSize: Int): HashSet<E> {
        return HashSet(Maps.capacity(expectedSize))
    }

    /**
     * Creates a thread-safe set backed by a hash map. The set is backed by a [ ] instance, and thus carries the same concurrency guarantees.
     *
     *
     * Unlike `HashSet`, this class does NOT allow `null` to be used as an element. The
     * set is serializable.
     *
     * @return a new, empty thread-safe `Set`
     * @since 15.0
     */
    fun <E> newConcurrentHashSet(): Set<E> {
        return Collections.newSetFromMap(ConcurrentHashMap())
    }

    /**
     * Creates a thread-safe set backed by a hash map and containing the given elements. The set is
     * backed by a [ConcurrentHashMap] instance, and thus carries the same concurrency
     * guarantees.
     *
     *
     * Unlike `HashSet`, this class does NOT allow `null` to be used as an element. The
     * set is serializable.
     *
     * @param elements the elements that the set should contain
     * @return a new thread-safe set containing those elements (minus duplicates)
     * @throws NullPointerException if `elements` or any of its contents is null
     * @since 15.0
     */
    fun <E> newConcurrentHashSet(elements: Iterable<E>): Set<E> {
        val set = newConcurrentHashSet<E>()
        Iterables.addAll(set, elements)
        return set
    }

    // LinkedHashSet

    /**
     * Creates a *mutable*, empty `LinkedHashSet` instance.
     *
     *
     * **Note:** if mutability is not required, use [ImmutableSet.of] instead.
     *
     *
     * **Note for Java 7 and later:** this method is now unnecessary and should be treated as
     * deprecated. Instead, use the `LinkedHashSet` constructor directly, taking advantage of
     * the new ["diamond" syntax](http://goo.gl/iz2Wi).
     *
     * @return a new, empty `LinkedHashSet`
     */
    fun <E> newLinkedHashSet(): LinkedHashSet<E> {
        return LinkedHashSet()
    }

    /**
     * Creates a *mutable* `LinkedHashSet` instance containing the given elements in order.
     *
     *
     * **Note:** if mutability is not required and the elements are non-null, use [ ][ImmutableSet.copyOf] instead.
     *
     *
     * **Note for Java 7 and later:** if `elements` is a [Collection], you don't
     * need this method. Instead, use the `LinkedHashSet` constructor directly, taking advantage
     * of the new ["diamond" syntax](http://goo.gl/iz2Wi).
     *
     *
     * Overall, this method is not very useful and will likely be deprecated in the future.
     *
     * @param elements the elements that the set should contain, in order
     * @return a new `LinkedHashSet` containing those elements (minus duplicates)
     */
    fun <E> newLinkedHashSet(elements: Iterable<E>): LinkedHashSet<E> {
        if (elements is Collection<*>) {
            return LinkedHashSet(Collections2.cast(elements))
        }
        val set = newLinkedHashSet<E>()
        Iterables.addAll(set, elements)
        return set
    }

    /**
     * Creates a `LinkedHashSet` instance, with a high enough "initial capacity" that it
     * *should* hold `expectedSize` elements without growth. This behavior cannot be
     * broadly guaranteed, but it is observed to be true for OpenJDK 1.7. It also can't be guaranteed
     * that the method isn't inadvertently *oversizing* the returned set.
     *
     * @param expectedSize the number of elements you expect to add to the returned set
     * @return a new, empty `LinkedHashSet` with enough capacity to hold `expectedSize`
     * elements without resizing
     * @throws IllegalArgumentException if `expectedSize` is negative
     * @since 11.0
     */
    fun <E> newLinkedHashSetWithExpectedSize(expectedSize: Int): LinkedHashSet<E> {
        return LinkedHashSet(Maps.capacity(expectedSize))
    }

    // TreeSet

    /**
     * Creates a *mutable*, empty `TreeSet` instance sorted by the natural sort ordering of
     * its elements.
     *
     *
     * **Note:** if mutability is not required, use [ImmutableSortedSet.of] instead.
     *
     *
     * **Note for Java 7 and later:** this method is now unnecessary and should be treated as
     * deprecated. Instead, use the `TreeSet` constructor directly, taking advantage of the new
     * ["diamond" syntax](http://goo.gl/iz2Wi).
     *
     * @return a new, empty `TreeSet`
     */
    fun <E : Comparable<*>> newTreeSet(): TreeSet<E> {
        return TreeSet()
    }

    /**
     * Creates a *mutable* `TreeSet` instance containing the given elements sorted by their
     * natural ordering.
     *
     *
     * **Note:** if mutability is not required, use [ImmutableSortedSet.copyOf]
     * instead.
     *
     *
     * **Note:** If `elements` is a `SortedSet` with an explicit comparator, this
     * method has different behavior than [TreeSet.TreeSet], which returns a `TreeSet` with that comparator.
     *
     *
     * **Note for Java 7 and later:** this method is now unnecessary and should be treated as
     * deprecated. Instead, use the `TreeSet` constructor directly, taking advantage of the new
     * ["diamond" syntax](http://goo.gl/iz2Wi).
     *
     *
     * This method is just a small convenience for creating an empty set and then calling [ ][Iterables.addAll]. This method is not very useful and will likely be deprecated in the future.
     *
     * @param elements the elements that the set should contain
     * @return a new `TreeSet` containing those elements (minus duplicates)
     */
    fun <E : Comparable<*>> newTreeSet(elements: Iterable<E>): TreeSet<E> {
        val set = newTreeSet<E>()
        Iterables.addAll(set, elements)
        return set
    }

    /**
     * Creates a *mutable*, empty `TreeSet` instance with the given comparator.
     *
     *
     * **Note:** if mutability is not required, use `ImmutableSortedSet.orderedBy(comparator).build()` instead.
     *
     *
     * **Note for Java 7 and later:** this method is now unnecessary and should be treated as
     * deprecated. Instead, use the `TreeSet` constructor directly, taking advantage of the new
     * ["diamond" syntax](http://goo.gl/iz2Wi). One caveat to this is that the `TreeSet` constructor uses a null `Comparator` to mean "natural ordering," whereas this
     * factory rejects null. Clean your code accordingly.
     *
     * @param comparator the comparator to use to sort the set
     * @return a new, empty `TreeSet`
     * @throws NullPointerException if `comparator` is null
     */
    fun <E> newTreeSet(comparator: Comparator<in E>): TreeSet<E> {
        return TreeSet(checkNotNull(comparator))
    }

    /**
     * Creates an empty `Set` that uses identity to determine equality. It compares object
     * references, instead of calling `equals`, to determine whether a provided object matches
     * an element in the set. For example, `contains` returns `false` when passed an
     * object that equals a set member, but isn't the same instance. This behavior is similar to the
     * way `IdentityHashMap` handles key lookups.
     *
     * @since 8.0
     */
    fun <E> newIdentityHashSet(): Set<E> {
        return Collections.newSetFromMap(Maps.newIdentityHashMap())
    }

    /**
     * Creates an empty `CopyOnWriteArraySet` instance.
     *
     *
     * **Note:** if you need an immutable empty [Set], use [Collections.emptySet]
     * instead.
     *
     * @return a new, empty `CopyOnWriteArraySet`
     * @since 12.0
     */
    @GwtIncompatible // CopyOnWriteArraySet
    fun <E> newCopyOnWriteArraySet(): CopyOnWriteArraySet<E> {
        return CopyOnWriteArraySet()
    }

    /**
     * Creates a `CopyOnWriteArraySet` instance containing the given elements.
     *
     * @param elements the elements that the set should contain, in order
     * @return a new `CopyOnWriteArraySet` containing those elements
     * @since 12.0
     */
    @GwtIncompatible // CopyOnWriteArraySet
    fun <E> newCopyOnWriteArraySet(elements: Iterable<E>): CopyOnWriteArraySet<E> {
        // We copy elements to an ArrayList first, rather than incurring the
        // quadratic cost of adding them to the COWAS directly.
        val elementsCollection = if (elements is Collection<*>)
            Collections2.cast(elements)
        else
            Lists.newArrayList(elements)
        return CopyOnWriteArraySet(elementsCollection)
    }

    /**
     * Creates an `EnumSet` consisting of all enum values that are not in the specified
     * collection. If the collection is an [EnumSet], this method has the same behavior as
     * [EnumSet.complementOf]. Otherwise, the specified collection must contain at least one
     * element, in order to determine the element type. If the collection could be empty, use [ ][.complementOf] instead of this method.
     *
     * @param collection the collection whose complement should be stored in the enum set
     * @return a new, modifiable `EnumSet` containing all values of the enum that aren't present
     * in the given collection
     * @throws IllegalArgumentException if `collection` is not an `EnumSet` instance and
     * contains no elements
     */
    fun <E : Enum<E>> complementOf(collection: Collection<E>): EnumSet<E> {
        if (collection is EnumSet<*>) {
            return EnumSet.complementOf(collection as EnumSet<E>)
        }
        checkArgument(
                !collection.isEmpty(), "collection is empty; use the other version of this method")
        val type = collection.iterator().next().getDeclaringClass()
        return makeComplementByHand(collection, type)
    }

    /**
     * Creates an `EnumSet` consisting of all enum values that are not in the specified
     * collection. This is equivalent to [EnumSet.complementOf], but can act on any input
     * collection, as long as the elements are of enum type.
     *
     * @param collection the collection whose complement should be stored in the `EnumSet`
     * @param type the type of the elements in the set
     * @return a new, modifiable `EnumSet` initially containing all the values of the enum not
     * present in the given collection
     */
    fun <E : Enum<E>> complementOf(
            collection: Collection<E>, type: Class<E>): EnumSet<E> {
        checkNotNull(collection)
        return if (collection is EnumSet<*>)
            EnumSet.complementOf(collection as EnumSet<E>)
        else
            makeComplementByHand(collection, type)
    }

    private fun <E : Enum<E>> makeComplementByHand(
            collection: Collection<E>, type: Class<E>): EnumSet<E> {
        val result = EnumSet.allOf(type)
        result.removeAll(collection)
        return result
    }

    /**
     * Returns a set backed by the specified map. The resulting set displays the same ordering,
     * concurrency, and performance characteristics as the backing map. In essence, this factory
     * method provides a [Set] implementation corresponding to any [Map] implementation.
     * There is no need to use this method on a [Map] implementation that already has a
     * corresponding [Set] implementation (such as [java.util.HashMap] or [ ]).
     *
     *
     * Each method invocation on the set returned by this method results in exactly one method
     * invocation on the backing map or its `keySet` view, with one exception. The `addAll` method is implemented as a sequence of `put` invocations on the backing map.
     *
     *
     * The specified map must be empty at the time this method is invoked, and should not be
     * accessed directly after this method returns. These conditions are ensured if the map is created
     * empty, passed directly to this method, and no reference to the map is retained, as illustrated
     * in the following code fragment:
     *
     * <pre>`Set<Object> identityHashSet = Sets.newSetFromMap(
     * new IdentityHashMap<Object, Boolean>());
    `</pre> *
     *
     *
     * The returned set is serializable if the backing map is.
     *
     * @param map the backing map
     * @return the set backed by the map
     * @throws IllegalArgumentException if `map` is not empty
     */
    @Deprecated("Use {@link Collections#newSetFromMap} instead.")
    fun <E> newSetFromMap(map: Map<E, Boolean>): Set<E> {
        return Collections.newSetFromMap(map)
    }

    /**
     * An unmodifiable view of a set which may be backed by other sets; this view will change as the
     * backing sets do. Contains methods to copy the data into a new set which will then remain
     * stable. There is usually no reason to retain a reference of type `SetView`; typically,
     * you either use it as a plain [Set], or immediately invoke [.immutableCopy] or
     * [.copyInto] and forget the `SetView` itself.
     *
     * @since 2.0
     */
    abstract class SetView<E> private constructor() // no subclasses but our own
        : AbstractSet<E>() {

        /**
         * Returns an immutable copy of the current contents of this set view. Does not support null
         * elements.
         *
         *
         * **Warning:** this may have unexpected results if a backing set of this view uses a
         * nonstandard notion of equivalence, for example if it is a [TreeSet] using a comparator
         * that is inconsistent with [Object.equals].
         */
        open fun immutableCopy(): ImmutableSet<E> {
            return ImmutableSet.copyOf(this)
        }

        /**
         * Copies the current contents of this set view into an existing set. This method has equivalent
         * behavior to `set.addAll(this)`, assuming that all the sets involved are based on the
         * same notion of equivalence.
         *
         * @return a reference to `set`, for convenience
         */
        // Note: S should logically extend Set<? super E> but can't due to either
        // some javac bug or some weirdness in the spec, not sure which.

        open fun <S : Set<E>> copyInto(set: S): S {
            set.addAll(this)
            return set
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         */

        @Deprecated("Unsupported operation.")
        override fun add(e: E?): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         */

        @Deprecated("Unsupported operation.")
        override fun remove(`object`: Any?): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         */

        @Deprecated("Unsupported operation.")
        override fun addAll(newElements: Collection<E>): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         */

        @Deprecated("Unsupported operation.")
        override fun removeAll(oldElements: Collection<*>): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         */

        @Deprecated("Unsupported operation.")
        override fun removeIf(filter: java.util.function.Predicate<in E>): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         */

        @Deprecated("Unsupported operation.")
        override fun retainAll(elementsToKeep: Collection<*>): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         */
        @Deprecated("Unsupported operation.")
        override fun clear() {
            throw UnsupportedOperationException()
        }

        /**
         * Scope the return type to [UnmodifiableIterator] to ensure this is an unmodifiable view.
         *
         * @since 20.0 (present with return type [Iterator] since 2.0)
         */
        abstract override fun iterator(): UnmodifiableIterator<E>
    }

    /**
     * Returns an unmodifiable **view** of the union of two sets. The returned set contains all
     * elements that are contained in either backing set. Iterating over the returned set iterates
     * first over all the elements of `set1`, then over each element of `set2`, in order,
     * that is not contained in `set1`.
     *
     *
     * Results are undefined if `set1` and `set2` are sets based on different
     * equivalence relations (as [HashSet], [TreeSet], and the [Map.keySet] of an
     * `IdentityHashMap` all are).
     */
    fun <E> union(set1: Set<E>, set2: Set<E>): SetView<E> {
        checkNotNull(set1, "set1")
        checkNotNull(set2, "set2")

        return object : SetView<E>() {
            override fun size(): Int {
                var size = set1.size
                for (e in set2) {
                    if (!set1.contains(e)) {
                        size++
                    }
                }
                return size
            }

            override fun isEmpty(): Boolean {
                return set1.isEmpty() && set2.isEmpty()
            }

            override fun iterator(): UnmodifiableIterator<E> {
                return object : AbstractIterator<E>() {
                    internal val itr1 = set1.iterator()
                    internal val itr2 = set2.iterator()

                    override fun computeNext(): E? {
                        if (itr1.hasNext()) {
                            return itr1.next()
                        }
                        while (itr2.hasNext()) {
                            val e = itr2.next()
                            if (!set1.contains(e)) {
                                return e
                            }
                        }
                        return endOfData()
                    }
                }
            }

            override fun stream(): Stream<E> {
                return Stream.concat(set1.stream(), set2.stream().filter { e -> !set1.contains(e) })
            }

            override fun parallelStream(): Stream<E> {
                return stream().parallel()
            }

            override operator fun contains(`object`: Any?): Boolean {
                return set1.contains(`object`) || set2.contains(`object`)
            }

            override fun <S : Set<E>> copyInto(set: S): S {
                set.addAll(set1)
                set.addAll(set2)
                return set
            }

            override fun immutableCopy(): ImmutableSet<E> {
                return ImmutableSet.Builder<E>().addAll(set1).addAll(set2).build()
            }
        }
    }

    /**
     * Returns an unmodifiable **view** of the intersection of two sets. The returned set contains
     * all elements that are contained by both backing sets. The iteration order of the returned set
     * matches that of `set1`.
     *
     *
     * Results are undefined if `set1` and `set2` are sets based on different
     * equivalence relations (as `HashSet`, `TreeSet`, and the keySet of an `IdentityHashMap` all are).
     *
     *
     * **Note:** The returned view performs slightly better when `set1` is the smaller of
     * the two sets. If you have reason to believe one of your sets will generally be smaller than the
     * other, pass it first. Unfortunately, since this method sets the generic type of the returned
     * set based on the type of the first set passed, this could in rare cases force you to make a
     * cast, for example:
     *
     * <pre>`Set<Object> aFewBadObjects = ...
     * Set<String> manyBadStrings = ...
     *
     * // impossible for a non-String to be in the intersection
     * SuppressWarnings("unchecked")
     * Set<String> badStrings = (Set) Sets.intersection(
     * aFewBadObjects, manyBadStrings);
    `</pre> *
     *
     *
     * This is unfortunate, but should come up only very rarely.
     */
    fun <E> intersection(set1: Set<E>, set2: Set<*>): SetView<E> {
        checkNotNull(set1, "set1")
        checkNotNull(set2, "set2")

        return object : SetView<E>() {
            override fun iterator(): UnmodifiableIterator<E> {
                return object : AbstractIterator<E>() {
                    internal val itr = set1.iterator()

                    override fun computeNext(): E? {
                        while (itr.hasNext()) {
                            val e = itr.next()
                            if (set2.contains(e)) {
                                return e
                            }
                        }
                        return endOfData()
                    }
                }
            }

            override fun stream(): Stream<E> {
                return set1.stream().filter { set2.contains(it) }
            }

            override fun parallelStream(): Stream<E> {
                return set1.parallelStream().filter { set2.contains(it) }
            }

            override fun size(): Int {
                var size = 0
                for (e in set1) {
                    if (set2.contains(e)) {
                        size++
                    }
                }
                return size
            }

            override fun isEmpty(): Boolean {
                return Collections.disjoint(set1, set2)
            }

            override operator fun contains(`object`: Any?): Boolean {
                return set1.contains(`object`) && set2.contains(`object`)
            }

            override fun containsAll(collection: Collection<*>): Boolean {
                return set1.containsAll(collection) && set2.containsAll(collection)
            }
        }
    }

    /**
     * Returns an unmodifiable **view** of the difference of two sets. The returned set contains
     * all elements that are contained by `set1` and not contained by `set2`. `set2`
     * may also contain elements not present in `set1`; these are simply ignored. The iteration
     * order of the returned set matches that of `set1`.
     *
     *
     * Results are undefined if `set1` and `set2` are sets based on different
     * equivalence relations (as `HashSet`, `TreeSet`, and the keySet of an `IdentityHashMap` all are).
     */
    fun <E> difference(set1: Set<E>, set2: Set<*>): SetView<E> {
        checkNotNull(set1, "set1")
        checkNotNull(set2, "set2")

        return object : SetView<E>() {
            override fun iterator(): UnmodifiableIterator<E> {
                return object : AbstractIterator<E>() {
                    internal val itr = set1.iterator()

                    override fun computeNext(): E? {
                        while (itr.hasNext()) {
                            val e = itr.next()
                            if (!set2.contains(e)) {
                                return e
                            }
                        }
                        return endOfData()
                    }
                }
            }

            override fun stream(): Stream<E> {
                return set1.stream().filter { e -> !set2.contains(e) }
            }

            override fun parallelStream(): Stream<E> {
                return set1.parallelStream().filter { e -> !set2.contains(e) }
            }

            override val size: Int
                get() {
                    var size = 0
                    for (e in set1) {
                        if (!set2.contains(e)) {
                            size++
                        }
                    }
                    return size
                }

            override fun isEmpty(): Boolean {
                return set2.containsAll(set1)
            }

            override operator fun contains(element: E): Boolean {
                return set1.contains(element) && !set2.contains(element)
            }
        }
    }

    /**
     * Returns an unmodifiable **view** of the symmetric difference of two sets. The returned set
     * contains all elements that are contained in either `set1` or `set2` but not in
     * both. The iteration order of the returned set is undefined.
     *
     *
     * Results are undefined if `set1` and `set2` are sets based on different
     * equivalence relations (as `HashSet`, `TreeSet`, and the keySet of an `IdentityHashMap` all are).
     *
     * @since 3.0
     */
    fun <E> symmetricDifference(
            set1: Set<E>, set2: Set<E>): SetView<E> {
        checkNotNull(set1, "set1")
        checkNotNull(set2, "set2")

        return object : SetView<E>() {
            override fun iterator(): UnmodifiableIterator<E> {
                val itr1 = set1.iterator()
                val itr2 = set2.iterator()
                return object : AbstractIterator<E>() {
                    public override fun computeNext(): E? {
                        while (itr1.hasNext()) {
                            val elem1 = itr1.next()
                            if (!set2.contains(elem1)) {
                                return elem1
                            }
                        }
                        while (itr2.hasNext()) {
                            val elem2 = itr2.next()
                            if (!set1.contains(elem2)) {
                                return elem2
                            }
                        }
                        return endOfData()
                    }
                }
            }

            override fun size(): Int {
                var size = 0
                for (e in set1) {
                    if (!set2.contains(e)) {
                        size++
                    }
                }
                for (e in set2) {
                    if (!set1.contains(e)) {
                        size++
                    }
                }
                return size
            }

            override fun isEmpty(): Boolean {
                return set1 == set2
            }

            override operator fun contains(element: Any?): Boolean {
                return set1.contains(element) xor set2.contains(element)
            }
        }
    }

    /**
     * Returns the elements of `unfiltered` that satisfy a predicate. The returned set is a live
     * view of `unfiltered`; changes to one affect the other.
     *
     *
     * The resulting set's iterator does not support `remove()`, but all other set methods
     * are supported. When given an element that doesn't satisfy the predicate, the set's `add()` and `addAll()` methods throw an [IllegalArgumentException]. When methods
     * such as `removeAll()` and `clear()` are called on the filtered set, only elements
     * that satisfy the filter will be removed from the underlying set.
     *
     *
     * The returned set isn't threadsafe or serializable, even if `unfiltered` is.
     *
     *
     * Many of the filtered set's methods, such as `size()`, iterate across every element in
     * the underlying set and determine which elements satisfy the filter. When a live view is
     * *not* needed, it may be faster to copy `Iterables.filter(unfiltered, predicate)` and
     * use the copy.
     *
     *
     * **Warning:** `predicate` must be *consistent with equals*, as documented at
     * [Predicate.apply]. Do not provide a predicate such as `Predicates.instanceOf(ArrayList.class)`, which is inconsistent with equals. (See [ ][Iterables.filter] for related functionality.)
     *
     *
     * **Java 8 users:** many use cases for this method are better addressed by [ ][java.util.stream.Stream.filter]. This method is not being deprecated, but we gently encourage
     * you to migrate to streams.
     */
    // TODO(kevinb): how to omit that last sentence when building GWT javadoc?
    fun <E> filter(unfiltered: Set<E>, predicate: Predicate<in E>): Set<E> {
        if (unfiltered is SortedSet<*>) {
            return filter(unfiltered as SortedSet<E>, predicate)
        }
        if (unfiltered is FilteredSet<*>) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            val filtered = unfiltered as FilteredSet<E>
            val combinedPredicate = Predicates.and(filtered.predicate, predicate)
            return FilteredSet(filtered.unfiltered as Set<E>, combinedPredicate)
        }

        return FilteredSet(checkNotNull(unfiltered), checkNotNull(predicate))
    }

    /**
     * Returns the elements of a `SortedSet`, `unfiltered`, that satisfy a predicate. The
     * returned set is a live view of `unfiltered`; changes to one affect the other.
     *
     *
     * The resulting set's iterator does not support `remove()`, but all other set methods
     * are supported. When given an element that doesn't satisfy the predicate, the set's `add()` and `addAll()` methods throw an [IllegalArgumentException]. When methods
     * such as `removeAll()` and `clear()` are called on the filtered set, only elements
     * that satisfy the filter will be removed from the underlying set.
     *
     *
     * The returned set isn't threadsafe or serializable, even if `unfiltered` is.
     *
     *
     * Many of the filtered set's methods, such as `size()`, iterate across every element in
     * the underlying set and determine which elements satisfy the filter. When a live view is
     * *not* needed, it may be faster to copy `Iterables.filter(unfiltered, predicate)` and
     * use the copy.
     *
     *
     * **Warning:** `predicate` must be *consistent with equals*, as documented at
     * [Predicate.apply]. Do not provide a predicate such as `Predicates.instanceOf(ArrayList.class)`, which is inconsistent with equals. (See [ ][Iterables.filter] for related functionality.)
     *
     * @since 11.0
     */
    fun <E> filter(unfiltered: SortedSet<E>, predicate: Predicate<in E>): SortedSet<E> {
        if (unfiltered is FilteredSet<*>) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            val filtered = unfiltered as FilteredSet<E>
            val combinedPredicate = Predicates.and(filtered.predicate, predicate)
            return FilteredSortedSet(filtered.unfiltered as SortedSet<E>, combinedPredicate)
        }

        return FilteredSortedSet(checkNotNull(unfiltered), checkNotNull(predicate))
    }

    /**
     * Returns the elements of a `NavigableSet`, `unfiltered`, that satisfy a predicate.
     * The returned set is a live view of `unfiltered`; changes to one affect the other.
     *
     *
     * The resulting set's iterator does not support `remove()`, but all other set methods
     * are supported. When given an element that doesn't satisfy the predicate, the set's `add()` and `addAll()` methods throw an [IllegalArgumentException]. When methods
     * such as `removeAll()` and `clear()` are called on the filtered set, only elements
     * that satisfy the filter will be removed from the underlying set.
     *
     *
     * The returned set isn't threadsafe or serializable, even if `unfiltered` is.
     *
     *
     * Many of the filtered set's methods, such as `size()`, iterate across every element in
     * the underlying set and determine which elements satisfy the filter. When a live view is
     * *not* needed, it may be faster to copy `Iterables.filter(unfiltered, predicate)` and
     * use the copy.
     *
     *
     * **Warning:** `predicate` must be *consistent with equals*, as documented at
     * [Predicate.apply]. Do not provide a predicate such as `Predicates.instanceOf(ArrayList.class)`, which is inconsistent with equals. (See [ ][Iterables.filter] for related functionality.)
     *
     * @since 14.0
     */
    @GwtIncompatible // NavigableSet
    fun <E> filter(
            unfiltered: NavigableSet<E>, predicate: Predicate<in E>): NavigableSet<E> {
        if (unfiltered is FilteredSet<*>) {
            // Support clear(), removeAll(), and retainAll() when filtering a filtered
            // collection.
            val filtered = unfiltered as FilteredSet<E>
            val combinedPredicate = Predicates.and(filtered.predicate, predicate)
            return FilteredNavigableSet(filtered.unfiltered as NavigableSet<E>, combinedPredicate)
        }

        return FilteredNavigableSet(checkNotNull(unfiltered), checkNotNull(predicate))
    }

    private open class FilteredSet<E> internal constructor(unfiltered: Set<E>, predicate: Predicate<in E>) : FilteredCollection<E>(unfiltered, predicate), Set<E> {

        override fun equals(`object`: Any?): Boolean {
            return equalsImpl(this, `object`)
        }

        override fun hashCode(): Int {
            return hashCodeImpl(this)
        }
    }

    private open class FilteredSortedSet<E> internal constructor(unfiltered: SortedSet<E>, predicate: Predicate<in E>) : FilteredSet<E>(unfiltered, predicate), SortedSet<E> {

        override fun comparator(): Comparator<in E>? {
            return (unfiltered as SortedSet<E>).comparator()
        }

        override fun subSet(fromElement: E, toElement: E): SortedSet<E> {
            return FilteredSortedSet(
                    (unfiltered as SortedSet<E>).subSet(fromElement, toElement), predicate)
        }

        override fun headSet(toElement: E): SortedSet<E> {
            return FilteredSortedSet((unfiltered as SortedSet<E>).headSet(toElement), predicate)
        }

        override fun tailSet(fromElement: E): SortedSet<E> {
            return FilteredSortedSet((unfiltered as SortedSet<E>).tailSet(fromElement), predicate)
        }

        override fun first(): E {
            return Iterators.find(unfiltered.iterator(), predicate)
        }

        override fun last(): E {
            var sortedUnfiltered = unfiltered as SortedSet<E>
            while (true) {
                val element = sortedUnfiltered.last()
                if (predicate.apply(element)) {
                    return element
                }
                sortedUnfiltered = sortedUnfiltered.headSet(element)
            }
        }
    }

    @GwtIncompatible // NavigableSet
    private class FilteredNavigableSet<E> internal constructor(unfiltered: NavigableSet<E>, predicate: Predicate<in E>) : FilteredSortedSet<E>(unfiltered, predicate), NavigableSet<E> {

        internal fun unfiltered(): NavigableSet<E> {
            return unfiltered as NavigableSet<E>
        }

        override fun lower(e: E): E? {
            return Iterators.find<E>(unfiltered().headSet(e, false).descendingIterator(), predicate, null)
        }

        override fun floor(e: E): E? {
            return Iterators.find<E>(unfiltered().headSet(e, true).descendingIterator(), predicate, null)
        }

        override fun ceiling(e: E): E? {
            return Iterables.find<E>(unfiltered().tailSet(e, true), predicate, null)
        }

        override fun higher(e: E): E? {
            return Iterables.find<E>(unfiltered().tailSet(e, false), predicate, null)
        }

        override fun pollFirst(): E? {
            return Iterables.removeFirstMatching(unfiltered(), predicate)
        }

        override fun pollLast(): E? {
            return Iterables.removeFirstMatching(unfiltered().descendingSet(), predicate)
        }

        override fun descendingSet(): NavigableSet<E> {
            return Sets.filter(unfiltered().descendingSet(), predicate)
        }

        override fun descendingIterator(): Iterator<E> {
            return Iterators.filter(unfiltered().descendingIterator(), predicate)
        }

        override fun last(): E {
            return Iterators.find(unfiltered().descendingIterator(), predicate)
        }

        override fun subSet(
                fromElement: E, fromInclusive: Boolean, toElement: E, toInclusive: Boolean): NavigableSet<E> {
            return filter(
                    unfiltered().subSet(fromElement, fromInclusive, toElement, toInclusive), predicate)
        }

        override fun headSet(toElement: E, inclusive: Boolean): NavigableSet<E> {
            return filter(unfiltered().headSet(toElement, inclusive), predicate)
        }

        override fun tailSet(fromElement: E, inclusive: Boolean): NavigableSet<E> {
            return filter(unfiltered().tailSet(fromElement, inclusive), predicate)
        }
    }

    /**
     * Returns every possible list that can be formed by choosing one element from each of the given
     * sets in order; the "n-ary [Cartesian
 * product](http://en.wikipedia.org/wiki/Cartesian_product)" of the sets. For example:
     *
     * <pre>`Sets.cartesianProduct(ImmutableList.of(
     * ImmutableSet.of(1, 2),
     * ImmutableSet.of("A", "B", "C")))
    `</pre> *
     *
     *
     * returns a set containing six lists:
     *
     *
     *  * `ImmutableList.of(1, "A")`
     *  * `ImmutableList.of(1, "B")`
     *  * `ImmutableList.of(1, "C")`
     *  * `ImmutableList.of(2, "A")`
     *  * `ImmutableList.of(2, "B")`
     *  * `ImmutableList.of(2, "C")`
     *
     *
     *
     * The result is guaranteed to be in the "traditional", lexicographical order for Cartesian
     * products that you would get from nesting for loops:
     *
     * <pre>`for (B b0 : sets.get(0)) {
     * for (B b1 : sets.get(1)) {
     * ...
     * ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
     * // operate on tuple
     * }
     * }
    `</pre> *
     *
     *
     * Note that if any input set is empty, the Cartesian product will also be empty. If no sets at
     * all are provided (an empty list), the resulting Cartesian product has one element, an empty
     * list (counter-intuitive, but mathematically consistent).
     *
     *
     * *Performance notes:* while the cartesian product of sets of size `m, n, p` is a
     * set of size `m x n x p`, its actual memory consumption is much smaller. When the
     * cartesian set is constructed, the input sets are merely copied. Only as the resulting set is
     * iterated are the individual lists created, and these are not retained after iteration.
     *
     * @param sets the sets to choose elements from, in the order that the elements chosen from those
     * sets should appear in the resulting lists
     * @param <B> any common base class shared by all axes (often just [Object])
     * @return the Cartesian product, as an immutable set containing immutable lists
     * @throws NullPointerException if `sets`, any one of the `sets`, or any element of a
     * provided set is null
     * @since 2.0
    </B> */
    fun <B> cartesianProduct(sets: List<Set<B>>): Set<List<B>> {
        return CartesianSet.create(sets)
    }

    /**
     * Returns every possible list that can be formed by choosing one element from each of the given
     * sets in order; the "n-ary [Cartesian
 * product](http://en.wikipedia.org/wiki/Cartesian_product)" of the sets. For example:
     *
     * <pre>`Sets.cartesianProduct(
     * ImmutableSet.of(1, 2),
     * ImmutableSet.of("A", "B", "C"))
    `</pre> *
     *
     *
     * returns a set containing six lists:
     *
     *
     *  * `ImmutableList.of(1, "A")`
     *  * `ImmutableList.of(1, "B")`
     *  * `ImmutableList.of(1, "C")`
     *  * `ImmutableList.of(2, "A")`
     *  * `ImmutableList.of(2, "B")`
     *  * `ImmutableList.of(2, "C")`
     *
     *
     *
     * The result is guaranteed to be in the "traditional", lexicographical order for Cartesian
     * products that you would get from nesting for loops:
     *
     * <pre>`for (B b0 : sets.get(0)) {
     * for (B b1 : sets.get(1)) {
     * ...
     * ImmutableList<B> tuple = ImmutableList.of(b0, b1, ...);
     * // operate on tuple
     * }
     * }
    `</pre> *
     *
     *
     * Note that if any input set is empty, the Cartesian product will also be empty. If no sets at
     * all are provided (an empty list), the resulting Cartesian product has one element, an empty
     * list (counter-intuitive, but mathematically consistent).
     *
     *
     * *Performance notes:* while the cartesian product of sets of size `m, n, p` is a
     * set of size `m x n x p`, its actual memory consumption is much smaller. When the
     * cartesian set is constructed, the input sets are merely copied. Only as the resulting set is
     * iterated are the individual lists created, and these are not retained after iteration.
     *
     * @param sets the sets to choose elements from, in the order that the elements chosen from those
     * sets should appear in the resulting lists
     * @param <B> any common base class shared by all axes (often just [Object])
     * @return the Cartesian product, as an immutable set containing immutable lists
     * @throws NullPointerException if `sets`, any one of the `sets`, or any element of a
     * provided set is null
     * @since 2.0
    </B> */
    fun <B> cartesianProduct(vararg sets: Set<B>): Set<List<B>> {
        return cartesianProduct(Arrays.asList(*sets))
    }

    private class CartesianSet<E> private constructor(@field:Transient private val axes: ImmutableList<ImmutableSet<E>>, @field:Transient private val delegate: CartesianList<E>) : ForwardingCollection<List<E>>(), Set<List<E>> {

        override fun delegate(): Collection<List<E>> {
            return delegate
        }

        override fun equals(`object`: Any?): Boolean {
            // Warning: this is broken if size() == 0, so it is critical that we
            // substitute an empty ImmutableSet to the user in place of this
            if (`object` is CartesianSet<*>) {
                val that = `object` as CartesianSet<*>?
                return this.axes == that!!.axes
            }
            return super.equals(`object`)
        }

        override fun hashCode(): Int {
            // Warning: this is broken if size() == 0, so it is critical that we
            // substitute an empty ImmutableSet to the user in place of this

            // It's a weird formula, but tests prove it works.
            var adjust = size - 1
            for (i in axes.indices) {
                adjust *= 31
                adjust = adjust.inv().inv()
                // in GWT, we have to deal with integer overflow carefully
            }
            var hash = 1
            for (axis in axes) {
                hash = 31 * hash + size / axis.size * axis.hashCode()

                hash = hash.inv().inv()
            }
            hash += adjust
            return hash.inv().inv()
        }

        companion object {

            internal fun <E> create(sets: List<Set<E>>): Set<List<E>> {
                val axesBuilder = ImmutableList.Builder<ImmutableSet<E>>(sets.size)
                for (set in sets) {
                    val copy = ImmutableSet.copyOf(set)
                    if (copy.isEmpty()) {
                        return ImmutableSet.of()
                    }
                    axesBuilder.add(copy)
                }
                val axes = axesBuilder.build()
                val listAxes = object : ImmutableList<List<E>>() {
                    override fun size(): Int {
                        return axes.size
                    }

                    override fun get(index: Int): List<E> {
                        return axes[index].asList()
                    }

                    internal override fun isPartialView(): Boolean {
                        return true
                    }
                }
                return CartesianSet(axes, CartesianList(listAxes))
            }
        }
    }

    /**
     * Returns the set of all possible subsets of `set`. For example, `powerSet(ImmutableSet.of(1, 2))` returns the set `{{}, {1}, {2}, {1, 2}}`.
     *
     *
     * Elements appear in these subsets in the same iteration order as they appeared in the input
     * set. The order in which these subsets appear in the outer set is undefined. Note that the power
     * set of the empty set is not the empty set, but a one-element set containing the empty set.
     *
     *
     * The returned set and its constituent sets use `equals` to decide whether two elements
     * are identical, even if the input set uses a different concept of equivalence.
     *
     *
     * *Performance notes:* while the power set of a set with size `n` is of size `2^n`, its memory usage is only `O(n)`. When the power set is constructed, the input set
     * is merely copied. Only as the power set is iterated are the individual subsets created, and
     * these subsets themselves occupy only a small constant amount of memory.
     *
     * @param set the set of elements to construct a power set from
     * @return the power set, as an immutable set of immutable sets
     * @throws IllegalArgumentException if `set` has more than 30 unique elements (causing the
     * power set size to exceed the `int` range)
     * @throws NullPointerException if `set` is or contains `null`
     * @see [Power set article at Wikipedia](http://en.wikipedia.org/wiki/Power_set)
     *
     * @since 4.0
     */
    @GwtCompatible(serializable = false)
    fun <E> powerSet(set: Set<E>): Set<Set<E>> {
        return PowerSet(set)
    }

    private class SubSet<E> internal constructor(private val inputSet: ImmutableMap<E, Int>, private val mask: Int) : AbstractSet<E>() {

        override fun iterator(): Iterator<E> {
            return object : UnmodifiableIterator<E>() {
                internal val elements = inputSet.keys.asList()
                internal var remainingSetBits = mask

                override fun hasNext(): Boolean {
                    return remainingSetBits != 0
                }

                override fun next(): E {
                    val index = Integer.numberOfTrailingZeros(remainingSetBits)
                    if (index == 32) {
                        throw NoSuchElementException()
                    }
                    remainingSetBits = remainingSetBits and (1 shl index).inv()
                    return elements[index]
                }
            }
        }

        override fun size(): Int {
            return Integer.bitCount(mask)
        }

        override operator fun contains(o: Any?): Boolean {
            val index = inputSet[o]
            return index != null && mask and (1 shl index) != 0
        }
    }

    private class PowerSet<E> internal constructor(input: Set<E>) : AbstractSet<Set<E>>() {
        internal val inputSet: ImmutableMap<E, Int>

        init {
            this.inputSet = Maps.indexMap(input)
            checkArgument(
                    inputSet.size <= 30, "Too many elements to create power set: %s > 30", inputSet.size)
        }

        override fun size(): Int {
            return 1 shl inputSet.size
        }

        override fun isEmpty(): Boolean {
            return false
        }

        override fun iterator(): Iterator<Set<E>> {
            return object : AbstractIndexedListIterator<Set<E>>(size) {
                override fun get(setBits: Int): Set<E> {
                    return SubSet(inputSet, setBits)
                }
            }
        }

        override operator fun contains(obj: Any?): Boolean {
            if (obj is Set<*>) {
                val set = obj as Set<*>?
                return inputSet.keys.containsAll(set!!)
            }
            return false
        }

        override fun equals(obj: Any?): Boolean {
            if (obj is PowerSet<*>) {
                val that = obj as PowerSet<*>?
                return inputSet == that!!.inputSet
            }
            return super.equals(obj)
        }

        override fun hashCode(): Int {
            /*
       * The sum of the sums of the hash codes in each subset is just the sum of
       * each input element's hash code times the number of sets that element
       * appears in. Each element appears in exactly half of the 2^n sets, so:
       */
            return inputSet.keys.hashCode() shl inputSet.size - 1
        }

        override fun toString(): String {
            return "powerSet($inputSet)"
        }
    }

    /**
     * Returns the set of all subsets of `set` of size `size`. For example, `combinations(ImmutableSet.of(1, 2, 3), 2)` returns the set `{{1, 2}, {1, 3}, {2, 3}}`.
     *
     *
     * Elements appear in these subsets in the same iteration order as they appeared in the input
     * set. The order in which these subsets appear in the outer set is undefined.
     *
     *
     * The returned set and its constituent sets use `equals` to decide whether two elements
     * are identical, even if the input set uses a different concept of equivalence.
     *
     *
     * *Performance notes:* the memory usage of the returned set is only `O(n)`. When
     * the result set is constructed, the input set is merely copied. Only as the result set is
     * iterated are the individual subsets created. Each of these subsets occupies an additional O(n)
     * memory but only for as long as the user retains a reference to it. That is, the set returned by
     * `combinations` does not retain the individual subsets.
     *
     * @param set the set of elements to take combinations of
     * @param size the number of elements per combination
     * @return the set of all combinations of `size` elements from `set`
     * @throws IllegalArgumentException if `size` is not between 0 and `set.size()`
     * inclusive
     * @throws NullPointerException if `set` is or contains `null`
     * @since 23.0
     */
    @Beta
    fun <E> combinations(set: Set<E>, size: Int): Set<Set<E>> {
        val index = Maps.indexMap(set)
        checkNonnegative(size, "size")
        checkArgument(size <= index.size, "size (%s) must be <= set.size() (%s)", size, index.size)
        if (size == 0) {
            return ImmutableSet.of<Set<E>>(ImmutableSet.of())
        } else if (size == index.size) {
            return ImmutableSet.of<Set<E>>(index.keys)
        }
        return object : AbstractSet<Set<E>>() {
            override operator fun contains(o: Any?): Boolean {
                if (o is Set<*>) {
                    val s = o as Set<*>?
                    return s!!.size == size && index.keys.containsAll(s)
                }
                return false
            }

            override fun iterator(): Iterator<Set<E>> {
                return object : AbstractIterator<Set<E>>() {
                    internal val bits = BitSet(index.size)

                    override fun computeNext(): Set<E>? {
                        if (bits.isEmpty) {
                            bits.set(0, size)
                        } else {
                            val firstSetBit = bits.nextSetBit(0)
                            val bitToFlip = bits.nextClearBit(firstSetBit)

                            if (bitToFlip == index.size) {
                                return endOfData()
                            }

                            /*
               * The current set in sorted order looks like
               * {firstSetBit, firstSetBit + 1, ..., bitToFlip - 1, ...}
               * where it does *not* contain bitToFlip.
               *
               * The next combination is
               *
               * {0, 1, ..., bitToFlip - firstSetBit - 2, bitToFlip, ...}
               *
               * This is lexicographically next if you look at the combinations in descending order
               * e.g. {2, 1, 0}, {3, 1, 0}, {3, 2, 0}, {3, 2, 1}, {4, 1, 0}...
               */

                            bits.set(0, bitToFlip - firstSetBit - 1)
                            bits.clear(bitToFlip - firstSetBit - 1, bitToFlip)
                            bits.set(bitToFlip)
                        }
                        val copy = bits.clone() as BitSet
                        return object : AbstractSet<E>() {
                            override operator fun contains(o: Any?): Boolean {
                                val i = index[o]
                                return i != null && copy.get(i)
                            }

                            override fun iterator(): Iterator<E> {
                                return object : AbstractIterator<E>() {
                                    internal var i = -1

                                    override fun computeNext(): E? {
                                        i = copy.nextSetBit(i + 1)
                                        return if (i == -1) {
                                            endOfData()
                                        } else index.keys.asList()[i]
                                    }
                                }
                            }

                            override fun size(): Int {
                                return size
                            }
                        }
                    }
                }
            }

            override fun size(): Int {
                return IntMath.binomial(index.size, size)
            }

            override fun toString(): String {
                return "Sets.combinations(" + index.keys + ", " + size + ")"
            }
        }
    }

    /** An implementation for [Set.hashCode].  */
    internal fun hashCodeImpl(s: Set<*>): Int {
        var hashCode = 0
        for (o in s) {
            hashCode += o?.hashCode() ?: 0

            hashCode = hashCode.inv().inv()
            // Needed to deal with unusual integer overflow in GWT.
        }
        return hashCode
    }

    /** An implementation for [Set.equals].  */
    internal fun equalsImpl(s: Set<*>, `object`: Any?): Boolean {
        if (s === `object`) {
            return true
        }
        if (`object` is Set<*>) {
            val o = `object` as Set<*>?

            try {
                return s.size == o!!.size && s.containsAll(o)
            } catch (ignored: NullPointerException) {
                return false
            } catch (ignored: ClassCastException) {
                return false
            }

        }
        return false
    }

    /**
     * Returns an unmodifiable view of the specified navigable set. This method allows modules to
     * provide users with "read-only" access to internal navigable sets. Query operations on the
     * returned set "read through" to the specified set, and attempts to modify the returned set,
     * whether direct or via its collection views, result in an `UnsupportedOperationException`.
     *
     *
     * The returned navigable set will be serializable if the specified navigable set is
     * serializable.
     *
     * @param set the navigable set for which an unmodifiable view is to be returned
     * @return an unmodifiable view of the specified navigable set
     * @since 12.0
     */
    fun <E> unmodifiableNavigableSet(set: NavigableSet<E>): NavigableSet<E> {
        return if (set is ImmutableCollection<*> || set is UnmodifiableNavigableSet<*>) {
            set
        } else UnmodifiableNavigableSet(set)
    }

    internal class UnmodifiableNavigableSet<E>(delegate: NavigableSet<E>) : ForwardingSortedSet<E>(), NavigableSet<E>, Serializable {
        private val delegate: NavigableSet<E>
        private val unmodifiableDelegate: SortedSet<E>


        @Transient
        private var descendingSet: UnmodifiableNavigableSet<E>? = null

        init {
            this.delegate = checkNotNull(delegate)
            this.unmodifiableDelegate = Collections.unmodifiableSortedSet(delegate)
        }

        override fun delegate(): SortedSet<E> {
            return unmodifiableDelegate
        }

        // default methods not forwarded by ForwardingSortedSet

        override fun removeIf(filter: java.util.function.Predicate<in E>): Boolean {
            throw UnsupportedOperationException()
        }

        override fun stream(): Stream<E> {
            return delegate.stream()
        }

        override fun parallelStream(): Stream<E> {
            return delegate.parallelStream()
        }

        override fun forEach(action: Consumer<in E>) {
            delegate.forEach(action)
        }

        override fun lower(e: E): E? {
            return delegate.lower(e)
        }

        override fun floor(e: E): E? {
            return delegate.floor(e)
        }

        override fun ceiling(e: E): E? {
            return delegate.ceiling(e)
        }

        override fun higher(e: E): E? {
            return delegate.higher(e)
        }

        override fun pollFirst(): E? {
            throw UnsupportedOperationException()
        }

        override fun pollLast(): E? {
            throw UnsupportedOperationException()
        }

        override fun descendingSet(): NavigableSet<E> {
            var result = descendingSet
            if (result == null) {
                descendingSet = UnmodifiableNavigableSet(delegate.descendingSet())
                result = descendingSet
                result!!.descendingSet = this
            }
            return result
        }

        override fun descendingIterator(): Iterator<E> {
            return Iterators.unmodifiableIterator(delegate.descendingIterator())
        }

        override fun subSet(
                fromElement: E, fromInclusive: Boolean, toElement: E, toInclusive: Boolean): NavigableSet<E> {
            return unmodifiableNavigableSet(
                    delegate.subSet(fromElement, fromInclusive, toElement, toInclusive))
        }

        override fun headSet(toElement: E, inclusive: Boolean): NavigableSet<E> {
            return unmodifiableNavigableSet(delegate.headSet(toElement, inclusive))
        }

        override fun tailSet(fromElement: E, inclusive: Boolean): NavigableSet<E> {
            return unmodifiableNavigableSet(delegate.tailSet(fromElement, inclusive))
        }

        companion object {

            private const val serialVersionUID: Long = 0
        }
    }

    /**
     * Returns a synchronized (thread-safe) navigable set backed by the specified navigable set. In
     * order to guarantee serial access, it is critical that **all** access to the backing
     * navigable set is accomplished through the returned navigable set (or its views).
     *
     *
     * It is imperative that the user manually synchronize on the returned sorted set when
     * iterating over it or any of its `descendingSet`, `subSet`, `headSet`, or
     * `tailSet` views.
     *
     * <pre>`NavigableSet<E> set = synchronizedNavigableSet(new TreeSet<E>());
     * ...
     * synchronized (set) {
     * // Must be in the synchronized block
     * Iterator<E> it = set.iterator();
     * while (it.hasNext()) {
     * foo(it.next());
     * }
     * }
    `</pre> *
     *
     *
     * or:
     *
     * <pre>`NavigableSet<E> set = synchronizedNavigableSet(new TreeSet<E>());
     * NavigableSet<E> set2 = set.descendingSet().headSet(foo);
     * ...
     * synchronized (set) { // Note: set, not set2!!!
     * // Must be in the synchronized block
     * Iterator<E> it = set2.descendingIterator();
     * while (it.hasNext())
     * foo(it.next());
     * }
    ` *
     * }</pre>
     *
     *
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     *
     * The returned navigable set will be serializable if the specified navigable set is
     * serializable.
     *
     * @param navigableSet the navigable set to be "wrapped" in a synchronized navigable set.
     * @return a synchronized view of the specified navigable set.
     * @since 13.0
     */
    @GwtIncompatible // NavigableSet
    fun <E> synchronizedNavigableSet(navigableSet: NavigableSet<E>): NavigableSet<E> {
        return Synchronized.navigableSet(navigableSet)
    }

    /** Remove each element in an iterable from a set.  */
    internal fun removeAllImpl(set: MutableSet<*>, iterator: Iterator<*>): Boolean {
        var changed = false
        while (iterator.hasNext()) {
            changed = changed or set.remove(iterator.next())
        }
        return changed
    }

    internal fun removeAllImpl(set: MutableSet<*>, collection: Collection<*>): Boolean {
        var collection = collection
        checkNotNull(collection) // for GWT
        if (collection is Multiset<*>) {
            collection = collection.elementSet()
        }
        /*
     * AbstractSet.removeAll(List) has quadratic behavior if the list size
     * is just more than the set's size.  We augment the test by
     * assuming that sets have fast contains() performance, and other
     * collections don't.  See
     * http://code.google.com/p/guava-libraries/issues/detail?id=1013
     */
        return if (collection is Set<*> && collection.size > set.size) {
            Iterators.removeAll(set.iterator(), collection)
        } else {
            removeAllImpl(set, collection.iterator())
        }
    }

    @GwtIncompatible // NavigableSet
    internal open class DescendingSet<E>(private val forward: NavigableSet<E>) : ForwardingNavigableSet<E>() {

        override fun delegate(): NavigableSet<E> {
            return forward
        }

        override fun lower(e: E): E? {
            return forward.higher(e)
        }

        override fun floor(e: E): E? {
            return forward.ceiling(e)
        }

        override fun ceiling(e: E): E? {
            return forward.floor(e)
        }

        override fun higher(e: E): E? {
            return forward.lower(e)
        }

        override fun pollFirst(): E? {
            return forward.pollLast()
        }

        override fun pollLast(): E? {
            return forward.pollFirst()
        }

        override fun descendingSet(): NavigableSet<E> {
            return forward
        }

        override fun descendingIterator(): Iterator<E> {
            return forward.iterator()
        }

        override fun subSet(
                fromElement: E, fromInclusive: Boolean, toElement: E, toInclusive: Boolean): NavigableSet<E> {
            return forward.subSet(toElement, toInclusive, fromElement, fromInclusive).descendingSet()
        }

        override fun subSet(fromElement: E, toElement: E): SortedSet<E> {
            return standardSubSet(fromElement, toElement)
        }

        override fun headSet(toElement: E, inclusive: Boolean): NavigableSet<E> {
            return forward.tailSet(toElement, inclusive).descendingSet()
        }

        override fun headSet(toElement: E): SortedSet<E> {
            return standardHeadSet(toElement)
        }

        override fun tailSet(fromElement: E, inclusive: Boolean): NavigableSet<E> {
            return forward.headSet(fromElement, inclusive).descendingSet()
        }

        override fun tailSet(fromElement: E): SortedSet<E> {
            return standardTailSet(fromElement)
        }

        override fun comparator(): Comparator<in E>? {
            val forwardComparator = forward.comparator()
            return if (forwardComparator == null) {
                Ordering.natural<Comparable>().reverse<Comparable>() as Comparator<*>
            } else {
                reverse<in E>(forwardComparator)
            }
        }

        // If we inline this, we get a javac error.
        private fun <T> reverse(forward: Comparator<T>): Ordering<T> {
            return Ordering.from(forward).reverse()
        }

        override fun first(): E {
            return forward.last()
        }

        override fun last(): E {
            return forward.first()
        }

        override fun iterator(): Iterator<E> {
            return forward.descendingIterator()
        }

        override fun toArray(): Array<Any> {
            return standardToArray()
        }

        override fun <T> toArray(array: Array<T>): Array<T> {
            return standardToArray(array)
        }

        override fun toString(): String {
            return standardToString()
        }
    }

    /**
     * Returns a view of the portion of `set` whose elements are contained by `range`.
     *
     *
     * This method delegates to the appropriate methods of [NavigableSet] (namely [ ][NavigableSet.subSet], [ ][NavigableSet.tailSet], and [headSet()][NavigableSet.headSet]) to actually construct the view. Consult these methods for a full
     * description of the returned view's behavior.
     *
     *
     * **Warning:** `Range`s always represent a range of values using the values' natural
     * ordering. `NavigableSet` on the other hand can specify a custom ordering via a [ ], which can violate the natural ordering. Using this method (or in general using
     * `Range`) with unnaturally-ordered sets can lead to unexpected and undefined behavior.
     *
     * @since 20.0
     */
    @Beta
    @GwtIncompatible // NavigableSet
    fun <K : Comparable<K>> subSet(
            set: NavigableSet<K>, range: Range<K>): NavigableSet<K> {
        if (set.comparator() != null
                && set.comparator() !== Ordering.natural<Comparable>()
                && range.hasLowerBound()
                && range.hasUpperBound()) {
            checkArgument(
                    set.comparator()!!.compare(range.lowerEndpoint(), range.upperEndpoint()) <= 0,
                    "set is using a custom comparator which is inconsistent with the natural ordering.")
        }
        if (range.hasLowerBound() && range.hasUpperBound()) {
            return set.subSet(
                    range.lowerEndpoint(),
                    range.lowerBoundType() == BoundType.CLOSED,
                    range.upperEndpoint(),
                    range.upperBoundType() == BoundType.CLOSED)
        } else if (range.hasLowerBound()) {
            return set.tailSet(range.lowerEndpoint(), range.lowerBoundType() == BoundType.CLOSED)
        } else if (range.hasUpperBound()) {
            return set.headSet(range.upperEndpoint(), range.upperBoundType() == BoundType.CLOSED)
        }
        return checkNotNull(set)
    }
}
