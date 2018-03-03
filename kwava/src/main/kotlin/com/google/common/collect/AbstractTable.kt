/*
 * Copyright (C) 2013 The Guava Authors
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

import com.google.common.annotations.GwtCompatible
import com.google.common.collect.Table.Cell

import com.google.j2objc.annotations.WeakOuter
import java.util.AbstractCollection
import java.util.AbstractSet
import java.util.Spliterator


/**
 * Skeletal, implementation-agnostic implementation of the [Table] interface.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
internal abstract class AbstractTable<R, C, V> : Table<R, C, V> {


    @Transient
    private var cellSet: Set<Cell<R, C, V>>? = null


    @Transient
    private var values: Collection<V>? = null

    override fun containsRow(rowKey: Any): Boolean {
        return Maps.safeContainsKey(rowMap(), rowKey)
    }

    override fun containsColumn(columnKey: Any): Boolean {
        return Maps.safeContainsKey(columnMap(), columnKey)
    }

    override fun rowKeySet(): Set<R> {
        return rowMap().keys
    }

    override fun columnKeySet(): Set<C> {
        return columnMap().keys
    }

    override fun containsValue(value: Any?): Boolean {
        for (row in rowMap().values) {
            if (row.containsValue(value)) {
                return true
            }
        }
        return false
    }

    override fun contains(rowKey: Any, columnKey: Any): Boolean {
        val row = Maps.safeGet(rowMap(), rowKey)
        return row != null && Maps.safeContainsKey(row, columnKey)
    }

    override fun get(rowKey: Any, columnKey: Any): V? {
        val row = Maps.safeGet(rowMap(), rowKey)
        return if (row == null) null else Maps.safeGet(row, columnKey)
    }

    override fun isEmpty(): Boolean {
        return size() == 0
    }

    override fun clear() {
        Iterators.clear(cellSet().iterator())
    }


    override fun remove(rowKey: Any, columnKey: Any): V? {
        val row = Maps.safeGet(rowMap(), rowKey)
        return if (row == null) null else Maps.safeRemove(row, columnKey)
    }


    override fun put(rowKey: R, columnKey: C, value: V): V {
        return row(rowKey).put(columnKey, value)
    }

    override fun putAll(table: Table<out R, out C, out V>) {
        for (cell in table.cellSet()) {
            put(cell.rowKey, cell.columnKey, cell.value)
        }
    }

    override fun cellSet(): Set<Cell<R, C, V>> {
        val result = cellSet
        return result ?: (cellSet = createCellSet())
    }

    internal open fun createCellSet(): Set<Cell<R, C, V>> {
        return CellSet()
    }

    internal abstract fun cellIterator(): Iterator<Table.Cell<R, C, V>>

    internal abstract fun cellSpliterator(): Spliterator<Table.Cell<R, C, V>>

    @WeakOuter
    internal inner class CellSet : AbstractSet<Cell<R, C, V>>() {
        override operator fun contains(o: Any?): Boolean {
            if (o is Cell<*, *, *>) {
                val cell = o as Cell<*, *, *>?
                val row = Maps.safeGet(rowMap(), cell!!.rowKey)
                return row != null && Collections2.safeContains(
                        row.entries, Maps.immutableEntry<*, *>(cell.columnKey, cell.value))
            }
            return false
        }

        override fun remove(o: Any?): Boolean {
            if (o is Cell<*, *, *>) {
                val cell = o as Cell<*, *, *>?
                val row = Maps.safeGet(rowMap(), cell!!.rowKey)
                return row != null && Collections2.safeRemove(
                        row.entries, Maps.immutableEntry<*, *>(cell.columnKey, cell.value))
            }
            return false
        }

        override fun clear() {
            this@AbstractTable.clear()
        }

        override fun iterator(): Iterator<Table.Cell<R, C, V>> {
            return cellIterator()
        }

        override fun spliterator(): Spliterator<Cell<R, C, V>> {
            return cellSpliterator()
        }

        override fun size(): Int {
            return this@AbstractTable.size()
        }
    }

    override fun values(): Collection<V> {
        val result = values
        return result ?: (values = createValues())
    }

    internal open fun createValues(): Collection<V> {
        return Values()
    }

    internal open fun valuesIterator(): Iterator<V> {
        return object : TransformedIterator<Cell<R, C, V>, V>(cellSet().iterator()) {
            internal override fun transform(cell: Cell<R, C, V>): V {
                return cell.value
            }
        }
    }

    internal open fun valuesSpliterator(): Spliterator<V> {
        return CollectSpliterators.map(cellSpliterator(), Function<Cell<R, C, V>, V> { it.getValue() })
    }

    @WeakOuter
    internal inner class Values : AbstractCollection<V>() {
        override fun iterator(): Iterator<V> {
            return valuesIterator()
        }

        override fun spliterator(): Spliterator<V> {
            return valuesSpliterator()
        }

        override operator fun contains(o: Any?): Boolean {
            return containsValue(o)
        }

        override fun clear() {
            this@AbstractTable.clear()
        }

        override fun size(): Int {
            return this@AbstractTable.size()
        }
    }

    override fun equals(obj: Any?): Boolean {
        return Tables.equalsImpl(this, obj)
    }

    override fun hashCode(): Int {
        return cellSet().hashCode()
    }

    /** Returns the string representation `rowMap().toString()`.  */
    override fun toString(): String {
        return rowMap().toString()
    }
}
