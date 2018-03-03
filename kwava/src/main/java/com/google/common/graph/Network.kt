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
import java.util.Optional


/**
 * An interface for [graph](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics))-structured data,
 * whose edges are unique objects.
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
 * `Network` supports the following use cases ([definitions of
 * terms](https://github.com/google/guava/wiki/GraphsExplained#definitions)):
 *
 *
 *  * directed graphs
 *  * undirected graphs
 *  * graphs that do/don't allow parallel edges
 *  * graphs that do/don't allow self-loops
 *  * graphs whose nodes/edges are insertion-ordered, sorted, or unordered
 *  * graphs whose edges are unique objects
 *
 *
 * <h3>Building a `Network`</h3>
 *
 *
 * The implementation classes that `common.graph` provides are not public, by design. To
 * create an instance of one of the built-in implementations of `Network`, use the [ ] class:
 *
 * <pre>`MutableNetwork<Integer, MyEdge> graph = NetworkBuilder.directed().build();
`</pre> *
 *
 *
 * [NetworkBuilder.build] returns an instance of [MutableNetwork], which is a
 * subtype of `Network` that provides methods for adding and removing nodes and edges. If you
 * do not need to mutate a graph (e.g. if you write a method than runs a read-only algorithm on the
 * graph), you should use the non-mutating [Network] interface, or an [ ].
 *
 *
 * You can create an immutable copy of an existing `Network` using [ ][ImmutableNetwork.copyOf]:
 *
 * <pre>`ImmutableNetwork<Integer, MyEdge> immutableGraph = ImmutableNetwork.copyOf(graph);
`</pre> *
 *
 *
 * Instances of [ImmutableNetwork] do not implement [MutableNetwork] (obviously!) and
 * are contractually guaranteed to be unmodifiable and thread-safe.
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
 * @param <E> Edge parameter type
 * @since 20.0
</E></N> */
@Beta
interface Network<N, E> : SuccessorsFunction<N>, PredecessorsFunction<N> {

    //
    // Network properties
    //

    /**
     * Returns true if the edges in this network are directed. Directed edges connect a [ ][EndpointPair.source] to a [target node][EndpointPair.target], while
     * undirected edges connect a pair of nodes to each other.
     */
    val isDirected: Boolean
    //
    // Network-level accessors
    //

    /** Returns all nodes in this network, in the order specified by [.nodeOrder].  */
    fun nodes(): Set<N>

    /** Returns all edges in this network, in the order specified by [.edgeOrder].  */
    fun edges(): Set<E>

    /**
     * Returns a live view of this network as a [Graph]. The resulting [Graph] will have
     * an edge connecting node A to node B if this [Network] has an edge connecting A to B.
     *
     *
     * If this network [allows parallel edges][.allowsParallelEdges], parallel edges will be
     * treated as if collapsed into a single edge. For example, the [.degree] of a node
     * in the [Graph] view may be less than the degree of the same node in this [Network].
     */
    fun asGraph(): Graph<N>

    /**
     * Returns true if this network allows parallel edges. Attempting to add a parallel edge to a
     * network that does not allow them will throw an [IllegalArgumentException].
     */
    fun allowsParallelEdges(): Boolean

    /**
     * Returns true if this network allows self-loops (edges that connect a node to itself).
     * Attempting to add a self-loop to a network that does not allow them will throw an [ ].
     */
    fun allowsSelfLoops(): Boolean

    /** Returns the order of iteration for the elements of [.nodes].  */
    fun nodeOrder(): ElementOrder<N>

    /** Returns the order of iteration for the elements of [.edges].  */
    fun edgeOrder(): ElementOrder<E>

    //
    // Element-level accessors
    //

    /**
     * Returns the nodes which have an incident edge in common with `node` in this network.
     *
     * @throws IllegalArgumentException if `node` is not an element of this network
     */
    fun adjacentNodes(node: N): Set<N>

    /**
     * Returns all nodes in this network adjacent to `node` which can be reached by traversing
     * `node`'s incoming edges *against* the direction (if any) of the edge.
     *
     *
     * In an undirected network, this is equivalent to [.adjacentNodes].
     *
     * @throws IllegalArgumentException if `node` is not an element of this network
     */
    override fun predecessors(node: N): Set<N>

    /**
     * Returns all nodes in this network adjacent to `node` which can be reached by traversing
     * `node`'s outgoing edges in the direction (if any) of the edge.
     *
     *
     * In an undirected network, this is equivalent to [.adjacentNodes].
     *
     *
     * This is *not* the same as "all nodes reachable from `node` by following outgoing
     * edges". For that functionality, see [Graphs.reachableNodes].
     *
     * @throws IllegalArgumentException if `node` is not an element of this network
     */
    override fun successors(node: N): Set<N>

    /**
     * Returns the edges whose [incident nodes][.incidentNodes] in this network include
     * `node`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this network
     */
    fun incidentEdges(node: N): Set<E>

    /**
     * Returns all edges in this network which can be traversed in the direction (if any) of the edge
     * to end at `node`.
     *
     *
     * In a directed network, an incoming edge's [EndpointPair.target] equals `node`.
     *
     *
     * In an undirected network, this is equivalent to [.incidentEdges].
     *
     * @throws IllegalArgumentException if `node` is not an element of this network
     */
    fun inEdges(node: N): Set<E>

    /**
     * Returns all edges in this network which can be traversed in the direction (if any) of the edge
     * starting from `node`.
     *
     *
     * In a directed network, an outgoing edge's [EndpointPair.source] equals `node`.
     *
     *
     * In an undirected network, this is equivalent to [.incidentEdges].
     *
     * @throws IllegalArgumentException if `node` is not an element of this network
     */
    fun outEdges(node: N): Set<E>

    /**
     * Returns the count of `node`'s [incident edges][.incidentEdges], counting
     * self-loops twice (equivalently, the number of times an edge touches `node`).
     *
     *
     * For directed networks, this is equal to `inDegree(node) + outDegree(node)`.
     *
     *
     * For undirected networks, this is equal to `incidentEdges(node).size()` + (number of
     * self-loops incident to `node`).
     *
     *
     * If the count is greater than `Integer.MAX_VALUE`, returns `Integer.MAX_VALUE`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this network
     */
    fun degree(node: N): Int

    /**
     * Returns the count of `node`'s [incoming edges][.inEdges] in a directed
     * network. In an undirected network, returns the [.degree].
     *
     *
     * If the count is greater than `Integer.MAX_VALUE`, returns `Integer.MAX_VALUE`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this network
     */
    fun inDegree(node: N): Int

    /**
     * Returns the count of `node`'s [outgoing edges][.outEdges] in a directed
     * network. In an undirected network, returns the [.degree].
     *
     *
     * If the count is greater than `Integer.MAX_VALUE`, returns `Integer.MAX_VALUE`.
     *
     * @throws IllegalArgumentException if `node` is not an element of this network
     */
    fun outDegree(node: N): Int

    /**
     * Returns the nodes which are the endpoints of `edge` in this network.
     *
     * @throws IllegalArgumentException if `edge` is not an element of this network
     */
    fun incidentNodes(edge: E): EndpointPair<N>

    /**
     * Returns the edges which have an [incident node][.incidentNodes] in common with
     * `edge`. An edge is not considered adjacent to itself.
     *
     * @throws IllegalArgumentException if `edge` is not an element of this network
     */
    fun adjacentEdges(edge: E): Set<E>

    /**
     * Returns the set of edges directly connecting `nodeU` to `nodeV`.
     *
     *
     * In an undirected network, this is equal to `edgesConnecting(nodeV, nodeU)`.
     *
     *
     * The resulting set of edges will be parallel (i.e. have equal [.incidentNodes].
     * If this network does not [allow parallel edges][.allowsParallelEdges], the resulting set
     * will contain at most one edge (equivalent to `edgeConnecting(nodeU, nodeV).asSet()`).
     *
     * @throws IllegalArgumentException if `nodeU` or `nodeV` is not an element of this
     * network
     */
    fun edgesConnecting(nodeU: N, nodeV: N): Set<E>

    /**
     * Returns the single edge directly connecting `nodeU` to `nodeV`, if one is present,
     * or `Optional.empty()` if no such edge exists.
     *
     *
     * In an undirected network, this is equal to `edgeConnecting(nodeV, nodeU)`.
     *
     * @throws IllegalArgumentException if there are multiple parallel edges connecting `nodeU`
     * to `nodeV`
     * @throws IllegalArgumentException if `nodeU` or `nodeV` is not an element of this
     * network
     * @since 23.0
     */
    fun edgeConnecting(nodeU: N, nodeV: N): Optional<E>

    /**
     * Returns the single edge directly connecting `nodeU` to `nodeV`, if one is present,
     * or `null` if no such edge exists.
     *
     *
     * In an undirected network, this is equal to `edgeConnectingOrNull(nodeV, nodeU)`.
     *
     * @throws IllegalArgumentException if there are multiple parallel edges connecting `nodeU`
     * to `nodeV`
     * @throws IllegalArgumentException if `nodeU` or `nodeV` is not an element of this
     * network
     * @since 23.0
     */

    fun edgeConnectingOrNull(nodeU: N, nodeV: N): E

    /**
     * Returns true if there is an edge directly connecting `nodeU` to `nodeV`. This is
     * equivalent to `nodes().contains(nodeU) && successors(nodeU).contains(nodeV)`, and to
     * `edgeConnectingOrNull(nodeU, nodeV) != null`.
     *
     *
     * In an undirected graph, this is equal to `hasEdgeConnecting(nodeV, nodeU)`.
     *
     * @since 23.0
     */
    fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean

    //
    // Network identity
    //

    /**
     * Returns `true` iff `object` is a [Network] that has the same elements and the
     * same structural relationships as those in this network.
     *
     *
     * Thus, two networks A and B are equal if **all** of the following are true:
     *
     *
     *  * A and B have equal [directedness][.isDirected].
     *  * A and B have equal [node sets][.nodes].
     *  * A and B have equal [edge sets][.edges].
     *  * Every edge in A and B connects the same nodes in the same direction (if any).
     *
     *
     *
     * Network properties besides [directedness][.isDirected] do **not** affect equality.
     * For example, two networks may be considered equal even if one allows parallel edges and the
     * other doesn't. Additionally, the order in which nodes or edges are added to the network, and
     * the order in which they are iterated over, are irrelevant.
     *
     *
     * A reference implementation of this is provided by [AbstractNetwork.equals].
     */
    override fun equals(`object`: Any?): Boolean

    /**
     * Returns the hash code for this network. The hash code of a network is defined as the hash code
     * of a map from each of its [edges][.edges] to their [ incident nodes][.incidentNodes].
     *
     *
     * A reference implementation of this is provided by [AbstractNetwork.hashCode].
     */
    override fun hashCode(): Int
}
