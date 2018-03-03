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


/**
 * A map, each entry of which maps a Java [raw type](http://tinyurl.com/2cmwkz) to an
 * instance of that type. In addition to implementing `Map`, the additional type-safe
 * operations [.putInstance] and [.getInstance] are available.
 *
 *
 * Like any other `Map<Class, Object>`, this map may contain entries for primitive types,
 * and a primitive type and its corresponding wrapper type may map to different values.
 *
 *
 * See the Guava User Guide article on [ `ClassToInstanceMap`](https://github.com/google/guava/wiki/NewCollectionTypesExplained#classtoinstancemap).
 *
 *
 * To map a generic type to an instance of that type, use [ ] instead.
 *
 * @param <B> the common supertype that all entries must share; often this is simply [Object]
 * @author Kevin Bourrillion
 * @since 2.0
</B> */
@GwtCompatible
interface ClassToInstanceMap<B> : Map<Class<out B>, B> {
    /**
     * Returns the value the specified class is mapped to, or `null` if no entry for this class
     * is present. This will only return a value that was bound to this specific class, not a value
     * that may have been bound to a subtype.
     */
    // TODO(kak): Consider removing this?
    fun <T : B> getInstance(type: Class<T>): T

    /**
     * Maps the specified class to the specified value. Does *not* associate this value with any
     * of the class's supertypes.
     *
     * @return the value previously associated with this class (possibly `null`), or `null` if there was no previous entry.
     */

    fun <T : B> putInstance(type: Class<T>, value: T): T
}
