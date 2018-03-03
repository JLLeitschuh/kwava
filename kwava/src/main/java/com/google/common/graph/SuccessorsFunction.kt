/*
 * Copyright (C) 2014 The Guava Authors
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

package com.google.common.graph

import com.google.common.annotations.Beta

/**
 * A functional interface for [graph](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics))-structured data.
 *
 *
 * This interface is meant to be used as the type of a parameter to graph algorithms (such as
 * breadth first traversal) that only need a way of accessing the successors of a node in a graph.
 *
 * <h3>Usage</h3>
 *
 * Given an algorithm, for example:
 *
 * <pre>`public <N> someGraphAlgorithm(N startNode, SuccessorsFunction<N> successorsFunction);
`</pre> *
 *
 * you will invoke it depending on the graph representation you're using.
 *
 *
 * If you have an instance of one of the primary `common.graph` types ([Graph],
 * [ValueGraph], and [Network]):
 *
 * <pre>`someGraphAlgorithm(startNode, graph);
`</pre> *
 *
 * This works because those types each implement `SuccessorsFunction`. It will also work with
 * any other implementation of this interface.
 *
 *
 * If you have your own graph implementation based around a custom node type `MyNode`,
 * which has a method `getChildren()` that retrieves its successors in a graph:
 *
 * <pre>`someGraphAlgorithm(startNode, MyNode::getChildren);
`</pre> *
 *
 *
 * If you have some other mechanism for returning the successors of a node, or one that doesn't
 * return an `Iterable<? extends N>`, then you can use a lambda to perform a more general
 * transformation:
 *
 * <pre>`someGraphAlgorithm(startNode, node -> ImmutableList.of(node.leftChild(), node.rightChild()));
`</pre> *
 *
 *
 * Graph algorithms that need additional capabilities (accessing both predecessors and
 * successors, iterating over the edges, etc.) should declare their input to be of a type that
 * provides those capabilities, such as [Graph], [ValueGraph], or [Network].
 *
 * <h3>Additional documentation</h3>
 *
 *
 * See the Guava User Guide for the `common.graph` package (["Graphs Explained"](https://github.com/google/guava/wiki/GraphsExplained)) for
 * additional documentation, including [notes for
 * implementors](https://github.com/google/guava/wiki/GraphsExplained#notes-for-implementors)
 *
 * @author Joshua O'Madadhain
 * @author Jens Nyman
 * @param <N> Node parameter type
 * @since 23.0
</N> */
@Beta
interface SuccessorsFunction<N> {

    /**
     * Returns all nodes in this graph adjacent to `node` which can be reached by traversing
     * `node`'s outgoing edges in the direction (if any) of the edge.
     *
     *
     * This is *not* the same as "all nodes reachable from `node` by following outgoing
     * edges". For that functionality, see [Graphs.reachableNodes].
     *
     *
     * Some algorithms that operate on a `SuccessorsFunction` may produce undesired results
     * if the returned [Iterable] contains duplicate elements. Implementations of such
     * algorithms should document their behavior in the presence of duplicates.
     *
     *
     * The elements of the returned `Iterable` must each be:
     *
     *
     *  * Non-null
     *  * Usable as `Map` keys (see the Guava User Guide's section on [
 * graph elements](https://github.com/google/guava/wiki/GraphsExplained#graph-elements-nodes-and-edges) for details)
     *
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    fun successors(node: N): Iterable<N>
}
