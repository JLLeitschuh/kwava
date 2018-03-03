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

package com.google.common.graph

import com.google.common.annotations.Beta
import java.util.Optional


/**
 * An interface for [graph](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics))-structured data,
 * whose edges have associated non-unique values.
 *
 *
 * A graph is composed of a set of nodes and a set of edges connecting pairs of nodes.
 *
 *
 * There are three primary interfaces provided to represent graphs. In order of increasing
 * complexity they are: [Graph], [ValueGraph], and [Network]. You should generally
 * prefer the simplest interface that satisfies your use case. See the [
 * "Choosing the right graph type"](https://github.com/google/guava/wiki/GraphsExplained#choosing-the-right-graph-type) section of the Guava User Guide for more details.
 *
 * <h3>Capabilities</h3>
 *
 *
 * `ValueGraph` supports the following use cases ([definitions of
 * terms](https://github.com/google/guava/wiki/GraphsExplained#definitions)):
 *
 *
 *  * directed graphs
 *  * undirected graphs
 *  * graphs that do/don't allow self-loops
 *  * graphs whose nodes/edges are insertion-ordered, sorted, or unordered
 *  * graphs whose edges have associated values
 *
 *
 *
 * `ValueGraph`, as a subtype of `Graph`, explicitly does not support parallel edges,
 * and forbids implementations or extensions with parallel edges. If you need parallel edges, use
 * [Network]. (You can use a positive `Integer` edge value as a loose representation of
 * edge multiplicity, but the `*degree()` and mutation methods will not reflect your
 * interpretation of the edge value as its multiplicity.)
 *
 * <h3>Building a `ValueGraph`</h3>
 *
 *
 * The implementation classes that `common.graph` provides are not public, by design. To
 * create an instance of one of the built-in implementations of `ValueGraph`, use the [ ] class:
 *
 * <pre>`MutableValueGraph<Integer, Double> graph = ValueGraphBuilder.directed().build();
`</pre> *
 *
 *
 * [ValueGraphBuilder.build] returns an instance of [MutableValueGraph], which is a
 * subtype of `ValueGraph` that provides methods for adding and removing nodes and edges. If
 * you do not need to mutate a graph (e.g. if you write a method than runs a read-only algorithm on
 * the graph), you should use the non-mutating [ValueGraph] interface, or an [ ].
 *
 *
 * You can create an immutable copy of an existing `ValueGraph` using [ ][ImmutableValueGraph.copyOf]:
 *
 * <pre>`ImmutableValueGraph<Integer, Double> immutableGraph = ImmutableValueGraph.copyOf(graph);
`</pre> *
 *
 *
 * Instances of [ImmutableValueGraph] do not implement [MutableValueGraph]
 * (obviously!) and are contractually guaranteed to be unmodifiable and thread-safe.
 *
 *
 * The Guava User Guide has [more
 * information on (and examples of) building graphs](https://github.com/google/guava/wiki/GraphsExplained#building-graph-instances).
 *
 * <h3>Additional documentation</h3>
 *
 *
 * See the Guava User Guide for the `common.graph` package (["Graphs Explained"](https://github.com/google/guava/wiki/GraphsExplained)) for
 * additional documentation, including:
 *
 *
 *  * [
 * `equals()`, `hashCode()`, and graph equivalence](https://github.com/google/guava/wiki/GraphsExplained#equals-hashcode-and-graph-equivalence)
 *  * [
 * Synchronization policy](https://github.com/google/guava/wiki/GraphsExplained#synchronization)
 *  * [Notes
 * for implementors](https://github.com/google/guava/wiki/GraphsExplained#notes-for-implementors)
 *
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
</V></N> */
@Beta
interface ValueGraph<N, V> : BaseGraph<N> {

    //
    // ValueGraph properties
    //

    /**
     * Returns true if the edges in this graph are directed. Directed edges connect a [ ][EndpointPair.source] to a [target node][EndpointPair.target], while
     * undirected edges connect a pair of nodes to each other.
     */
    override val isDirected: Boolean
    //
    // ValueGraph-level accessors
    //

    /** Returns all nodes in this graph, in the order specified by [.nodeOrder].  */
    override fun nodes(): Set<N>

    /** Returns all edges in this graph.  */
    override fun edges(): Set<EndpointPair<N>>

    /**
     * Returns a live view of this graph as a [Graph]. The resulting [Graph] will have an
     * edge connecting node A to node B if this [ValueGraph] has an edge connecting A to B.
     */
    fun asGraph(): Graph<N>

    /**
     * Returns true if this graph allows self-loops (edges that connect a node to itself). Attempting
     * to add a self-loop to a graph that does not allow them will throw an [ ].
     */
    override fun allowsSelfLoops(): Boolean

    /** Returns the order of iteration for the elements of [.nodes].  */
    override fun nodeOrder(): ElementOrder<N>

    //
    // Element-level accessors
    //

    /**
     * Returns the nodes which have an incident edge in common with `node` in this graph.
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    override fun adjacentNodes(node: N): Set<N>

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
    override fun incidentEdges(node: N): Set<EndpointPair<N>>

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
    override fun degree(node: N): Int

    /**
     * Returns the count of `node`'s incoming edges (equal to `predecessors(node).size()`)
     * in a directed graph. In an undirected graph, returns the [.degree].
     *
     *
     * If the count is greater than `Integer.MAX_VALUE`, returns `Integer.MAX_VALUE`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    override fun inDegree(node: N): Int

    /**
     * Returns the count of `node`'s outgoing edges (equal to `successors(node).size()`)
     * in a directed graph. In an undirected graph, returns the [.degree].
     *
     *
     * If the count is greater than `Integer.MAX_VALUE`, returns `Integer.MAX_VALUE`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this graph
     */
    override fun outDegree(node: N): Int

    /**
     * Returns true if there is an edge directly connecting `nodeU` to `nodeV`. This is
     * equivalent to `nodes().contains(nodeU) && successors(nodeU).contains(nodeV)`.
     *
     *
     * In an undirected graph, this is equal to `hasEdgeConnecting(nodeV, nodeU)`.
     *
     * @since 23.0
     */
    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean

    /**
     * Returns the value of the edge connecting `nodeU` to `nodeV`, if one is present;
     * otherwise, returns `Optional.empty()`.
     *
     *
     * In an undirected graph, this is equal to `edgeValue(nodeV, nodeU)`.
     *
     * @throws IllegalArgumentException if `nodeU` or `nodeV` is not an element of this
     * graph
     * @since 23.0 (since 20.0 with return type `V`)
     */
    fun edgeValue(nodeU: N, nodeV: N): Optional<V>

    /**
     * Returns the value of the edge connecting `nodeU` to `nodeV`, if one is present;
     * otherwise, returns `defaultValue`.
     *
     *
     * In an undirected graph, this is equal to `edgeValueOrDefault(nodeV, nodeU,
     * defaultValue)`.
     *
     * @throws IllegalArgumentException if `nodeU` or `nodeV` is not an element of this
     * graph
     */

    fun edgeValueOrDefault(nodeU: N, nodeV: N, defaultValue: V): V

    //
    // ValueGraph identity
    //

    /**
     * Returns `true` iff `object` is a [ValueGraph] that has the same elements and
     * the same structural relationships as those in this graph.
     *
     *
     * Thus, two value graphs A and B are equal if **all** of the following are true:
     *
     *
     *  * A and B have equal [directedness][.isDirected].
     *  * A and B have equal [node sets][.nodes].
     *  * A and B have equal [edge sets][.edges].
     *  * The [value][.edgeValue] of a given edge is the same in both A and B.
     *
     *
     *
     * Graph properties besides [directedness][.isDirected] do **not** affect equality.
     * For example, two graphs may be considered equal even if one allows self-loops and the other
     * doesn't. Additionally, the order in which nodes or edges are added to the graph, and the order
     * in which they are iterated over, are irrelevant.
     *
     *
     * A reference implementation of this is provided by [AbstractValueGraph.equals].
     */
    override fun equals(`object`: Any?): Boolean

    /**
     * Returns the hash code for this graph. The hash code of a graph is defined as the hash code of a
     * map from each of its [edges][.edges] to the associated [ edge value][.edgeValue].
     *
     *
     * A reference implementation of this is provided by [AbstractValueGraph.hashCode].
     */
    override fun hashCode(): Int
}
