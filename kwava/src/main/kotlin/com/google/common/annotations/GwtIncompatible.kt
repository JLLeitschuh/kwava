/*
 * Copyright (C) 2009 The Guava Authors
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

package com.google.common.annotations

/**
 * The presence of this annotation on an API indicates that the method may *not* be used with
 * the [Google Web Toolkit](http://www.gwtproject.org/) (GWT).
 *
 *
 * This annotation behaves identically to [the
 * `@GwtIncompatible` annotation in GWT itself](http://www.gwtproject.org/javadoc/latest/com/google/gwt/core/shared/GwtIncompatible.html).
 *
 * @author Charles Fry
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FIELD)
@MustBeDocumented
@GwtCompatible
annotation class GwtIncompatible(
        /**
         * Describes why the annotated element is incompatible with GWT. Since this is generally due to a
         * dependence on a type/method which GWT doesn't support, it is sufficient to simply reference the
         * unsupported type/method. E.g. "Class.isInstance".
         *
         *
         * As of Guava 20.0, this value is optional. We encourage authors who wish to describe why an
         * API is `@GwtIncompatible` to instead leave an implementation comment.
         */
        val value: String = "")
