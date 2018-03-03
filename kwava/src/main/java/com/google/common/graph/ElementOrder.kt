/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.base.Preconditions.checkState

import com.google.common.annotations.Beta
import com.google.common.base.MoreObjects
import com.google.common.base.MoreObjects.ToStringHelper
import com.google.common.base.Objects
import com.google.common.collect.Maps
import com.google.common.collect.Ordering
import com.google.errorprone.annotations.Immutable
import java.util.Comparator


/**
 * Used to represent the order of elements in a data structure that supports different options for
 * iteration order guarantees.
 *
 *
 * Example usage:
 *
 * <pre>`MutableGraph<Integer> graph =
 * GraphBuilder.directed().nodeOrder(ElementOrder.<Integer>natural()).build();
`</pre> *
 *
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
@Immutable
class ElementOrder<T> private constructor(type: Type, private// Hopefully the comparator provided is immutable!
val comparator: Comparator<T>?) {
    private val type: Type

    /**
     * The type of ordering that this object specifies.
     *
     *
     *  * UNORDERED: no order is guaranteed.
     *  * INSERTION: insertion ordering is guaranteed.
     *  * SORTED: ordering according to a supplied comparator is guaranteed.
     *
     */
    enum class Type {
        UNORDERED,
        INSERTION,
        SORTED
    }

    init {
        this.type = checkNotNull(type)
        checkState(type == Type.SORTED == (comparator != null))
    }

    /** Returns the type of ordering used.  */
    fun type(): Type {
        return type
    }

    /**
     * Returns the [Comparator] used.
     *
     * @throws UnsupportedOperationException if comparator is not defined
     */
    fun comparator(): Comparator<T> {
        if (comparator != null) {
            return comparator
        }
        throw UnsupportedOperationException("This ordering does not define a comparator.")
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj !is ElementOrder<*>) {
            return false
        }

        val other = obj as ElementOrder<*>?
        return type == other!!.type && Objects.equal(comparator, other.comparator)
    }

    override fun hashCode(): Int {
        return Objects.hashCode(type, comparator)
    }

    override fun toString(): String {
        val helper = MoreObjects.toStringHelper(this).add("type", type)
        if (comparator != null) {
            helper.add("comparator", comparator)
        }
        return helper.toString()
    }

    /** Returns an empty mutable map whose keys will respect this [ElementOrder].  */
    internal fun <K : T, V> createMap(expectedSize: Int): Map<K, V> {
        when (type) {
            ElementOrder.Type.UNORDERED -> return Maps.newHashMapWithExpectedSize(expectedSize)
            ElementOrder.Type.INSERTION -> return Maps.newLinkedHashMapWithExpectedSize(expectedSize)
            ElementOrder.Type.SORTED -> return Maps.newTreeMap(comparator())
            else -> throw AssertionError()
        }
    }

    internal fun <T1 : T> cast(): ElementOrder<T1> {
        return this as ElementOrder<T1>
    }

    companion object {

        /** Returns an instance which specifies that no ordering is guaranteed.  */
        fun <S> unordered(): ElementOrder<S> {
            return ElementOrder(Type.UNORDERED, null)
        }

        /** Returns an instance which specifies that insertion ordering is guaranteed.  */
        fun <S> insertion(): ElementOrder<S> {
            return ElementOrder(Type.INSERTION, null)
        }

        /**
         * Returns an instance which specifies that the natural ordering of the elements is guaranteed.
         */
        fun <S : Comparable<S>> natural(): ElementOrder<S> {
            return ElementOrder(Type.SORTED, Ordering.natural())
        }

        /**
         * Returns an instance which specifies that the ordering of the elements is guaranteed to be
         * determined by `comparator`.
         */
        fun <S> sorted(comparator: Comparator<S>): ElementOrder<S> {
            return ElementOrder(Type.SORTED, comparator)
        }
    }
}
