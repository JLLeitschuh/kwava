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

import com.google.common.annotations.Beta
import com.google.common.annotations.GwtCompatible
import com.google.common.base.Objects


/**
 * A map which forwards all its method calls to another map. Subclasses should override one or more
 * methods to modify the behavior of the backing map as desired per the [decorator pattern](http://en.wikipedia.org/wiki/Decorator_pattern).
 *
 *
 * **Warning:** The methods of `ForwardingMap` forward *indiscriminately* to the
 * methods of the delegate. For example, overriding [.put] alone *will not* change the
 * behavior of [.putAll], which can lead to unexpected behavior. In this case, you should
 * override `putAll` as well, either providing your own implementation, or delegating to the
 * provided `standardPutAll` method.
 *
 *
 * **`default` method warning:** This class does *not* forward calls to `default` methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the `ForwardingMap`.
 *
 *
 * Each of the `standard` methods, where appropriate, use [Objects.equal] to test
 * equality for both keys and values. This may not be the desired behavior for map implementations
 * that use non-standard notions of key equality, such as a `SortedMap` whose comparator is
 * not consistent with `equals`.
 *
 *
 * The `standard` methods and the collection views they return are not guaranteed to be
 * thread-safe, even when all of the methods that they depend on are thread-safe.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible
abstract class ForwardingMap<K, V>
// TODO(lowasser): identify places where thread safety is actually lost

/** Constructor for use by subclasses.  */
protected constructor() : ForwardingObject(), MutableMap<K, V> {

    abstract override fun delegate(): MutableMap<K, V>

    override val size: Int
        get() = delegate().size

    override fun isEmpty(): Boolean {
        return delegate().isEmpty()
    }


    override fun remove(key: K): V? {
        return delegate().remove(key)
    }

    override fun clear() {
        delegate().clear()
    }

    override fun containsKey(key: K): Boolean {
        return delegate().containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        return delegate().containsValue(value)
    }

    override operator fun get(key: K): V? {
        return delegate().get(key)
    }


    override fun put(key: K, value: V): V? {
        return delegate().put(key, value)
    }

    override fun putAll(map: Map<out K, V>) {
        delegate().putAll(map)
    }

    override val keys: MutableSet<K>
        get() = delegate().keys

    override val values: MutableCollection<V>
        get() = delegate().values


    override fun entrySet(): MutableSet<MutableMap.MutableEntry<K, V>> {
        return delegate().entries
    }

    override fun equals(other: Any?): Boolean {
        return other === this || delegate() == other
    }

    override fun hashCode(): Int {
        return delegate().hashCode()
    }

    /**
     * A sensible definition of [.putAll] in terms of [.put]. If you
     * override [.put], you may wish to override [.putAll] to forward
     * to this implementation.
     *
     * @since 7.0
     */
    protected fun standardPutAll(map: Map<out K, V>) {
        Maps.putAllImpl(this, map)
    }

    /**
     * A sensible, albeit inefficient, definition of [.remove] in terms of the `iterator`
     * method of [.entrySet]. If you override [.entrySet], you may wish to override [ ][.remove] to forward to this implementation.
     *
     *
     * Alternately, you may wish to override [.remove] with `keySet().remove`, assuming
     * that approach would not lead to an infinite loop.
     *
     * @since 7.0
     */
    @Beta
    protected fun standardRemove(key: Any): V? {
        val entryIterator = entries.iterator()
        while (entryIterator.hasNext()) {
            val entry = entryIterator.next()
            if (Objects.equal(entry.key, key)) {
                val value = entry.value
                entryIterator.remove()
                return value
            }
        }
        return null
    }

    /**
     * A sensible definition of [.clear] in terms of the `iterator` method of [ ][.entrySet]. In many cases, you may wish to override [.clear] to forward to this
     * implementation.
     *
     * @since 7.0
     */
    protected fun standardClear() {
        Iterators.clear(entries.iterator())
    }

    /**
     * A sensible implementation of [Map.keySet] in terms of the following methods: [ ][ForwardingMap.clear], [ForwardingMap.containsKey], [ForwardingMap.isEmpty], [ ][ForwardingMap.remove], [ForwardingMap.size], and the [Set.iterator] method of
     * [ForwardingMap.entrySet]. In many cases, you may wish to override [ ][ForwardingMap.keySet] to forward to this implementation or a subclass thereof.
     *
     * @since 10.0
     */
    /** Constructor for use by subclasses.  */
    @Beta
    protected inner class StandardKeySet : Maps.KeySet<K, V>(this@ForwardingMap)

    /**
     * A sensible, albeit inefficient, definition of [.containsKey] in terms of the `iterator` method of [.entrySet]. If you override [.entrySet], you may wish to
     * override [.containsKey] to forward to this implementation.
     *
     * @since 7.0
     */
    @Beta
    protected open fun standardContainsKey(key: Any): Boolean {
        return Maps.containsKeyImpl(this, key)
    }

    /**
     * A sensible implementation of [Map.values] in terms of the following methods: [ ][ForwardingMap.clear], [ForwardingMap.containsValue], [ForwardingMap.isEmpty],
     * [ForwardingMap.size], and the [Set.iterator] method of [ ][ForwardingMap.entrySet]. In many cases, you may wish to override [ForwardingMap.values]
     * to forward to this implementation or a subclass thereof.
     *
     * @since 10.0
     */
    /** Constructor for use by subclasses.  */
    @Beta
    internal inner class StandardValues : Maps.Values<K, V>(this@ForwardingMap)

    /**
     * A sensible definition of [.containsValue] in terms of the `iterator` method of
     * [.entrySet]. If you override [.entrySet], you may wish to override [ ][.containsValue] to forward to this implementation.
     *
     * @since 7.0
     */
    protected fun standardContainsValue(value: Any): Boolean {
        return Maps.containsValueImpl(this, value)
    }

    /**
     * A sensible implementation of [Map.entrySet] in terms of the following methods: [ ][ForwardingMap.clear], [ForwardingMap.containsKey], [ForwardingMap.get], [ ][ForwardingMap.isEmpty], [ForwardingMap.remove], and [ForwardingMap.size]. In many
     * cases, you may wish to override [.entrySet] to forward to this implementation or a
     * subclass thereof.
     *
     * @since 10.0
     */
    /** Constructor for use by subclasses.  */
    @Beta
    protected abstract inner class StandardEntrySet : Maps.EntrySet<K, V>() {

        internal override fun map(): Map<K, V> {
            return this@ForwardingMap
        }
    }

    /**
     * A sensible definition of [.isEmpty] in terms of the `iterator` method of [ ][.entrySet]. If you override [.entrySet], you may wish to override [.isEmpty] to
     * forward to this implementation.
     *
     * @since 7.0
     */
    protected fun standardIsEmpty(): Boolean {
        return !entries.iterator().hasNext()
    }

    /**
     * A sensible definition of [.equals] in terms of the `equals` method of [ ][.entrySet]. If you override [.entrySet], you may wish to override [.equals] to
     * forward to this implementation.
     *
     * @since 7.0
     */
    protected fun standardEquals(`object`: Any): Boolean {
        return Maps.equalsImpl(this, `object`)
    }

    /**
     * A sensible definition of [.hashCode] in terms of the `iterator` method of [ ][.entrySet]. If you override [.entrySet], you may wish to override [.hashCode] to
     * forward to this implementation.
     *
     * @since 7.0
     */
    protected fun standardHashCode(): Int {
        return Sets.hashCodeImpl(entries)
    }

    /**
     * A sensible definition of [.toString] in terms of the `iterator` method of [ ][.entrySet]. If you override [.entrySet], you may wish to override [.toString] to
     * forward to this implementation.
     *
     * @since 7.0
     */
    protected fun standardToString(): String {
        return Maps.toStringImpl(this)
    }
}
