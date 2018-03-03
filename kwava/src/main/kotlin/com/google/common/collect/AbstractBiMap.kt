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
import com.google.common.base.Objects
import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkState
import com.google.common.collect.CollectPreconditions.checkRemove
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.function.BiFunction


/**
 * A general-purpose bimap implementation using any two backing `Map` instances.
 *
 *
 * Note that this class contains `equals()` calls that keep it from supporting `IdentityHashMap` backing maps.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 */
@GwtCompatible(emulated = true)
internal abstract class AbstractBiMap<K, V> : ForwardingMap<K, V>, BiMap<K, V>, Serializable {


    @Transient
    private var delegate: MutableMap<K, V>? = null


    @Transient
    var inverse: AbstractBiMap<V, K>? = null


    @Transient
    private var keySet: Set<K>? = null


    @Transient
    private var valueSet: Set<V>? = null


    @Transient
    private var entrySet: Set<Map.Entry<K, V>>? = null

    /** Package-private constructor for creating a map-backed bimap.  */
    constructor(forward: MutableMap<K, V>, backward: MutableMap<V, K>) {
        setDelegates(forward, backward)
    }

    /** Private constructor for inverse bimap.  */
    private constructor(backward: MutableMap<K, V>, forward: AbstractBiMap<V, K>) {
        delegate = backward
        inverse = forward
    }

    override fun delegate(): Map<K, V>? {
        return delegate
    }

    /** Returns its input, or throws an exception if this is not a valid key.  */

    internal open fun checkKey(key: K): K {
        return key
    }

    /** Returns its input, or throws an exception if this is not a valid value.  */

    internal open fun checkValue(value: V): V {
        return value
    }

    /**
     * Specifies the delegate maps going in each direction. Called by the constructor and by
     * subclasses during deserialization.
     */
    fun setDelegates(forward: MutableMap<K, V>, backward: MutableMap<V, K>) {
        checkState(delegate == null)
        checkState(inverse == null)
        checkArgument(forward.isEmpty())
        checkArgument(backward.isEmpty())
        checkArgument(forward !== backward)
        delegate = forward
        inverse = makeInverse(backward)
    }

    fun makeInverse(backward: MutableMap<V, K>): AbstractBiMap<V, K> {
        return Inverse(backward, this)
    }

    // Query Operations (optimizations)

    override fun containsValue(value: V): Boolean {
        return inverse!!.containsKey(value)
    }

    // Modification Operations


    override fun put(key: K, value: V): V {
        return putInBothMaps(key, value, false)
    }


    override fun forcePut(key: K, value: V): V {
        return putInBothMaps(key, value, true)
    }

    private fun putInBothMaps(key: K, value: V, force: Boolean): V {
        checkKey(key)
        checkValue(value)
        val containedKey = containsKey(key)
        if (containedKey && Objects.equal(value, get(key))) {
            return value
        }
        if (force) {
            inverse()!!.remove(value)
        } else {
            checkArgument(!containsValue(value), "value already present: $value")
        }
        val oldValue = delegate!!.put(key, value)!!
        updateInverseMap(key, containedKey, oldValue, value)
        return oldValue
    }

    private fun updateInverseMap(key: K, containedKey: Boolean, oldValue: V, newValue: V) {
        if (containedKey) {
            removeFromInverseMap(oldValue)
        }
        inverse!!.delegate!![newValue] = key
    }


    override fun remove(key: K): V? {
        return if (containsKey(key)) removeFromBothMaps(key) else null
    }


    private fun removeFromBothMaps(key: K): V {
        val oldValue = delegate!!.remove(key)
        removeFromInverseMap(oldValue!!)
        return oldValue
    }

    private fun removeFromInverseMap(oldValue: V) {
        inverse!!.delegate!!.remove(oldValue)
    }

    // Bulk Operations

    override fun putAll(map: Map<out K, V>) {
        for ((key, value) in map) {
            put(key, value)
        }
    }

    override fun replaceAll(function: BiFunction<in K, in V, out V>) {
        (this.delegate as java.util.Map<K, V>).replaceAll(function)
        inverse!!.delegate!!.clear()
        var broken: Entry<K, V>? = null
        val itr = this.delegate!!.entries.iterator()
        while (itr.hasNext()) {
            val entry = itr.next()
            val k = entry.key
            val v = entry.value
            val conflict = (inverse!!.delegate as java.util.Map<V, K>).putIfAbsent(v, k)
            if (conflict != null) {
                broken = entry
                // We're definitely going to throw, but we'll try to keep the BiMap in an internally
                // consistent state by removing the bad entry.
                itr.remove()
            }
        }
        if (broken != null) {
            throw IllegalArgumentException("value already present: " + broken!!.value)
        }
    }

    override fun clear() {
        delegate!!.clear()
        inverse!!.delegate!!.clear()
    }

    // Views

    override fun inverse(): BiMap<V, K>? {
        return inverse
    }

    override fun keySet(): Set<K> {
        val result = keySet
        return result ?: (keySet = KeySet())
    }

    @WeakOuter
    private inner class KeySet : ForwardingSet<K>() {
        override fun delegate(): Set<K> {
            return delegate!!.keys
        }

        override fun clear() {
            this@AbstractBiMap.clear()
        }

        override fun remove(key: Any): Boolean {
            if (!contains(key)) {
                return false
            }
            removeFromBothMaps(key)
            return true
        }

        override fun removeAll(keysToRemove: Collection<*>): Boolean {
            return standardRemoveAll(keysToRemove)
        }

        override fun retainAll(keysToRetain: Collection<*>): Boolean {
            return standardRetainAll(keysToRetain)
        }

        override fun iterator(): Iterator<K> {
            return Maps.keyIterator(entries.iterator())
        }
    }

    override fun values(): Set<V> {
        /*
     * We can almost reuse the inverse's keySet, except we have to fix the
     * iteration order so that it is consistent with the forward map.
     */
        val result = valueSet
        return result ?: (valueSet = ValueSet())
    }

    @WeakOuter
    private inner class ValueSet : ForwardingSet<V>() {
        internal val valuesDelegate = inverse!!.keys

        override fun delegate(): Set<V> {
            return valuesDelegate
        }

        override fun iterator(): Iterator<V> {
            return Maps.valueIterator(entries.iterator())
        }

        override fun toArray(): Array<Any> {
            return standardToArray()
        }

        override fun <T> toArray(array: Array<T>): Array<T> {
            return standardToArray(array)
        }

        override fun toString(): String {
            return standardToString()
        }
    }

    override fun entrySet(): Set<Entry<K, V>> {
        val result = entrySet
        return result ?: (entrySet = EntrySet())
    }

    internal inner class BiMapEntry(private val delegate: Entry<K, V>) : ForwardingMapEntry<K, V>() {

        override fun delegate(): Entry<K, V> {
            return delegate
        }

        override fun setValue(value: V): V {
            checkValue(value)
            // Preconditions keep the map and inverse consistent.
            checkState(entries.contains(this), "entry no longer in map")
            // similar to putInBothMaps, but set via entry
            if (Objects.equal(value, value)) {
                return value
            }
            checkArgument(!containsValue(value), "value already present: %s", value)
            val oldValue = delegate.setValue(value)
            checkState(Objects.equal(value, get(key)), "entry no longer in map")
            updateInverseMap(key, true, oldValue, value)
            return oldValue
        }
    }

    fun entrySetIterator(): Iterator<Entry<K, V>> {
        val iterator = delegate!!.entries.iterator()
        return object : Iterator<Entry<K, V>> {
            internal var entry: Entry<K, V>? = null

            override fun hasNext(): Boolean {
                return iterator.hasNext()
            }

            override fun next(): Entry<K, V> {
                entry = iterator.next()
                return BiMapEntry(entry)
            }

            override fun remove() {
                checkRemove(entry != null)
                val value = entry!!.value
                iterator.remove()
                removeFromInverseMap(value)
                entry = null
            }
        }
    }

    @WeakOuter
    private inner class EntrySet : ForwardingSet<Entry<K, V>>() {
        internal val esDelegate: MutableSet<Entry<K, V>> = delegate!!.entries

        override fun delegate(): Set<Entry<K, V>> {
            return esDelegate
        }

        override fun clear() {
            this@AbstractBiMap.clear()
        }

        override fun remove(`object`: Any): Boolean {
            if (!esDelegate.contains(`object`)) {
                return false
            }

            // safe because esDelegate.contains(object).
            val entry = `object` as Entry<*, *>
            inverse!!.delegate!!.remove(entry.value)
            /*
       * Remove the mapping in inverse before removing from esDelegate because
       * if entry is part of esDelegate, entry might be invalidated after the
       * mapping is removed from esDelegate.
       */
            esDelegate.remove(entry)
            return true
        }

        override fun iterator(): Iterator<Entry<K, V>> {
            return entrySetIterator()
        }

        // See java.util.Collections.CheckedEntrySet for details on attacks.

        override fun toArray(): Array<Any> {
            return standardToArray()
        }

        override fun <T> toArray(array: Array<T>): Array<T> {
            return standardToArray(array)
        }

        override fun contains(o: Any): Boolean {
            return Maps.containsEntryImpl<K, V>(delegate(), o)
        }

        override fun containsAll(c: Collection<*>): Boolean {
            return standardContainsAll(c)
        }

        override fun removeAll(c: Collection<*>): Boolean {
            return standardRemoveAll(c)
        }

        override fun retainAll(c: Collection<*>): Boolean {
            return standardRetainAll(c)
        }
    }

    /** The inverse of any other `AbstractBiMap` subclass.  */
    internal class Inverse<K, V>(backward: MutableMap<K, V>, forward: AbstractBiMap<V, K>) : AbstractBiMap<K, V>(backward, forward) {

        /*
     * Serialization stores the forward bimap, the inverse of this inverse.
     * Deserialization calls inverse() on the forward bimap and returns that
     * inverse.
     *
     * If a bimap and its inverse are serialized together, the deserialized
     * instances have inverse() methods that return the other.
     */

        override fun checkKey(key: K): K {
            return inverse!!.checkValue(key)
        }

        override fun checkValue(value: V): V {
            return inverse!!.checkKey(value)
        }

        /** @serialData the forward bimap
         */
        @GwtIncompatible // java.io.ObjectOutputStream
        @Throws(IOException::class)
        private fun writeObject(stream: ObjectOutputStream) {
            stream.defaultWriteObject()
            stream.writeObject(inverse())
        }

        @GwtIncompatible // java.io.ObjectInputStream
        @Throws(IOException::class, ClassNotFoundException::class)
        private// reading data stored by writeObject
        fun readObject(stream: ObjectInputStream) {
            stream.defaultReadObject()
            inverse = (stream.readObject() as AbstractBiMap<V, K>)
        }

        @GwtIncompatible // Not needed in the emulated source.
        fun readResolve(): Any {
            return inverse()!!.inverse()
        }

        companion object {

            @GwtIncompatible // Not needed in emulated source.
            private val serialVersionUID: Long = 0
        }
    }

    companion object {

        @GwtIncompatible // Not needed in emulated source.
        private const val serialVersionUID: Long = 0
    }
}
