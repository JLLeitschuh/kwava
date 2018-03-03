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

import com.google.common.base.Preconditions.checkNotNull

import com.google.common.annotations.GwtCompatible
import com.google.common.base.Function
import com.google.common.base.Objects
import java.io.Serializable


/**
 * An ordering that orders elements by applying an order to the result of a function on those
 * elements.
 */
@GwtCompatible(serializable = true)
internal class ByFunctionOrdering<F, T>(function: Function<F, out T>, ordering: Ordering<T>) : Ordering<F>(), Serializable {
    val function: Function<F, out T>
    val ordering: Ordering<T>

    init {
        this.function = checkNotNull(function)
        this.ordering = checkNotNull(ordering)
    }

    override fun compare(left: F, right: F): Int {
        return ordering.compare(function.apply(left), function.apply(right))
    }

    override fun equals(`object`: Any?): Boolean {
        if (`object` === this) {
            return true
        }
        if (`object` is ByFunctionOrdering<*, *>) {
            val that = `object` as ByFunctionOrdering<*, *>?
            return this.function == that!!.function && this.ordering == that.ordering
        }
        return false
    }

    override fun hashCode(): Int {
        return Objects.hashCode(function, ordering)
    }

    override fun toString(): String {
        return ordering.toString() + ".onResultOf(" + function + ")"
    }

    companion object {

        private const val serialVersionUID: Long = 0
    }
}
