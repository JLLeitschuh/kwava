/*
 * Copyright (C) 2017 The Guava Authors
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

/**
 * A non-public interface for the methods shared between [Graph] and [ValueGraph].
 *
 * @author James Sexton
 * @param <N> Node parameter type
</N> */
internal interface BaseGraph<N> : SuccessorsFunction<N>, PredecessorsFunction<N> {

    //
    // Graph properties
    //

    /**
     * Returns true if the edges in this graph are directed. Directed edges connect a [ ][EndpointPair.source] to a [target node][EndpointPair.target], while
     * undirected edges connect a pair of nodes to each other.
     */
    val isDirected: Boolean
    //
    // Graph-level accessors
    //

    /** Returns all nodes in this graph, in the order specified by [.nodeOrder].  */
    fun nodes(): Set<N>

    /** Returns all edges in this graph.  */
    fun edges(): Set<EndpointPair<N>>

    /**
     * Returns true if this graph allows self-loops (edges that connect a node to itself). Attempting
     * to add a self-loop to a graph that does not allow them will throw an [ ].
     */
    fun allowsSelfLoops(): Boolean

    /** Returns the order of iteration for the elements of [.nodes].  */
    fun nodeOrder(): ElementOrder<N>

    //
    // Element-level accessors
    //

    /**
     * Returns the nodes which have an incident edge in common with `node` in this graph.
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    fun adjacentNodes(node: N): Set<N>

    /**
     * Returns all nodes in this graph adjacent to `node` which can be reached by traversing
     * `node`'s incoming edges *against* the direction (if any) of the edge.
     *
     *
     * In an undirected graph, this is equivalent to [.adjacentNodes].
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    override fun predecessors(node: N): Set<N>

    /**
     * Returns all nodes in this graph adjacent to `node` which can be reached by traversing
     * `node`'s outgoing edges in the direction (if any) of the edge.
     *
     *
     * In an undirected graph, this is equivalent to [.adjacentNodes].
     *
     *
     * This is *not* the same as "all nodes reachable from `node` by following outgoing
     * edges". For that functionality, see [Graphs.reachableNodes].
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    override fun successors(node: N): Set<N>

    /**
     * Returns the edges in this graph whose endpoints include `node`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     * @since 24.0
     */
    fun incidentEdges(node: N): Set<EndpointPair<N>>

    /**
     * Returns the count of `node`'s incident edges, counting self-loops twice (equivalently,
     * the number of times an edge touches `node`).
     *
     *
     * For directed graphs, this is equal to `inDegree(node) + outDegree(node)`.
     *
     *
     * For undirected graphs, this is equal to `incidentEdges(node).size()` + (number of
     * self-loops incident to `node`).
     *
     *
     * If the count is greater than `Integer.MAX_VALUE`, returns `Integer.MAX_VALUE`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    fun degree(node: N): Int

    /**
     * Returns the count of `node`'s incoming edges (equal to `predecessors(node).size()`)
     * in a directed graph. In an undirected graph, returns the [.degree].
     *
     *
     * If the count is greater than `Integer.MAX_VALUE`, returns `Integer.MAX_VALUE`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    fun inDegree(node: N): Int

    /**
     * Returns the count of `node`'s outgoing edges (equal to `successors(node).size()`)
     * in a directed graph. In an undirected graph, returns the [.degree].
     *
     *
     * If the count is greater than `Integer.MAX_VALUE`, returns `Integer.MAX_VALUE`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    fun outDegree(node: N): Int

    /**
     * Returns true if there is an edge directly connecting `nodeU` to `nodeV`. This is
     * equivalent to `nodes().contains(nodeU) && successors(nodeU).contains(nodeV)`.
     *
     *
     * In an undirected graph, this is equal to `hasEdgeConnecting(nodeV, nodeU)`.
     *
     * @since 23.0
     */
    fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean
}
