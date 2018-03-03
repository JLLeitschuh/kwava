/*
 * Copyright (C) 2008 The Guava Authors
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
import com.google.common.primitives.Ints


/**
 * Static methods for implementing hash-based collections.
 *
 * @author Kevin Bourrillion
 * @author Jesse Wilson
 * @author Austin Appleby
 */
@GwtCompatible
internal object Hashing {

    /*
   * These should be ints, but we need to use longs to force GWT to do the multiplications with
   * enough precision.
   */
    private const val C1: Long = -0x3361d2af
    private const val C2: Long = 0x1b873593

    private val MAX_TABLE_SIZE = Ints.MAX_POWER_OF_TWO

    /*
   * This method was rewritten in Java from an intermediate step of the Murmur hash function in
   * http://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp, which contained the
   * following header:
   *
   * MurmurHash3 was written by Austin Appleby, and is placed in the public domain. The author
   * hereby disclaims copyright to this source code.
   */
    fun smear(hashCode: Int): Int {
        return (C2 * Integer.rotateLeft((hashCode * C1).toInt(), 15)).toInt()
    }

    fun smearedHash(o: Any?): Int {
        return smear(o?.hashCode() ?: 0)
    }

    fun closedTableSize(expectedEntries: Int, loadFactor: Double): Int {
        var expectedEntries = expectedEntries
        // Get the recommended table size.
        // Round down to the nearest power of 2.
        expectedEntries = Math.max(expectedEntries, 2)
        var tableSize = Integer.highestOneBit(expectedEntries)
        // Check to make sure that we will not exceed the maximum load factor.
        if (expectedEntries > (loadFactor * tableSize).toInt()) {
            tableSize = tableSize shl 1
            return if (tableSize > 0) tableSize else MAX_TABLE_SIZE
        }
        return tableSize
    }

    fun needsResizing(size: Int, tableSize: Int, loadFactor: Double): Boolean {
        return size > loadFactor * tableSize && tableSize < MAX_TABLE_SIZE
    }
}
