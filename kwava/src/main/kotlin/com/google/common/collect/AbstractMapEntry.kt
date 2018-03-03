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
import com.google.common.base.Objects
import kotlin.collections.Map.Entry


/**
 * Implementation of the `equals`, `hashCode`, and `toString` methods of `Entry`.
 *
 * @author Jared Levy
 */
@GwtCompatible
internal abstract class AbstractMapEntry<K, V> : Entry<K, V> {

    abstract override fun getKey(): K?

    abstract override fun getValue(): V?

    override fun setValue(value: V): V {
        throw UnsupportedOperationException()
    }

    override fun equals(`object`: Any?): Boolean {
        if (`object` is Entry<*, *>) {
            val that = `object` as Entry<*, *>?
            return Objects.equal(this.key, that!!.key) && Objects.equal(this.value, that.value)
        }
        return false
    }

    override fun hashCode(): Int {
        val k = key
        val v = value
        return (k?.hashCode() ?: 0) xor (v?.hashCode() ?: 0)
    }

    /** Returns a string representation of the form `{key}={value}`.  */
    override fun toString(): String {
        return key.toString() + "=" + value
    }
}
