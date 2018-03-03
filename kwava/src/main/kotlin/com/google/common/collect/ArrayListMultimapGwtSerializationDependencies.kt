/*
 * Copyright (C) 2016 The Guava Authors
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
 * A dummy superclass to support GWT serialization of the element types of an [ ]. The GWT supersource for this class contains a field for each type.
 *
 *
 * For details about this hack, see [GwtSerializationDependencies], which takes the same
 * approach but with a subclass rather than a superclass.
 *
 *
 * TODO(cpovirk): Consider applying this subclass approach to our other types.
 */
@GwtCompatible(emulated = true)
internal abstract class ArrayListMultimapGwtSerializationDependencies<K, V>(map: MutableMap<K, Collection<V>>) : AbstractListMultimap<K, V>(map)// TODO(cpovirk): Maybe I should have just one shared superclass for AbstractMultimap itself?
