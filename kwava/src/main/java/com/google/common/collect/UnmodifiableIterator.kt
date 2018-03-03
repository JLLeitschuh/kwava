/*
 * Copyright (C) 2008 The Guava Authors
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
 * An iterator that does not support [.remove].
 *
 *
 * `UnmodifiableIterator` is used primarily in conjunction with implementations of [ ], such as [ImmutableList]. You can, however, convert an existing
 * iterator to an `UnmodifiableIterator` using [Iterators.unmodifiableIterator].
 *
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible
abstract class UnmodifiableIterator<E>
/** Constructor for use by subclasses.  */
protected constructor() : Iterator<E>
