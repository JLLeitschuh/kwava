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

import com.google.common.annotations.GwtCompatible
import java.io.Serializable
import java.util.*
import java.util.function.Consumer

/**
 * Implementation of [ImmutableSet] backed by a non-empty [java.util.EnumSet].
 *
 * @author Jared Levy
 */
@GwtCompatible(serializable = true, emulated = true)
internal// we're overriding default serialization
class ImmutableEnumSet<E : Enum<E>>
private constructor(
        /*
          * Notes on EnumSet and <E extends Enum<E>>:
          *
          * This class isn't an arbitrary ForwardingImmutableSet because we need to
          * know that calling {@code clone()} during deserialization will return an
          * object that no one else has a reference to, allowing us to guarantee
          * immutability. Hence, we support only {@link EnumSet}.
          */
        @field:Transient private val delegate: EnumSet<E>) : ImmutableSet<E>() {

    @Transient
    private var hashCode: Int = 0

    internal override fun isPartialView(): Boolean {
        return false
    }

    override fun iterator(): UnmodifiableIterator<E> {
        return Iterators.unmodifiableIterator(delegate.iterator())
    }

    override fun spliterator(): Spliterator<E> {
        return delegate.spliterator()
    }

    override fun forEach(action: Consumer<in E>) {
        delegate.forEach(action)
    }

    override val size: Int
        get() = delegate.size

    override operator fun contains(element: E): Boolean {
        return delegate.contains(element)
    }

    override fun containsAll(collection: Collection<E>): Boolean {
        var collection = collection
        if (collection is ImmutableEnumSet<E>) {
            collection = collection.delegate
        }
        return delegate.containsAll(collection)
    }

    override fun isEmpty(): Boolean {
        return delegate.isEmpty()
    }

    override fun equals(`object`: Any?): Boolean {
        var `object` = `object`
        if (`object` === this) {
            return true
        }
        if (`object` is ImmutableEnumSet<*>) {
            `object` = `object`.delegate
        }
        return delegate == `object`
    }

    internal override fun isHashCodeFast(): Boolean {
        return true
    }

    override fun hashCode(): Int {
        val result = hashCode
        return if (result == 0) {
            hashCode = delegate.hashCode()
            hashCode
        } else result
    }

    override fun toString(): String {
        return delegate.toString()
    }

    // All callers of the constructor are restricted to <E extends Enum<E>>.
    internal override fun writeReplace(): Any {
        return EnumSerializedForm(delegate)
    }

    /*
   * This class is used to serialize ImmutableEnumSet instances.
   */
    private class EnumSerializedForm<E : Enum<E>> internal constructor(internal val delegate: EnumSet<E>) : Serializable {

        internal fun readResolve(): Any {
            // EJ2 #76: Write readObject() methods defensively.
            return ImmutableEnumSet(delegate.clone())
        }

        companion object {

            private const val serialVersionUID: Long = 0
        }
    }

    companion object {
        @JvmStatic
        fun asImmutable(set: EnumSet<*>): ImmutableSet<*> {
            return when (set.size) {
                0 -> ImmutableSet.of<Any>()
                1 -> ImmutableSet.of(Iterables.getOnlyElement<Any>(set))
                else -> ImmutableEnumSet(set)
            }
        }
    }
}
