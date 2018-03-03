/*
 * Copyright (C) 2015 The Guava Authors
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

import com.google.common.annotations.GwtCompatible
import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkNotNull
import java.util.*
import java.util.function.*
import java.util.function.Function
import java.util.stream.IntStream


/** Spliterator utilities for `common.collect` internals.  */
@GwtCompatible
internal object CollectSpliterators {

    fun <T> indexed(size: Int, extraCharacteristics: Int, function: IntFunction<T>): Spliterator<T> {
        return indexed(size, extraCharacteristics, function, null)
    }

    fun <T> indexed(
            size: Int,
            extraCharacteristics: Int,
            function: IntFunction<T>,
            comparator: Comparator<in T>?): Spliterator<T> {
        if (comparator != null) {
            checkArgument(extraCharacteristics and Spliterator.SORTED != 0)
        }
        class WithCharacteristics(private val delegate: Spliterator.OfInt) : Spliterator<T> {

            override fun tryAdvance(action: Consumer<in T>): Boolean {
                return delegate.tryAdvance({ i : Int -> action.accept(function.apply(i)) } as IntConsumer)
            }

            override fun forEachRemaining(action: Consumer<in T>) {
                delegate.forEachRemaining({ i : Int -> action.accept(function.apply(i)) } as IntConsumer)
            }

            override fun trySplit(): Spliterator<T>? {
                val split = delegate.trySplit()
                return if (split == null) null else WithCharacteristics(split)
            }

            override fun estimateSize(): Long {
                return delegate.estimateSize()
            }

            override fun characteristics(): Int {
                return (Spliterator.ORDERED
                        or Spliterator.SIZED
                        or Spliterator.SUBSIZED
                        or extraCharacteristics)
            }

            override fun getComparator(): Comparator<in T>? {
                return if (hasCharacteristics(Spliterator.SORTED)) {
                    comparator
                } else {
                    throw IllegalStateException()
                }
            }
        }
        return WithCharacteristics(IntStream.range(0, size).spliterator())
    }

    /**
     * Returns a `Spliterator` over the elements of `fromSpliterator` mapped by `function`.
     */
    fun <F, T> map(
            fromSpliterator: Spliterator<F>?, function: Function<in F, out T>): Spliterator<T> {
        checkNotNull(fromSpliterator)
        checkNotNull(function)
        return object : Spliterator<T> {

            override fun tryAdvance(action: Consumer<in T>): Boolean {
                return fromSpliterator!!.tryAdvance { fromElement -> action.accept(function.apply(fromElement)) }
            }

            override fun forEachRemaining(action: Consumer<in T>) {
                fromSpliterator!!.forEachRemaining { fromElement -> action.accept(function.apply(fromElement)) }
            }

            override fun trySplit(): Spliterator<T>? {
                val fromSplit = fromSpliterator!!.trySplit()
                return if (fromSplit != null) map(fromSplit, function) else null
            }

            override fun estimateSize(): Long {
                return fromSpliterator!!.estimateSize()
            }

            override fun characteristics(): Int {
                return fromSpliterator!!.characteristics() and (Spliterator.DISTINCT or Spliterator.NONNULL or Spliterator.SORTED).inv()
            }
        }
    }

    /** Returns a `Spliterator` filtered by the specified predicate.  */
    fun <T> filter(fromSpliterator: Spliterator<T>, predicate: Predicate<in T?>): Spliterator<T> {
        checkNotNull(fromSpliterator)
        checkNotNull(predicate)
        class Splitr : Spliterator<T>, Consumer<T> {
            var holder: T? = null

            override fun accept(t: T) {
                this.holder = t
            }

            override fun tryAdvance(action: Consumer<in T?>): Boolean {
                while (fromSpliterator.tryAdvance(this)) {
                    try {
                        if (predicate.test(holder)) {
                            action.accept(holder)
                            return true
                        }
                    } finally {
                        holder = null
                    }
                }
                return false
            }

            override fun trySplit(): Spliterator<T>? {
                val fromSplit = fromSpliterator.trySplit()
                return if (fromSplit == null) null else filter(fromSplit, predicate)
            }

            override fun estimateSize(): Long {
                return fromSpliterator.estimateSize() / 2
            }

            override fun getComparator(): Comparator<in T> {
                return fromSpliterator.comparator
            }

            override fun characteristics(): Int {
                return fromSpliterator.characteristics() and (Spliterator.DISTINCT
                        or Spliterator.NONNULL
                        or Spliterator.ORDERED
                        or Spliterator.SORTED)
            }
        }
        return Splitr()
    }

    /**
     * Returns a `Spliterator` that iterates over the elements of the spliterators generated by
     * applying `function` to the elements of `fromSpliterator`.
     */
    fun <F, T> flatMap(
            fromSpliterator: Spliterator<F>,
            function: Function<in F, Spliterator<T>>,
            topCharacteristics: Int,
            topSize: Long): Spliterator<T> {
        checkArgument(
                topCharacteristics and Spliterator.SUBSIZED == 0,
                "flatMap does not support SUBSIZED characteristic")
        checkArgument(
                topCharacteristics and Spliterator.SORTED == 0,
                "flatMap does not support SORTED characteristic")
        checkNotNull(fromSpliterator)
        checkNotNull(function)
        class FlatMapSpliterator(
                var prefix: Spliterator<T>?, val from: Spliterator<F>, var characteristics: Int, var estimatedSize: Long) : Spliterator<T> {

            override fun tryAdvance(action: Consumer<in T>): Boolean {
                while (true) {
                    if (prefix != null && prefix!!.tryAdvance(action)) {
                        if (estimatedSize != java.lang.Long.MAX_VALUE) {
                            estimatedSize--
                        }
                        return true
                    } else {
                        prefix = null
                    }
                    if (!from.tryAdvance { fromElement -> prefix = function.apply(fromElement) }) {
                        return false
                    }
                }
            }

            override fun forEachRemaining(action: Consumer<in T>) {
                if (prefix != null) {
                    prefix!!.forEachRemaining(action)
                    prefix = null
                }
                from.forEachRemaining { fromElement -> function.apply(fromElement).forEachRemaining(action) }
                estimatedSize = 0
            }

            override fun trySplit(): Spliterator<T>? {
                val fromSplit = from.trySplit()
                if (fromSplit != null) {
                    val splitCharacteristics = characteristics and Spliterator.SIZED.inv()
                    var estSplitSize = estimateSize()
                    if (estSplitSize < java.lang.Long.MAX_VALUE) {
                        estSplitSize /= 2
                        this.estimatedSize -= estSplitSize
                        this.characteristics = splitCharacteristics
                    }
                    val result = FlatMapSpliterator(this.prefix, fromSplit, splitCharacteristics, estSplitSize)
                    this.prefix = null
                    return result
                } else if (prefix != null) {
                    val result = prefix
                    this.prefix = null
                    return result
                } else {
                    return null
                }
            }

            override fun estimateSize(): Long {
                if (prefix != null) {
                    estimatedSize = Math.max(estimatedSize, prefix!!.estimateSize())
                }
                return Math.max(estimatedSize, 0)
            }

            override fun characteristics(): Int {
                return characteristics
            }
        }
        return FlatMapSpliterator(null, fromSpliterator, topCharacteristics, topSize)
    }
}
