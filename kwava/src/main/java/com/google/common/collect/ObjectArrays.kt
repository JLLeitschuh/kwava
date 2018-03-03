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

import com.google.common.annotations.GwtCompatible
import com.google.common.annotations.GwtIncompatible


/**
 * Static utility methods pertaining to object arrays.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible(emulated = true)
object ObjectArrays {

    /**
     * Returns a new array of the given length with the specified component type.
     *
     * @param type the component type
     * @param length the length of the new array
     */
    @GwtIncompatible // Array.newInstance(Class, int)
    fun <T : Any?> newArray(type: Class<T>, length: Int): Array<T> {
        return Array<Any?>(length) { null } as Array<T>
    }

    /**
     * Returns a new array of the given length with the same type as a reference array.
     *
     * @param reference any array of the desired type
     * @param length the length of the new array
     */
    fun <T> newArray(reference: Array<T>, length: Int): Array<T> {
        return Platform.newArray<T>(reference, length)
    }

    /**
     * Returns a new array that contains the concatenated contents of two arrays.
     *
     * @param first the first array of elements to concatenate
     * @param second the second array of elements to concatenate
     * @param type the component type of the returned array
     */
    @GwtIncompatible // Array.newInstance(Class, int)
    fun <T> concat(first: Array<T>, second: Array<T>, type: Class<T>): Array<T> {
        return first + second
    }

    /**
     * Returns a new array that prepends `element` to `array`.
     *
     * @param element the element to prepend to the front of `array`
     * @param array the array of elements to append
     * @return an array whose size is one larger than `array`, with `element` occupying
     * the first position, and the elements of `array` occupying the remaining elements.
     */
    fun <T> concat(element: T, array: Array<T>): Array<T> {
        return (arrayOf<Any?>(element) as Array<T>) + array
    }

    /**
     * Returns a new array that appends `element` to `array`.
     *
     * @param array the array of elements to prepend
     * @param element the element to append to the end
     * @return an array whose size is one larger than `array`, with the same contents as `array`, plus `element` occupying the last position.
     */
    fun <T> concat(array: Array<T>, element: T): Array<T> {
        return array + element
    }

    /**
     * Returns an array containing all of the elements in the specified collection; the runtime type
     * of the returned array is that of the specified array. If the collection fits in the specified
     * array, it is returned therein. Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of the specified collection.
     *
     *
     * If the collection fits in the specified array with room to spare (i.e., the array has more
     * elements than the collection), the element in the array immediately following the end of the
     * collection is set to `null`. This is useful in determining the length of the collection
     * *only* if the caller knows that the collection does not contain any null elements.
     *
     *
     * This method returns the elements in the order they are returned by the collection's
     * iterator.
     *
     *
     * TODO(kevinb): support concurrently modified collections?
     *
     * @param c the collection for which to return an array of elements
     * @param array the array in which to place the collection elements
     * @throws ArrayStoreException if the runtime type of the specified array is not a supertype of
     * the runtime type of every element in the specified collection
     */
    internal fun <T> toArrayImpl(c: Collection<*>, array: Array<T>): Array<T> {
        var array = array
        val size = c.size
        if (array.size < size) {
            array = newArray<T>(array, size)
        }
        fillArray(c, array as Array<Any?>)
        if (array.size > size) {
            array[size] = null
        }
        return array
    }


    private fun fillArray(elements: Iterable<*>, array: Array<Any?>): Array<Any?> {
        var i = 0
        for (element in elements) {
            array[i++] = element
        }
        return array
    }

    /** Swaps `array[i]` with `array[j]`.  */
    internal fun swap(array: Array<Any>, i: Int, j: Int) {
        val temp = array[i]
        array[i] = array[j]
        array[j] = temp
    }


    internal fun checkElementsNotNull(vararg array: Any): Array<Any> {
        return checkElementsNotNull(array, array.size)
    }


    internal fun checkElementsNotNull(array: Array<Any>, length: Int): Array<Any> {
        for (i in 0 until length) {
            checkElementNotNull(array[i], i)
        }
        return array
    }

    // We do this instead of Preconditions.checkNotNull to save boxing and array
    // creation cost.

    internal fun checkElementNotNull(element: Any?, index: Int): Any {
        if (element == null) {
            throw NullPointerException("at index $index")
        }
        return element
    }
}
