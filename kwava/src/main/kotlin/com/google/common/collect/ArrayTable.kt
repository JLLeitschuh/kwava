/*
 * Copyright (C) 2009 The Guava Authors
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
import com.google.common.base.Preconditions.checkElementIndex
import com.google.common.base.Preconditions.checkNotNull

import com.google.common.annotations.Beta
import com.google.common.annotations.GwtCompatible
import com.google.common.annotations.GwtIncompatible
import com.google.common.base.Objects
import com.google.common.collect.Maps.IteratorBasedAbstractMap

import com.google.j2objc.annotations.WeakOuter
import java.io.Serializable
import java.lang.reflect.Array
import java.util.Arrays
import java.util.Spliterator


/**
 * Fixed-size [Table] implementation backed by a two-dimensional array.
 *
 *
 * The allowed row and column keys must be supplied when the table is created. The table always
 * contains a mapping for every row key / column pair. The value corresponding to a given row and
 * column is null unless another value is provided.
 *
 *
 * The table's size is constant: the product of the number of supplied row keys and the number of
 * supplied column keys. The `remove` and `clear` methods are not supported by the table
 * or its views. The [.erase] and [.eraseAll] methods may be used instead.
 *
 *
 * The ordering of the row and column keys provided when the table is constructed determines the
 * iteration ordering across rows and columns in the table's views. None of the view iterators
 * support [Iterator.remove]. If the table is modified after an iterator is created, the
 * iterator remains valid.
 *
 *
 * This class requires less memory than the [HashBasedTable] and [TreeBasedTable]
 * implementations, except when the table is sparse.
 *
 *
 * Null row keys or column keys are not permitted.
 *
 *
 * This class provides methods involving the underlying array structure, where the array indices
 * correspond to the position of a row or column in the lists of allowed keys and values. See the
 * [.at], [.set], [.toArray], [.rowKeyList], and [.columnKeyList]
 * methods for more details.
 *
 *
 * Note that this implementation is not synchronized. If multiple threads access the same cell of
 * an `ArrayTable` concurrently and one of the threads modifies its value, there is no
 * guarantee that the new value will be fully visible to the other threads. To guarantee that
 * modifications are visible, synchronize access to the table. Unlike other `Table`
 * implementations, synchronization is unnecessary between a thread that writes to one cell and a
 * thread that reads from another.
 *
 *
 * See the Guava User Guide article on [ `Table`](https://github.com/google/guava/wiki/NewCollectionTypesExplained#table).
 *
 * @author Jared Levy
 * @since 10.0
 */
@Beta
@GwtCompatible(emulated = true)
class ArrayTable<R, C, V> : AbstractTable<R, C, V>, Serializable {

    private val rowList: ImmutableList<R>
    private val columnList: ImmutableList<C>

    // TODO(jlevy): Add getters returning rowKeyToIndex and columnKeyToIndex?
    private val rowKeyToIndex: ImmutableMap<R, Int>
    private val columnKeyToIndex: ImmutableMap<C, Int>
    private val array: Array<Array<V>>


    @Transient
    private var columnMap: ColumnMap? = null


    @Transient
    private var rowMap: RowMap? = null

    private constructor(rowKeys: Iterable<R>, columnKeys: Iterable<C>) {
        this.rowList = ImmutableList.copyOf(rowKeys)
        this.columnList = ImmutableList.copyOf(columnKeys)
        checkArgument(rowList.isEmpty() == columnList.isEmpty())

        /*
     * TODO(jlevy): Support only one of rowKey / columnKey being empty? If we
     * do, when columnKeys is empty but rowKeys isn't, rowKeyList() can contain
     * elements but rowKeySet() will be empty and containsRow() won't
     * acknolwedge them.
     */
        rowKeyToIndex = Maps.indexMap(rowList)
        columnKeyToIndex = Maps.indexMap(columnList)

        val tmpArray = Array<Array<Any>>(rowList.size) { arrayOfNulls<Any>(columnList.size) } as Array<Array<V>>
        array = tmpArray
        // Necessary because in GWT the arrays are initialized with "undefined" instead of null.
        eraseAll()
    }

    private constructor(table: Table<R, C, V>) : this(table.rowKeySet(), table.columnKeySet()) {
        putAll(table)
    }

    private constructor(table: ArrayTable<R, C, V>) {
        rowList = table.rowList
        columnList = table.columnList
        rowKeyToIndex = table.rowKeyToIndex
        columnKeyToIndex = table.columnKeyToIndex
        val copy = Array<Array<Any>>(rowList.size) { arrayOfNulls<Any>(columnList.size) } as Array<Array<V>>
        array = copy
        for (i in rowList.indices) {
            System.arraycopy(table.array[i], 0, copy[i], 0, table.array[i].size)
        }
    }

    private abstract class ArrayMap<K, V> private constructor(private val keyIndex: ImmutableMap<K, Int>) : IteratorBasedAbstractMap<K, V>() {

        internal abstract val keyRole: String

        override fun keySet(): Set<K> {
            return keyIndex.keys
        }

        internal fun getKey(index: Int): K {
            return keyIndex.keys.asList().get(index)
        }


        internal abstract fun getValue(index: Int): V


        internal abstract fun setValue(index: Int, newValue: V?): V

        override fun size(): Int {
            return keyIndex.size
        }

        override fun isEmpty(): Boolean {
            return keyIndex.isEmpty()
        }

        internal fun getEntry(index: Int): Entry<K, V> {
            checkElementIndex(index, size)
            return object : AbstractMapEntry<K, V>() {
                override fun getKey(): K? {
                    return this@ArrayMap.getKey(index)
                }

                override fun getValue(): V? {
                    return this@ArrayMap.getValue(index)
                }

                override fun setValue(value: V): V {
                    return this@ArrayMap.setValue(index, value)
                }
            }
        }

        internal override fun entryIterator(): Iterator<Entry<K, V>> {
            return object : AbstractIndexedListIterator<Entry<K, V>>(size) {
                override fun get(index: Int): Entry<K, V> {
                    return getEntry(index)
                }
            }
        }

        internal override fun entrySpliterator(): Spliterator<Entry<K, V>> {
            return CollectSpliterators.indexed<Entry<K, V>>(size, Spliterator.ORDERED, IntFunction<Entry<K, V>> { this.getEntry(it) })
        }

        // TODO(lowasser): consider an optimized values() implementation

        override fun containsKey(key: Any?): Boolean {
            return keyIndex.containsKey(key)
        }

        override operator fun get(key: Any?): V? {
            val index = keyIndex[key]
            return if (index == null) {
                null
            } else {
                getValue(index)
            }
        }

        override fun put(key: K?, value: V?): V {
            val index = keyIndex[key] ?: throw IllegalArgumentException(
                    keyRole + " " + key + " not in " + keyIndex.keys)
            return setValue(index, value)
        }

        override fun remove(key: Any?): V {
            throw UnsupportedOperationException()
        }

        override fun clear() {
            throw UnsupportedOperationException()
        }
    }

    /**
     * Returns, as an immutable list, the row keys provided when the table was constructed, including
     * those that are mapped to null values only.
     */
    fun rowKeyList(): ImmutableList<R> {
        return rowList
    }

    /**
     * Returns, as an immutable list, the column keys provided when the table was constructed,
     * including those that are mapped to null values only.
     */
    fun columnKeyList(): ImmutableList<C> {
        return columnList
    }

    /**
     * Returns the value corresponding to the specified row and column indices. The same value is
     * returned by `get(rowKeyList().get(rowIndex), columnKeyList().get(columnIndex))`, but this
     * method runs more quickly.
     *
     * @param rowIndex position of the row key in [.rowKeyList]
     * @param columnIndex position of the row key in [.columnKeyList]
     * @return the value with the specified row and column
     * @throws IndexOutOfBoundsException if either index is negative, `rowIndex` is greater than
     * or equal to the number of allowed row keys, or `columnIndex` is greater than or equal
     * to the number of allowed column keys
     */
    fun at(rowIndex: Int, columnIndex: Int): V {
        // In GWT array access never throws IndexOutOfBoundsException.
        checkElementIndex(rowIndex, rowList.size)
        checkElementIndex(columnIndex, columnList.size)
        return array[rowIndex][columnIndex]
    }

    /**
     * Associates `value` with the specified row and column indices. The logic `put(rowKeyList().get(rowIndex), columnKeyList().get(columnIndex), value)` has the same
     * behavior, but this method runs more quickly.
     *
     * @param rowIndex position of the row key in [.rowKeyList]
     * @param columnIndex position of the row key in [.columnKeyList]
     * @param value value to store in the table
     * @return the previous value with the specified row and column
     * @throws IndexOutOfBoundsException if either index is negative, `rowIndex` is greater than
     * or equal to the number of allowed row keys, or `columnIndex` is greater than or equal
     * to the number of allowed column keys
     */

    operator fun set(rowIndex: Int, columnIndex: Int, value: V?): V {
        // In GWT array access never throws IndexOutOfBoundsException.
        checkElementIndex(rowIndex, rowList.size)
        checkElementIndex(columnIndex, columnList.size)
        val oldValue = array[rowIndex][columnIndex]
        array[rowIndex][columnIndex] = value
        return oldValue
    }

    /**
     * Returns a two-dimensional array with the table contents. The row and column indices correspond
     * to the positions of the row and column in the iterables provided during table construction. If
     * the table lacks a mapping for a given row and column, the corresponding array element is null.
     *
     *
     * Subsequent table changes will not modify the array, and vice versa.
     *
     * @param valueClass class of values stored in the returned array
     */
    @GwtIncompatible // reflection
    fun toArray(valueClass: Class<V>): Array<Array<V>> {
        val copy = Array.newInstance(valueClass, rowList.size, columnList.size) as Array<Array<V>>// TODO: safe?
        for (i in rowList.indices) {
            System.arraycopy(array[i], 0, copy[i], 0, array[i].size)
        }
        return copy
    }

    /**
     * Not supported. Use [.eraseAll] instead.
     *
     * @throws UnsupportedOperationException always
     */
    @Deprecated("Use {@link #eraseAll}")
    override fun clear() {
        throw UnsupportedOperationException()
    }

    /** Associates the value `null` with every pair of allowed row and column keys.  */
    fun eraseAll() {
        for (row in array) {
            Arrays.fill(row, null)
        }
    }

    /**
     * Returns `true` if the provided keys are among the keys provided when the table was
     * constructed.
     */
    override fun contains(rowKey: Any, columnKey: Any): Boolean {
        return containsRow(rowKey) && containsColumn(columnKey)
    }

    /**
     * Returns `true` if the provided column key is among the column keys provided when the
     * table was constructed.
     */
    override fun containsColumn(columnKey: Any): Boolean {
        return columnKeyToIndex.containsKey(columnKey)
    }

    /**
     * Returns `true` if the provided row key is among the row keys provided when the table was
     * constructed.
     */
    override fun containsRow(rowKey: Any): Boolean {
        return rowKeyToIndex.containsKey(rowKey)
    }

    override fun containsValue(value: Any?): Boolean {
        for (row in array) {
            for (element in row) {
                if (Objects.equal(value, element)) {
                    return true
                }
            }
        }
        return false
    }

    override fun get(rowKey: Any, columnKey: Any): V? {
        val rowIndex = rowKeyToIndex.get(rowKey)
        val columnIndex = columnKeyToIndex.get(columnKey)
        return if (rowIndex == null || columnIndex == null) null else at(rowIndex, columnIndex)
    }

    /**
     * Returns `true` if `rowKeyList().size == 0` or `columnKeyList().size() == 0`.
     */
    override fun isEmpty(): Boolean {
        return rowList.isEmpty() || columnList.isEmpty()
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if `rowKey` is not in [.rowKeySet] or `columnKey` is not in [.columnKeySet].
     */

    override fun put(rowKey: R, columnKey: C, value: V): V {
        checkNotNull(rowKey)
        checkNotNull(columnKey)
        val rowIndex = rowKeyToIndex[rowKey]
        checkArgument(rowIndex != null, "Row %s not in %s", rowKey, rowList)
        val columnIndex = columnKeyToIndex[columnKey]
        checkArgument(columnIndex != null, "Column %s not in %s", columnKey, columnList)
        return set(rowIndex!!, columnIndex!!, value)
    }

    /*
   * TODO(jlevy): Consider creating a merge() method, similar to putAll() but
   * copying non-null values only.
   */

    /**
     * {@inheritDoc}
     *
     *
     * If `table` is an `ArrayTable`, its null values will be stored in this table,
     * possibly replacing values that were previously non-null.
     *
     * @throws NullPointerException if `table` has a null key
     * @throws IllegalArgumentException if any of the provided table's row keys or column keys is not
     * in [.rowKeySet] or [.columnKeySet]
     */
    override fun putAll(table: Table<out R, out C, out V>) {
        super.putAll(table)
    }

    /**
     * Not supported. Use [.erase] instead.
     *
     * @throws UnsupportedOperationException always
     */

    @Deprecated("Use {@link #erase}")
    override fun remove(rowKey: Any, columnKey: Any): V? {
        throw UnsupportedOperationException()
    }

    /**
     * Associates the value `null` with the specified keys, assuming both keys are valid. If
     * either key is null or isn't among the keys provided during construction, this method has no
     * effect.
     *
     *
     * This method is equivalent to `put(rowKey, columnKey, null)` when both provided keys
     * are valid.
     *
     * @param rowKey row key of mapping to be erased
     * @param columnKey column key of mapping to be erased
     * @return the value previously associated with the keys, or `null` if no mapping existed
     * for the keys
     */

    fun erase(rowKey: Any, columnKey: Any): V? {
        val rowIndex = rowKeyToIndex.get(rowKey)
        val columnIndex = columnKeyToIndex.get(columnKey)
        return if (rowIndex == null || columnIndex == null) {
            null
        } else set(rowIndex, columnIndex, null)
    }

    // TODO(jlevy): Add eraseRow and eraseColumn methods?

    override fun size(): Int {
        return rowList.size * columnList.size
    }

    /**
     * Returns an unmodifiable set of all row key / column key / value triplets. Changes to the table
     * will update the returned set.
     *
     *
     * The returned set's iterator traverses the mappings with the first row key, the mappings with
     * the second row key, and so on.
     *
     *
     * The value in the returned cells may change if the table subsequently changes.
     *
     * @return set of table cells consisting of row key / column key / value triplets
     */
    override fun cellSet(): Set<Table.Cell<R, C, V>> {
        return super.cellSet()
    }

    internal override fun cellIterator(): Iterator<Table.Cell<R, C, V>> {
        return object : AbstractIndexedListIterator<Table.Cell<R, C, V>>(size()) {
            override fun get(index: Int): Table.Cell<R, C, V> {
                return getCell(index)
            }
        }
    }

    internal override fun cellSpliterator(): Spliterator<Table.Cell<R, C, V>> {
        return CollectSpliterators.indexed(
                size(), Spliterator.ORDERED or Spliterator.NONNULL or Spliterator.DISTINCT, IntFunction<Table.Cell<R, C, V>> { this.getCell(it) })
    }

    private fun getCell(index: Int): Table.Cell<R, C, V> {
        return object : Tables.AbstractCell<R, C, V>() {
            internal val rowIndex = index / columnList.size
            internal val columnIndex = index % columnList.size

            override fun getRowKey(): R {
                return rowList[rowIndex]
            }

            override fun getColumnKey(): C {
                return columnList[columnIndex]
            }

            override fun getValue(): V {
                return at(rowIndex, columnIndex)
            }
        }
    }

    private fun getValue(index: Int): V {
        val rowIndex = index / columnList.size
        val columnIndex = index % columnList.size
        return at(rowIndex, columnIndex)
    }

    /**
     * Returns a view of all mappings that have the given column key. If the column key isn't in
     * [.columnKeySet], an empty immutable map is returned.
     *
     *
     * Otherwise, for each row key in [.rowKeySet], the returned map associates the row key
     * with the corresponding value in the table. Changes to the returned map will update the
     * underlying table, and vice versa.
     *
     * @param columnKey key of column to search for in the table
     * @return the corresponding map from row keys to values
     */
    override fun column(columnKey: C): Map<R, V> {
        checkNotNull(columnKey)
        val columnIndex = columnKeyToIndex[columnKey]
        return if (columnIndex == null) ImmutableMap.of() else Column(columnIndex)
    }

    private inner class Column internal constructor(internal val columnIndex: Int) : ArrayMap<R, V>(rowKeyToIndex) {

        override val keyRole: String
            get() = "Row"

        override fun getValue(index: Int): V {
            return at(index, columnIndex)
        }

        override fun setValue(index: Int, newValue: V?): V {
            return set(index, columnIndex, newValue)
        }
    }

    /**
     * Returns an immutable set of the valid column keys, including those that are associated with
     * null values only.
     *
     * @return immutable set of column keys
     */
    override fun columnKeySet(): ImmutableSet<C> {
        return columnKeyToIndex.keys
    }

    override fun columnMap(): Map<C, Map<R, V>> {
        val map = columnMap
        return map ?: (columnMap = ColumnMap())
    }

    @WeakOuter
    private inner class ColumnMap private constructor() : ArrayMap<C, Map<R, V>>(columnKeyToIndex) {

        override val keyRole: String
            get() = "Column"

        override fun getValue(index: Int): Map<R, V> {
            return Column(index)
        }

        override fun setValue(index: Int, newValue: Map<R, V>?): Map<R, V> {
            throw UnsupportedOperationException()
        }

        override fun put(key: C?, value: Map<R, V>?): Map<R, V> {
            throw UnsupportedOperationException()
        }
    }

    /**
     * Returns a view of all mappings that have the given row key. If the row key isn't in [ ][.rowKeySet], an empty immutable map is returned.
     *
     *
     * Otherwise, for each column key in [.columnKeySet], the returned map associates the
     * column key with the corresponding value in the table. Changes to the returned map will update
     * the underlying table, and vice versa.
     *
     * @param rowKey key of row to search for in the table
     * @return the corresponding map from column keys to values
     */
    override fun row(rowKey: R): Map<C, V> {
        checkNotNull(rowKey)
        val rowIndex = rowKeyToIndex[rowKey]
        return if (rowIndex == null) ImmutableMap.of() else Row(rowIndex)
    }

    private inner class Row internal constructor(internal val rowIndex: Int) : ArrayMap<C, V>(columnKeyToIndex) {

        override val keyRole: String
            get() = "Column"

        override fun getValue(index: Int): V {
            return at(rowIndex, index)
        }

        override fun setValue(index: Int, newValue: V?): V {
            return set(rowIndex, index, newValue)
        }
    }

    /**
     * Returns an immutable set of the valid row keys, including those that are associated with null
     * values only.
     *
     * @return immutable set of row keys
     */
    override fun rowKeySet(): ImmutableSet<R> {
        return rowKeyToIndex.keys
    }

    override fun rowMap(): Map<R, Map<C, V>> {
        val map = rowMap
        return map ?: (rowMap = RowMap())
    }

    @WeakOuter
    private inner class RowMap private constructor() : ArrayMap<R, Map<C, V>>(rowKeyToIndex) {

        override val keyRole: String
            get() = "Row"

        override fun getValue(index: Int): Map<C, V> {
            return Row(index)
        }

        override fun setValue(index: Int, newValue: Map<C, V>?): Map<C, V> {
            throw UnsupportedOperationException()
        }

        override fun put(key: R?, value: Map<C, V>?): Map<C, V> {
            throw UnsupportedOperationException()
        }
    }

    /**
     * Returns an unmodifiable collection of all values, which may contain duplicates. Changes to the
     * table will update the returned collection.
     *
     *
     * The returned collection's iterator traverses the values of the first row key, the values of
     * the second row key, and so on.
     *
     * @return collection of values
     */
    override fun values(): Collection<V> {
        return super.values()
    }

    internal override fun valuesIterator(): Iterator<V> {
        return object : AbstractIndexedListIterator<V>(size()) {
            override fun get(index: Int): V {
                return getValue(index)
            }
        }
    }

    internal override fun valuesSpliterator(): Spliterator<V> {
        return CollectSpliterators.indexed(size(), Spliterator.ORDERED, IntFunction<V> { this.getValue(it) })
    }

    companion object {

        /**
         * Creates an `ArrayTable` filled with `null`.
         *
         * @param rowKeys row keys that may be stored in the generated table
         * @param columnKeys column keys that may be stored in the generated table
         * @throws NullPointerException if any of the provided keys is null
         * @throws IllegalArgumentException if `rowKeys` or `columnKeys` contains duplicates
         * or if exactly one of `rowKeys` or `columnKeys` is empty.
         */
        fun <R, C, V> create(
                rowKeys: Iterable<R>, columnKeys: Iterable<C>): ArrayTable<R, C, V> {
            return ArrayTable(rowKeys, columnKeys)
        }

        /*
   * TODO(jlevy): Add factory methods taking an Enum class, instead of an
   * iterable, to specify the allowed row keys and/or column keys. Note that
   * custom serialization logic is needed to support different enum sizes during
   * serialization and deserialization.
   */

        /**
         * Creates an `ArrayTable` with the mappings in the provided table.
         *
         *
         * If `table` includes a mapping with row key `r` and a separate mapping with
         * column key `c`, the returned table contains a mapping with row key `r` and column
         * key `c`. If that row key / column key pair in not in `table`, the pair maps to
         * `null` in the generated table.
         *
         *
         * The returned table allows subsequent `put` calls with the row keys in `table.rowKeySet()` and the column keys in `table.columnKeySet()`. Calling [.put]
         * with other keys leads to an `IllegalArgumentException`.
         *
         *
         * The ordering of `table.rowKeySet()` and `table.columnKeySet()` determines the
         * row and column iteration ordering of the returned table.
         *
         * @throws NullPointerException if `table` has a null key
         */
        fun <R, C, V> create(table: Table<R, C, V>): ArrayTable<R, C, V> {
            return if (table is ArrayTable<*, *, *>)
                ArrayTable(table as ArrayTable<R, C, V>)
            else
                ArrayTable(table)
        }

        private const val serialVersionUID: Long = 0
    }
}
