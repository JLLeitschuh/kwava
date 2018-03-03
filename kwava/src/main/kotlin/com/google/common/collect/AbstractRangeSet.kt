/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect

import com.google.common.annotations.GwtIncompatible


/**
 * A skeletal implementation of `RangeSet`.
 *
 * @author Louis Wasserman
 */
@GwtIncompatible
internal abstract class AbstractRangeSet<C : Comparable<*>> : RangeSet<C> {

    override fun contains(value: C): Boolean {
        return rangeContaining(value) != null
    }

    abstract override fun rangeContaining(value: C): Range<C>?

    override fun isEmpty(): Boolean {
        return asRanges().isEmpty()
    }

    override fun add(range: Range<C>) {
        throw UnsupportedOperationException()
    }

    override fun remove(range: Range<C>) {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        remove(Range.all())
    }

    override fun enclosesAll(other: RangeSet<C>): Boolean {
        return enclosesAll(other.asRanges())
    }

    override fun addAll(other: RangeSet<C>) {
        addAll(other.asRanges())
    }

    override fun removeAll(other: RangeSet<C>) {
        removeAll(other.asRanges())
    }

    override fun intersects(otherRange: Range<C>): Boolean {
        return !subRangeSet(otherRange).isEmpty
    }

    abstract override fun encloses(otherRange: Range<C>): Boolean

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        } else if (obj is RangeSet<*>) {
            val other = obj as RangeSet<*>?
            return this.asRanges() == other!!.asRanges()
        }
        return false
    }

    override fun hashCode(): Int {
        return asRanges().hashCode()
    }

    override fun toString(): String {
        return asRanges().toString()
    }
}
