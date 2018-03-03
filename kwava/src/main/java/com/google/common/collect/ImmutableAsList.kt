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
import com.google.common.annotations.GwtIncompatible
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.Serializable

/**
 * List returned by [ImmutableCollection.asList] that delegates `contains` checks to the
 * backing collection.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 */
@GwtCompatible(serializable = true, emulated = true)
internal abstract class ImmutableAsList<E> : ImmutableList<E>() {

    override val isPartialView: Boolean
        get() = delegateCollection().isPartialView

    internal abstract fun delegateCollection(): ImmutableCollection<E>

    override operator fun contains(element: E): Boolean {
        // The collection's contains() is at least as fast as ImmutableList's
        // and is often faster.
        return delegateCollection().contains(element)
    }

    override val size: Int
        get() = delegateCollection().size

    override fun isEmpty(): Boolean {
        return delegateCollection().isEmpty()
    }

    /** Serialized form that leads to the same performance as the original list.  */
    @GwtIncompatible // serialization
    internal class SerializedForm(val collection: ImmutableCollection<*>) : Serializable {

        fun readResolve(): Any {
            return collection.asList()
        }

        companion object {

            private const val serialVersionUID: Long = 0
        }
    }

    @GwtIncompatible // serialization
    @Throws(InvalidObjectException::class)
    private fun readObject(stream: ObjectInputStream) {
        throw InvalidObjectException("Use SerializedForm")
    }

    @GwtIncompatible // serialization
    internal override fun writeReplace(): Any {
        return SerializedForm(delegateCollection())
    }
}
