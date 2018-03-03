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
 * The presence of this annotation on a type indicates that the type may be used with the [Google Web Toolkit](http://code.google.com/webtoolkit/) (GWT). When applied to a method,
 * the return type of the method is GWT compatible. It's useful to indicate that an instance created
 * by factory methods has a GWT serializable type. In the following example,
 *
 * <pre>
 * @GwtCompatible
 * class Lists {
 * ...
 * @GwtCompatible(serializable = true)
 * static &lt;E&gt; List&lt;E&gt; newArrayList(E... elements) {
 * ...
 * }
 * }
</pre> *
 *
 *
 * The return value of `Lists.newArrayList(E[])` has GWT serializable type. It is also
 * useful in specifying contracts of interface methods. In the following example,
 *
 * <pre>
 * @GwtCompatible
 * interface ListFactory {
 * ...
 * @GwtCompatible(serializable = true)
 * &lt;E&gt; List&lt;E&gt; newArrayList(E... elements);
 * }
</pre> *
 *
 *
 * The `newArrayList(E[])` method of all implementations of `ListFactory` is expected
 * to return a value with a GWT serializable type.
 *
 *
 * Note that a `GwtCompatible` type may have some [GwtIncompatible] methods.
 *
 *
 * @author Charles Fry
 * @author Hayward Chan
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@MustBeDocumented
@GwtCompatible
annotation class GwtCompatible(
        /**
         * When `true`, the annotated type or the type of the method return value is GWT
         * serializable.
         *
         * @see [
         * Documentation about GWT serialization](http://code.google.com/webtoolkit/doc/latest/DevGuideServerCommunication.html.DevGuideSerializableTypes)
         */
        val serializable: Boolean = false,
        /**
         * When `true`, the annotated type is emulated in GWT. The emulated source (also known as
         * super-source) is different from the implementation used by the JVM.
         *
         * @see [
         * Documentation about GWT emulated source](http://code.google.com/webtoolkit/doc/latest/DevGuideOrganizingProjects.html.DevGuideModules)
         */
        val emulated: Boolean = false)
