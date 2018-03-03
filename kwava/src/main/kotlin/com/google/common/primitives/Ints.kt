/*
 * Copyright (C) 2008 The Guava Authors
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

package com.google.common.primitives

import com.google.common.annotations.Beta
import com.google.common.annotations.GwtCompatible
import com.google.common.base.Converter
import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkElementIndex
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.base.Preconditions.checkPositionIndexes
import java.io.Serializable
import java.util.*
import kotlin.experimental.and


/**
 * Static utility methods pertaining to `int` primitives, that are not already found in either
 * [Integer] or [Arrays].
 *
 *
 * See the Guava User Guide article on [primitive utilities](https://github.com/google/guava/wiki/PrimitivesExplained).
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@GwtCompatible
object Ints {

    /**
     * The number of bytes required to represent a primitive `int` value.
     *
     *
     * **Java 8 users:** use [Integer.BYTES] instead.
     */
    val BYTES = Integer.SIZE / java.lang.Byte.SIZE

    /**
     * The largest power of two that can be represented as an `int`.
     *
     * @since 10.0
     */
    val MAX_POWER_OF_TWO = 1 shl Integer.SIZE - 2

    /**
     * Returns a hash code for `value`; equal to the result of invoking `((Integer)
     * value).hashCode()`.
     *
     *
     * **Java 8 users:** use [Integer.hashCode] instead.
     *
     * @param value a primitive `int` value
     * @return a hash code for the value
     */
    fun hashCode(value: Int): Int {
        return value
    }

    /**
     * Returns the `int` value that is equal to `value`, if possible.
     *
     * @param value any value in the range of the `int` type
     * @return the `int` value that equals `value`
     * @throws IllegalArgumentException if `value` is greater than [Integer.MAX_VALUE] or
     * less than [Integer.MIN_VALUE]
     */
    fun checkedCast(value: Long): Int {
        val result = value.toInt()
        checkArgument(result.toLong() == value, "Out of range: %s", value)
        return result
    }

    /**
     * Returns the `int` nearest in value to `value`.
     *
     * @param value any `long` value
     * @return the same value cast to `int` if it is in the range of the `int` type,
     * [Integer.MAX_VALUE] if it is too large, or [Integer.MIN_VALUE] if it is too
     * small
     */
    fun saturatedCast(value: Long): Int {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE
        }
        return if (value < Integer.MIN_VALUE) {
            Integer.MIN_VALUE
        } else value.toInt()
    }

    /**
     * Compares the two specified `int` values. The sign of the value returned is the same as
     * that of `((Integer) a).compareTo(b)`.
     *
     *
     * **Note for Java 7 and later:** this method should be treated as deprecated; use the
     * equivalent [Integer.compare] method instead.
     *
     * @param a the first `int` to compare
     * @param b the second `int` to compare
     * @return a negative value if `a` is less than `b`; a positive value if `a` is
     * greater than `b`; or zero if they are equal
     */
    fun compare(a: Int, b: Int): Int {
        return if (a < b) -1 else if (a > b) 1 else 0
    }

    /**
     * Returns `true` if `target` is present as an element anywhere in `array`.
     *
     * @param array an array of `int` values, possibly empty
     * @param target a primitive `int` value
     * @return `true` if `array[i] == target` for some value of `i`
     */
    fun contains(array: IntArray, target: Int): Boolean {
        for (value in array) {
            if (value == target) {
                return true
            }
        }
        return false
    }

    /**
     * Returns the index of the first appearance of the value `target` in `array`.
     *
     * @param array an array of `int` values, possibly empty
     * @param target a primitive `int` value
     * @return the least index `i` for which `array[i] == target`, or `-1` if no
     * such index exists.
     */
    fun indexOf(array: IntArray, target: Int): Int {
        return indexOf(array, target, 0, array.size)
    }

    // TODO(kevinb): consider making this public
    private fun indexOf(array: IntArray, target: Int, start: Int, end: Int): Int {
        for (i in start until end) {
            if (array[i] == target) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns the start position of the first occurrence of the specified `target` within
     * `array`, or `-1` if there is no such occurrence.
     *
     *
     * More formally, returns the lowest index `i` such that `Arrays.copyOfRange(array,
     * i, i + target.length)` contains exactly the same elements as `target`.
     *
     * @param array the array to search for the sequence `target`
     * @param target the array to search for as a sub-sequence of `array`
     */
    fun indexOf(array: IntArray, target: IntArray): Int {
        checkNotNull(array, "array")
        checkNotNull(target, "target")
        if (target.size == 0) {
            return 0
        }

        outer@ for (i in 0 until array.size - target.size + 1) {
            for (j in target.indices) {
                if (array[i + j] != target[j]) {
                    continue@outer
                }
            }
            return i
        }
        return -1
    }

    /**
     * Returns the index of the last appearance of the value `target` in `array`.
     *
     * @param array an array of `int` values, possibly empty
     * @param target a primitive `int` value
     * @return the greatest index `i` for which `array[i] == target`, or `-1` if no
     * such index exists.
     */
    fun lastIndexOf(array: IntArray, target: Int): Int {
        return lastIndexOf(array, target, 0, array.size)
    }

    // TODO(kevinb): consider making this public
    private fun lastIndexOf(array: IntArray, target: Int, start: Int, end: Int): Int {
        for (i in end - 1 downTo start) {
            if (array[i] == target) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns the least value present in `array`.
     *
     * @param array a *nonempty* array of `int` values
     * @return the value present in `array` that is less than or equal to every other value in
     * the array
     * @throws IllegalArgumentException if `array` is empty
     */
    fun min(vararg array: Int): Int {
        checkArgument(array.size > 0)
        var min = array[0]
        for (i in 1 until array.size) {
            if (array[i] < min) {
                min = array[i]
            }
        }
        return min
    }

    /**
     * Returns the greatest value present in `array`.
     *
     * @param array a *nonempty* array of `int` values
     * @return the value present in `array` that is greater than or equal to every other value
     * in the array
     * @throws IllegalArgumentException if `array` is empty
     */
    fun max(vararg array: Int): Int {
        checkArgument(array.size > 0)
        var max = array[0]
        for (i in 1 until array.size) {
            if (array[i] > max) {
                max = array[i]
            }
        }
        return max
    }

    /**
     * Returns the value nearest to `value` which is within the closed range `min..max`.
     *
     *
     * If `value` is within the range `min..max`, `value` is returned
     * unchanged. If `value` is less than `min`, `min` is returned, and if `value` is greater than `max`, `max` is returned.
     *
     * @param value the `int` value to constrain
     * @param min the lower bound (inclusive) of the range to constrain `value` to
     * @param max the upper bound (inclusive) of the range to constrain `value` to
     * @throws IllegalArgumentException if `min > max`
     * @since 21.0
     */
    @Beta
    fun constrainToRange(value: Int, min: Int, max: Int): Int {
        checkArgument(min <= max, "min (%s) must be less than or equal to max (%s)", min, max)
        return Math.min(Math.max(value, min), max)
    }

    /**
     * Returns the values from each provided array combined into a single array. For example, `concat(new int[] {a, b}, new int[] {}, new int[] {c}` returns the array `{a, b, c}`.
     *
     * @param arrays zero or more `int` arrays
     * @return a single array containing all the values from the source arrays, in order
     */
    fun concat(vararg arrays: IntArray): IntArray {
        var length = 0
        for (array in arrays) {
            length += array.size
        }
        val result = IntArray(length)
        var pos = 0
        for (array in arrays) {
            System.arraycopy(array, 0, result, pos, array.size)
            pos += array.size
        }
        return result
    }

    /**
     * Returns a big-endian representation of `value` in a 4-element byte array; equivalent to
     * `ByteBuffer.allocate(4).putInt(value).array()`. For example, the input value `0x12131415` would yield the byte array `{0x12, 0x13, 0x14, 0x15}`.
     *
     *
     * If you need to convert and concatenate several values (possibly even of different types),
     * use a shared [java.nio.ByteBuffer] instance, or use [ ][com.google.common.io.ByteStreams.newDataOutput] to get a growable buffer.
     */
    fun toByteArray(value: Int): ByteArray {
        return byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte())
    }

    /**
     * Returns the `int` value whose big-endian representation is stored in the first 4 bytes of
     * `bytes`; equivalent to `ByteBuffer.wrap(bytes).getInt()`. For example, the input
     * byte array `{0x12, 0x13, 0x14, 0x15, 0x33}` would yield the `int` value `0x12131415`.
     *
     *
     * Arguably, it's preferable to use [java.nio.ByteBuffer]; that library exposes much more
     * flexibility at little cost in readability.
     *
     * @throws IllegalArgumentException if `bytes` has fewer than 4 elements
     */
    fun fromByteArray(bytes: ByteArray): Int {
        checkArgument(bytes.size >= BYTES, "array too small: %s < %s", bytes.size, BYTES)
        return fromBytes(bytes[0], bytes[1], bytes[2], bytes[3])
    }

    /**
     * Returns the `int` value whose byte representation is the given 4 bytes, in big-endian
     * order; equivalent to `Ints.fromByteArray(new byte[] {b1, b2, b3, b4})`.
     *
     * @since 7.0
     */
    fun fromBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Int {
        return b1.toInt() shl 24 or ((b2 and (0xFF).toByte()).toInt() shl 16) or ((b3 and (0xFF).toByte()).toInt() shl 8) or (b4 and (0xFF).toByte()).toInt()
    }

    private class IntConverter : Converter<String, Int>(), Serializable {

        override fun doForward(value: String): Int? {
            return Integer.decode(value)
        }

        override fun doBackward(value: Int): String {
            return value.toString()
        }

        override fun toString(): String {
            return "Ints.stringConverter()"
        }

        private fun readResolve(): Any {
            return INSTANCE
        }

        companion object {
            internal val INSTANCE = IntConverter()

            private const val serialVersionUID: Long = 1
        }
    }

    /**
     * Returns a serializable converter object that converts between strings and integers using [ ][Integer.decode] and [Integer.toString]. The returned converter throws [ ] if the input string is invalid.
     *
     *
     * **Warning:** please see [Integer.decode] to understand exactly how strings are
     * parsed. For example, the string `"0123"` is treated as *octal* and converted to the
     * value `83`.
     *
     * @since 16.0
     */
    @Beta
    fun stringConverter(): Converter<String, Int> {
        return IntConverter.INSTANCE
    }

    /**
     * Returns an array containing the same values as `array`, but guaranteed to be of a
     * specified minimum length. If `array` already has a length of at least `minLength`,
     * it is returned directly. Otherwise, a new array of size `minLength + padding` is
     * returned, containing the values of `array`, and zeroes in the remaining places.
     *
     * @param array the source array
     * @param minLength the minimum length the returned array must guarantee
     * @param padding an extra amount to "grow" the array by if growth is necessary
     * @throws IllegalArgumentException if `minLength` or `padding` is negative
     * @return an array containing the values of `array`, with guaranteed minimum length `minLength`
     */
    fun ensureCapacity(array: IntArray, minLength: Int, padding: Int): IntArray {
        checkArgument(minLength >= 0, "Invalid minLength: %s", minLength)
        checkArgument(padding >= 0, "Invalid padding: %s", padding)
        return if (array.size < minLength) Arrays.copyOf(array, minLength + padding) else array
    }

    /**
     * Returns a string containing the supplied `int` values separated by `separator`. For
     * example, `join("-", 1, 2, 3)` returns the string `"1-2-3"`.
     *
     * @param separator the text that should appear between consecutive values in the resulting string
     * (but not at the start or end)
     * @param array an array of `int` values, possibly empty
     */
    fun join(separator: String, vararg array: Int): String {
        checkNotNull(separator)
        if (array.size == 0) {
            return ""
        }

        // For pre-sizing a builder, just get the right order of magnitude
        val builder = StringBuilder(array.size * 5)
        builder.append(array[0])
        for (i in 1 until array.size) {
            builder.append(separator).append(array[i])
        }
        return builder.toString()
    }

    /**
     * Returns a comparator that compares two `int` arrays [lexicographically](http://en.wikipedia.org/wiki/Lexicographical_order). That is, it
     * compares, using [.compare]), the first pair of values that follow any common
     * prefix, or when one array is a prefix of the other, treats the shorter array as the lesser. For
     * example, `[] < [1] < [1, 2] < [2]`.
     *
     *
     * The returned comparator is inconsistent with [Object.equals] (since arrays
     * support only identity equality), but it is consistent with [Arrays.equals].
     *
     * @since 2.0
     */
    fun lexicographicalComparator(): Comparator<IntArray> {
        return LexicographicalComparator.INSTANCE
    }

    private enum class LexicographicalComparator : Comparator<IntArray> {
        INSTANCE;

        override fun compare(left: IntArray, right: IntArray): Int {
            val minLength = Math.min(left.size, right.size)
            for (i in 0 until minLength) {
                val result = Ints.compare(left[i], right[i])
                if (result != 0) {
                    return result
                }
            }
            return left.size - right.size
        }

        override fun toString(): String {
            return "Ints.lexicographicalComparator()"
        }
    }

    /**
     * Sorts the elements of `array` in descending order.
     *
     * @since 23.1
     */
    fun sortDescending(array: IntArray) {
        checkNotNull(array)
        sortDescending(array, 0, array.size)
    }

    /**
     * Sorts the elements of `array` between `fromIndex` inclusive and `toIndex`
     * exclusive in descending order.
     *
     * @since 23.1
     */
    fun sortDescending(array: IntArray, fromIndex: Int, toIndex: Int) {
        checkNotNull(array)
        checkPositionIndexes(fromIndex, toIndex, array.size)
        array.sort(fromIndex, toIndex)
        reverse(array, fromIndex, toIndex)
    }

    /**
     * Reverses the elements of `array`. This is equivalent to `Collections.reverse(Ints.asList(array))`, but is likely to be more efficient.
     *
     * @since 23.1
     */
    fun reverse(array: IntArray) {
        checkNotNull(array)
        reverse(array, 0, array.size)
    }

    /**
     * Reverses the elements of `array` between `fromIndex` inclusive and `toIndex`
     * exclusive. This is equivalent to `Collections.reverse(Ints.asList(array).subList(fromIndex, toIndex))`, but is likely to be more
     * efficient.
     *
     * @throws IndexOutOfBoundsException if `fromIndex < 0`, `toIndex > array.length`, or
     * `toIndex > fromIndex`
     * @since 23.1
     */
    fun reverse(array: IntArray, fromIndex: Int, toIndex: Int) {
        checkNotNull(array)
        checkPositionIndexes(fromIndex, toIndex, array.size)
        var i = fromIndex
        var j = toIndex - 1
        while (i < j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }

    /**
     * Returns an array containing each value of `collection`, converted to a `int` value
     * in the manner of [Number.intValue].
     *
     *
     * Elements are copied from the argument collection as if by `collection.toArray()`.
     * Calling this method is as thread-safe as calling that method.
     *
     * @param collection a collection of `Number` instances
     * @return an array containing the same values as `collection`, in the same order, converted
     * to primitives
     * @throws NullPointerException if `collection` or any of its elements is null
     * @since 1.0 (parameter was `Collection<Integer>` before 12.0)
     */
    fun toArray(collection: Collection<Number>): IntArray {
        if (collection is IntArrayAsList) {
            return collection.toIntArray()
        }

        val boxedArray = collection.toTypedArray()
        val len = boxedArray.size
        val array = IntArray(len)
        for (i in 0 until len) {
            // checkNotNull for GWT (do not optimize)
            array[i] = checkNotNull(boxedArray[i]).toInt()
        }
        return array
    }

    /**
     * Returns a fixed-size list backed by the specified array, similar to [ ][Arrays.asList]. The list supports [List.set], but any attempt to
     * set a value to `null` will result in a [NullPointerException].
     *
     *
     * The returned list maintains the values, but not the identities, of `Integer` objects
     * written to or read from it. For example, whether `list.get(0) == list.get(0)` is true for
     * the returned list is unspecified.
     *
     *
     * **Note:** when possible, you should represent your data as an [ImmutableIntArray]
     * instead, which has an [asList][ImmutableIntArray.asList] view.
     *
     * @param backingArray the array to back the list
     * @return a list view of the array
     */
    fun asList(vararg backingArray: Int): List<Int> {
        return if (backingArray.size == 0) {
            emptyList()
        } else IntArrayAsList(backingArray)
    }

    @GwtCompatible
    private class IntArrayAsList
    @JvmOverloads
    internal constructor(
            internal val array: IntArray,
            internal val start: Int = 0,
            internal val end: Int = array.size) : AbstractMutableList<Int>(), RandomAccess, Serializable {

        override fun add(index: Int, element: Int) {
            throw UnsupportedOperationException()
        }

        override fun removeAt(index: Int): Int {
            throw UnsupportedOperationException()
        }

        override val size: Int
            get() = end - start

        override fun isEmpty(): Boolean {
            return false
        }

        override fun get(index: Int): Int {
            checkElementIndex(index, size)
            return array[start + index]
        }

        override fun spliterator(): Spliterator.OfInt {
            return Spliterators.spliterator(array, start, end, 0)
        }

        override operator fun contains(target: Int): Boolean {
            // Overridden to prevent a ton of boxing
            return Ints.indexOf(array, (target as Int?)!!, start, end) != -1
        }

        override fun indexOf(target: Int): Int {
            // Overridden to prevent a ton of boxing
            val i = Ints.indexOf(array, (target as Int?)!!, start, end)
            if (i >= 0) {
                return i - start
            }
            return -1
        }

        override fun lastIndexOf(target: Int): Int {
            // Overridden to prevent a ton of boxing
            val i = Ints.lastIndexOf(array, (target as Int?)!!, start, end)
            if (i >= 0) {
                return i - start
            }
            return -1
        }

        override fun set(index: Int, element: Int): Int {
            checkElementIndex(index, size)
            val oldValue = array[start + index]
            array[start + index] = element
            return oldValue
        }

        override fun subList(fromIndex: Int, toIndex: Int): MutableList<Int> {
            val size = size
            checkPositionIndexes(fromIndex, toIndex, size)
            return if (fromIndex == toIndex) {
                mutableListOf()
            } else IntArrayAsList(array, start + fromIndex, start + toIndex)
        }

        override fun equals(`object`: Any?): Boolean {
            if (`object` === this) {
                return true
            }
            if (`object` is IntArrayAsList) {
                val that = `object` as IntArrayAsList?
                val size = size
                if (that!!.size != size) {
                    return false
                }
                for (i in 0 until size) {
                    if (array[start + i] != that.array[that.start + i]) {
                        return false
                    }
                }
                return true
            }
            return super.equals(`object`)
        }

        override fun hashCode(): Int {
            var result = 1
            for (i in start until end) {
                result = 31 * result + Ints.hashCode(array[i])
            }
            return result
        }

        override fun toString(): String {
            val builder = StringBuilder(size * 5)
            builder.append('[').append(array[start])
            for (i in start + 1 until end) {
                builder.append(", ").append(array[i])
            }
            return builder.append(']').toString()
        }

        internal fun toIntArray(): IntArray {
            return Arrays.copyOfRange(array, start, end)
        }

        companion object {

            private const val serialVersionUID: Long = 0
        }
    }

    /**
     * Parses the specified string as a signed integer value using the specified radix. The ASCII
     * character `'-'` (`'&#92;u002D'`) is recognized as the minus sign.
     *
     *
     * Unlike [Integer.parseInt], this method returns `null` instead of
     * throwing an exception if parsing fails. Additionally, this method only accepts ASCII digits,
     * and returns `null` if non-ASCII digits are present in the string.
     *
     *
     * Note that strings prefixed with ASCII `'+'` are rejected, even under JDK 7, despite
     * the change to [Integer.parseInt] for that version.
     *
     * @param string the string representation of an integer value
     * @param radix the radix to use when parsing
     * @return the integer value represented by `string` using `radix`, or `null` if
     * `string` has a length of zero or cannot be parsed as an integer value
     * @throws IllegalArgumentException if `radix < Character.MIN_RADIX` or `radix >
     * Character.MAX_RADIX`
     * @since 19.0
     */
    @Beta
    @JvmOverloads
    fun tryParse(string: String, radix: Int = 10): Int? {
        val result = Longs.tryParse(string, radix)
        return if (result == null || result.toLong() != result.toInt().toLong()) {
            null
        } else {
            result.toInt()
        }
    }
}
/**
 * Parses the specified string as a signed decimal integer value. The ASCII character `'-'`
 * (`'&#92;u002D'`) is recognized as the minus sign.
 *
 *
 * Unlike [Integer.parseInt], this method returns `null` instead of
 * throwing an exception if parsing fails. Additionally, this method only accepts ASCII digits,
 * and returns `null` if non-ASCII digits are present in the string.
 *
 *
 * Note that strings prefixed with ASCII `'+'` are rejected, even under JDK 7, despite
 * the change to [Integer.parseInt] for that version.
 *
 * @param string the string representation of an integer value
 * @return the integer value represented by `string`, or `null` if `string` has
 * a length of zero or cannot be parsed as an integer value
 * @since 11.0
 */
