/*
 * Copyright (C) 2012 The Guava Authors
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
import java.util.function.Consumer

/**
 * An [ImmutableAsList] implementation specialized for when the delegate collection is already
 * backed by an `ImmutableList` or array.
 *
 * @author Louis Wasserman
 */
@GwtCompatible(emulated = true)
internal open // uses writeReplace, not default serialization
class RegularImmutableAsList<E>
constructor(
        private val delegate: ImmutableCollection<E>,
        private val delegateList: ImmutableList<out E>) : ImmutableAsList<E>() {

    constructor(delegate: ImmutableCollection<E>, array: Array<Any?>) :
            this(delegate, ImmutableList.asImmutableList<E>(array))

    override fun delegateCollection(): ImmutableCollection<E> {
        return delegate
    }

    fun delegateList(): ImmutableList<out E> {
        return delegateList
    }

    override// safe covariant cast!
    fun listIterator(index: Int): UnmodifiableListIterator<E> {
        return delegateList.listIterator(index) as UnmodifiableListIterator<E>
    }

    @GwtIncompatible // not present in emulated superclass
    override fun forEach(action: Consumer<in E>) {
        delegateList.forEach(action)
    }

    @GwtIncompatible // not present in emulated superclass
    override fun copyIntoArray(dst: Array<Any?>, offset: Int): Int {
        return delegateList.copyIntoArray(dst, offset)
    }

    override fun get(index: Int): E {
        return delegateList[index]
    }
}
