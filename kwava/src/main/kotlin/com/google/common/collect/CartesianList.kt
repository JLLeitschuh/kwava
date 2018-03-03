/*
 * Copyright (C) 2012 The Guava Authors
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

import com.google.common.base.Preconditions.checkElementIndex

import com.google.common.annotations.GwtCompatible
import com.google.common.math.IntMath
import java.util.AbstractList
import java.util.RandomAccess


/**
 * Implementation of [Lists.cartesianProduct].
 *
 * @author Louis Wasserman
 */
@GwtCompatible
internal class CartesianList<E>(@field:Transient private val axes: ImmutableList<List<E>>) : AbstractList<List<E>>(), RandomAccess {
    @Transient
    private val axesSizeProduct: IntArray

    init {
        val axesSizeProduct = IntArray(axes.size + 1)
        axesSizeProduct[axes.size] = 1
        try {
            for (i in axes.indices.reversed()) {
                axesSizeProduct[i] = IntMath.checkedMultiply(axesSizeProduct[i + 1], axes[i].size)
            }
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException(
                    "Cartesian product too large; must have size at most Integer.MAX_VALUE")
        }

        this.axesSizeProduct = axesSizeProduct
    }

    private fun getAxisIndexForProductIndex(index: Int, axis: Int): Int {
        return index / axesSizeProduct[axis + 1] % axes[axis].size
    }

    override fun get(index: Int): ImmutableList<E> {
        checkElementIndex(index, size)
        return object : ImmutableList<E>() {

            override fun size(): Int {
                return axes.size
            }

            override fun get(axis: Int): E {
                checkElementIndex(axis, size)
                val axisIndex = getAxisIndexForProductIndex(index, axis)
                return axes[axis][axisIndex]
            }

            internal override fun isPartialView(): Boolean {
                return true
            }
        }
    }

    override fun size(): Int {
        return axesSizeProduct[0]
    }

    override operator fun contains(o: Any?): Boolean {
        if (o !is List<*>) {
            return false
        }
        val list = o as List<*>?
        if (list!!.size != axes.size) {
            return false
        }
        val itr = list.listIterator()
        while (itr.hasNext()) {
            val index = itr.nextIndex()
            if (!axes[index].contains(itr.next())) {
                return false
            }
        }
        return true
    }

    companion object {

        fun <E> create(lists: List<List<E>>): List<List<E>> {
            val axesBuilder = ImmutableList.Builder<List<E>>(lists.size)
            for (list in lists) {
                val copy = ImmutableList.copyOf(list)
                if (copy.isEmpty()) {
                    return ImmutableList.of()
                }
                axesBuilder.add(copy)
            }
            return CartesianList(axesBuilder.build())
        }
    }
}
