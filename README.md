# Kwava: Kotlin port of the Google Core Libraries for Java

Kwava is a set of core libraries that includes new collection types (such as
multimap and multiset), immutable collections, a graph library, functional
types, an in-memory cache, and APIs/utilities for concurrency, I/O, hashing,
primitives, reflection, string processing, and much more!

Kwava will come in a few flavors.

*   The JRE flavor requires JDK 1.8 or higher.
*   The Javascript version.
*   The native version.


## Project Status

This is a very early attempt at this project. I don't have any idea if this whole project is even feasable at this point.
I'm looking for people who are interested in helping port some of these classes.

The goal is to allow this port of Guava to be supported on all of the various Kotlin compiler targets.


## IMPORTANT WARNINGS

1. APIs marked with the `@Beta` annotation at the class or method level
are subject to change. They can be modified in any way, or even
removed, at any time. If your code is a library itself (i.e. it is
used on the CLASSPATH of users outside your own control), you should
not use beta APIs, unless you [repackage] them. **If your
code is a library, we strongly recommend using the [Guava Beta Checker] to
ensure that you do not use any `@Beta` APIs!**

2. APIs without `@Beta` will remain binary-compatible for the indefinite
future. (Previously, we sometimes removed such APIs after a deprecation period.
The last release to remove non-`@Beta` APIs was Guava 21.0.) Even `@Deprecated`
APIs will remain (again, unless they are `@Beta`). We have no plans to start
removing things again, but officially, we're leaving our options open in case
of surprises (like, say, a serious security problem).

3. Serialized forms of ALL objects are subject to change unless noted
otherwise. Do not persist these and assume they can be read by a
future version of the library.

4. Our classes are not designed to protect against a malicious caller.
You should not use them for communication between trusted and
untrusted code.

5. For the mainline flavor, we unit-test the libraries using only OpenJDK 1.8 on
Linux. Some features, especially in `com.google.common.io`, may not work
correctly in other environments. For the Android flavor, our unit tests run on
API level 15 (Ice Cream Sandwich).

[current release]: https://github.com/google/guava/releases/tag/v24.0
[guava-snapshot-api-docs]: http://google.github.io/guava/releases/snapshot-jre/api/docs/
[guava-snapshot-api-diffs]: http://google.github.io/guava/releases/snapshot-jre/api/diffs/
[Guava Explained]: https://github.com/google/guava/wiki/Home
[Guava Beta Checker]: https://github.com/google/guava-beta-checker
