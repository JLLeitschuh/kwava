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

import com.google.common.base.Preconditions.checkArgument
import com.google.common.graph.GraphConstants.NODE_NOT_IN_GRAPH

import com.google.common.annotations.Beta
import com.google.common.base.Objects
import com.google.common.collect.Iterables
import com.google.common.collect.Maps

import java.util.ArrayDeque
import java.util.Collections
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.Optional
import java.util.Queue


/**
 * Static utility methods for [Graph], [ValueGraph], and [Network] instances.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
object Graphs {

    // Graph query methods

    /**
     * Returns true if `graph` has at least one cycle. A cycle is defined as a non-empty subset
     * of edges in a graph arranged to form a path (a sequence of adjacent outgoing edges) starting
     * and ending with the same node.
     *
     *
     * This method will detect any non-empty cycle, including self-loops (a cycle of length 1).
     */
    fun <N> hasCycle(graph: Graph<N>): Boolean {
        val numEdges = graph.edges().size
        if (numEdges == 0) {
            return false // An edge-free graph is acyclic by definition.
        }
        if (!graph.isDirected && numEdges >= graph.nodes().size) {
            return true // Optimization for the undirected case: at least one cycle must exist.
        }

        val visitedNodes = Maps.newHashMapWithExpectedSize<Any, NodeVisitState>(graph.nodes().size)
        for (node in graph.nodes()) {
            if (subgraphHasCycle(graph, visitedNodes, node, null)) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if `network` has at least one cycle. A cycle is defined as a non-empty
     * subset of edges in a graph arranged to form a path (a sequence of adjacent outgoing edges)
     * starting and ending with the same node.
     *
     *
     * This method will detect any non-empty cycle, including self-loops (a cycle of length 1).
     */
    fun hasCycle(network: Network<*, *>): Boolean {
        // In a directed graph, parallel edges cannot introduce a cycle in an acyclic graph.
        // However, in an undirected graph, any parallel edge induces a cycle in the graph.
        return if (!network.isDirected
                && network.allowsParallelEdges()
                && network.edges().size > network.asGraph().edges().size) {
            true
        } else hasCycle<*>(network.asGraph())
    }

    /**
     * Performs a traversal of the nodes reachable from `node`. If we ever reach a node we've
     * already visited (following only outgoing edges and without reusing edges), we know there's a
     * cycle in the graph.
     */
    private fun <N> subgraphHasCycle(
            graph: Graph<N>,
            visitedNodes: MutableMap<Any, NodeVisitState>,
            node: N,
            previousNode: N?): Boolean {
        val state = visitedNodes.get(node)
        if (state == NodeVisitState.COMPLETE) {
            return false
        }
        if (state == NodeVisitState.PENDING) {
            return true
        }

        visitedNodes[node] = NodeVisitState.PENDING
        for (nextNode in graph.successors(node)) {
            if (canTraverseWithoutReusingEdge(graph, nextNode, previousNode) && subgraphHasCycle(graph, visitedNodes, nextNode, node)) {
                return true
            }
        }
        visitedNodes[node] = NodeVisitState.COMPLETE
        return false
    }

    /**
     * Determines whether an edge has already been used during traversal. In the directed case a cycle
     * is always detected before reusing an edge, so no special logic is required. In the undirected
     * case, we must take care not to "backtrack" over an edge (i.e. going from A to B and then going
     * from B to A).
     */
    private fun canTraverseWithoutReusingEdge(
            graph: Graph<*>, nextNode: Any, previousNode: Any?): Boolean {
        return if (graph.isDirected || !Objects.equal(previousNode, nextNode)) {
            true
        } else false
        // This falls into the undirected A->B->A case. The Graph interface does not support parallel
        // edges, so this traversal would require reusing the undirected AB edge.
    }

    /**
     * Returns the transitive closure of `graph`. The transitive closure of a graph is another
     * graph with an edge connecting node A to node B if node B is [reachable][.reachableNodes] from node A.
     *
     *
     * This is a "snapshot" based on the current topology of `graph`, rather than a live view
     * of the transitive closure of `graph`. In other words, the returned [Graph] will not
     * be updated after modifications to `graph`.
     */
    // TODO(b/31438252): Consider potential optimizations for this algorithm.
    fun <N> transitiveClosure(graph: Graph<N>): Graph<N> {
        val transitiveClosure = GraphBuilder.from(graph).allowsSelfLoops(true).build<N>()
        // Every node is, at a minimum, reachable from itself. Since the resulting transitive closure
        // will have no isolated nodes, we can skip adding nodes explicitly and let putEdge() do it.

        if (graph.isDirected) {
            // Note: works for both directed and undirected graphs, but we only use in the directed case.
            for (node in graph.nodes()) {
                for (reachableNode in reachableNodes(graph, node)) {
                    transitiveClosure.putEdge(node, reachableNode)
                }
            }
        } else {
            // An optimization for the undirected case: for every node B reachable from node A,
            // node A and node B have the same reachability set.
            val visitedNodes = HashSet<N>()
            for (node in graph.nodes()) {
                if (!visitedNodes.contains(node)) {
                    val reachableNodes = reachableNodes(graph, node)
                    visitedNodes.addAll(reachableNodes)
                    var pairwiseMatch = 1 // start at 1 to include self-loops
                    for (nodeU in reachableNodes) {
                        for (nodeV in Iterables.limit(reachableNodes, pairwiseMatch++)) {
                            transitiveClosure.putEdge(nodeU, nodeV)
                        }
                    }
                }
            }
        }

        return transitiveClosure
    }

    /**
     * Returns the set of nodes that are reachable from `node`. Node B is defined as reachable
     * from node A if there exists a path (a sequence of adjacent outgoing edges) starting at node A
     * and ending at node B. Note that a node is always reachable from itself via a zero-length path.
     *
     *
     * This is a "snapshot" based on the current topology of `graph`, rather than a live view
     * of the set of nodes reachable from `node`. In other words, the returned [Set] will
     * not be updated after modifications to `graph`.
     *
     * @throws IllegalArgumentException if `node` is not present in `graph`
     */
    fun <N> reachableNodes(graph: Graph<N>, node: N): Set<N> {
        checkArgument(graph.nodes().contains(node), NODE_NOT_IN_GRAPH, node)
        val visitedNodes = LinkedHashSet<N>()
        val queuedNodes = ArrayDeque<N>()
        visitedNodes.add(node)
        queuedNodes.add(node)
        // Perform a breadth-first traversal rooted at the input node.
        while (!queuedNodes.isEmpty()) {
            val currentNode = queuedNodes.remove()
            for (successor in graph.successors(currentNode)) {
                if (visitedNodes.add(successor)) {
                    queuedNodes.add(successor)
                }
            }
        }
        return Collections.unmodifiableSet(visitedNodes)
    }

    // Graph mutation methods

    // Graph view methods

    /**
     * Returns a view of `graph` with the direction (if any) of every edge reversed. All other
     * properties remain intact, and further updates to `graph` will be reflected in the view.
     */
    fun <N> transpose(graph: Graph<N>): Graph<N> {
        if (!graph.isDirected) {
            return graph // the transpose of an undirected graph is an identical graph
        }

        return if (graph is TransposedGraph<*>) {
            (graph as TransposedGraph<N>).graph
        } else TransposedGraph(graph)

    }

    /**
     * Returns a view of `graph` with the direction (if any) of every edge reversed. All other
     * properties remain intact, and further updates to `graph` will be reflected in the view.
     */
    fun <N, V> transpose(graph: ValueGraph<N, V>): ValueGraph<N, V> {
        if (!graph.isDirected) {
            return graph // the transpose of an undirected graph is an identical graph
        }

        return if (graph is TransposedValueGraph<*, *>) {
            (graph as TransposedValueGraph<N, V>).graph
        } else TransposedValueGraph(graph)

    }

    /**
     * Returns a view of `network` with the direction (if any) of every edge reversed. All other
     * properties remain intact, and further updates to `network` will be reflected in the view.
     */
    fun <N, E> transpose(network: Network<N, E>): Network<N, E> {
        if (!network.isDirected) {
            return network // the transpose of an undirected network is an identical network
        }

        return if (network is TransposedNetwork<*, *>) {
            (network as TransposedNetwork<N, E>).network
        } else TransposedNetwork(network)

    }

    // NOTE: this should work as long as the delegate graph's implementation of edges() (like that of
    // AbstractGraph) derives its behavior from calling successors().
    private class TransposedGraph<N> internal constructor(private val graph: Graph<N>) : ForwardingGraph<N>() {

        override fun delegate(): Graph<N> {
            return graph
        }

        override fun predecessors(node: N): Set<N> {
            return delegate().successors(node) // transpose
        }

        override fun successors(node: N): Set<N> {
            return delegate().predecessors(node) // transpose
        }

        override fun inDegree(node: N): Int {
            return delegate().outDegree(node) // transpose
        }

        override fun outDegree(node: N): Int {
            return delegate().inDegree(node) // transpose
        }

        override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean {
            return delegate().hasEdgeConnecting(nodeV, nodeU) // transpose
        }
    }

    // NOTE: this should work as long as the delegate graph's implementation of edges() (like that of
    // AbstractValueGraph) derives its behavior from calling successors().
    private class TransposedValueGraph<N, V> internal constructor(private val graph: ValueGraph<N, V>) : ForwardingValueGraph<N, V>() {

        override fun delegate(): ValueGraph<N, V> {
            return graph
        }

        override fun predecessors(node: N): Set<N> {
            return delegate().successors(node) // transpose
        }

        override fun successors(node: N): Set<N> {
            return delegate().predecessors(node) // transpose
        }

        override fun inDegree(node: N): Int {
            return delegate().outDegree(node) // transpose
        }

        override fun outDegree(node: N): Int {
            return delegate().inDegree(node) // transpose
        }

        override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean {
            return delegate().hasEdgeConnecting(nodeV, nodeU) // transpose
        }

        override fun edgeValue(nodeU: N, nodeV: N): Optional<V> {
            return delegate().edgeValue(nodeV, nodeU) // transpose
        }

        override fun edgeValueOrDefault(nodeU: N, nodeV: N, defaultValue: V): V {
            return delegate().edgeValueOrDefault(nodeV, nodeU, defaultValue) // transpose
        }
    }

    private class TransposedNetwork<N, E> internal constructor(private val network: Network<N, E>) : ForwardingNetwork<N, E>() {

        override fun delegate(): Network<N, E> {
            return network
        }

        override fun predecessors(node: N): Set<N> {
            return delegate().successors(node) // transpose
        }

        override fun successors(node: N): Set<N> {
            return delegate().predecessors(node) // transpose
        }

        override fun inDegree(node: N): Int {
            return delegate().outDegree(node) // transpose
        }

        override fun outDegree(node: N): Int {
            return delegate().inDegree(node) // transpose
        }

        override fun inEdges(node: N): Set<E> {
            return delegate().outEdges(node) // transpose
        }

        override fun outEdges(node: N): Set<E> {
            return delegate().inEdges(node) // transpose
        }

        override fun incidentNodes(edge: E): EndpointPair<N> {
            val endpointPair = delegate().incidentNodes(edge)
            return EndpointPair.of(network, endpointPair.nodeV(), endpointPair.nodeU()) // transpose
        }

        override fun edgesConnecting(nodeU: N, nodeV: N): Set<E> {
            return delegate().edgesConnecting(nodeV, nodeU) // transpose
        }

        override fun edgeConnecting(nodeU: N, nodeV: N): Optional<E> {
            return delegate().edgeConnecting(nodeV, nodeU) // transpose
        }

        override fun edgeConnectingOrNull(nodeU: N, nodeV: N): E? {
            return delegate().edgeConnectingOrNull(nodeV, nodeU) // transpose
        }

        override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean {
            return delegate().hasEdgeConnecting(nodeV, nodeU) // transpose
        }
    }

    // Graph copy methods

    /**
     * Returns the subgraph of `graph` induced by `nodes`. This subgraph is a new graph
     * that contains all of the nodes in `nodes`, and all of the [edges][Graph.edges]
     * from `graph` for which both nodes are contained by `nodes`.
     *
     * @throws IllegalArgumentException if any element in `nodes` is not a node in the graph
     */
    fun <N> inducedSubgraph(graph: Graph<N>, nodes: Iterable<N>): MutableGraph<N> {
        val subgraph = if (nodes is Collection<*>)
            GraphBuilder.from(graph).expectedNodeCount((nodes as Collection<*>).size).build()
        else
            GraphBuilder.from(graph).build<N>()
        for (node in nodes) {
            subgraph.addNode(node)
        }
        for (node in subgraph.nodes()) {
            for (successorNode in graph.successors(node)) {
                if (subgraph.nodes().contains(successorNode)) {
                    subgraph.putEdge(node, successorNode)
                }
            }
        }
        return subgraph
    }

    /**
     * Returns the subgraph of `graph` induced by `nodes`. This subgraph is a new graph
     * that contains all of the nodes in `nodes`, and all of the [edges][Graph.edges]
     * (and associated edge values) from `graph` for which both nodes are contained by `nodes`.
     *
     * @throws IllegalArgumentException if any element in `nodes` is not a node in the graph
     */
    fun <N, V> inducedSubgraph(
            graph: ValueGraph<N, V>, nodes: Iterable<N>): MutableValueGraph<N, V> {
        val subgraph = if (nodes is Collection<*>)
            ValueGraphBuilder.from(graph).expectedNodeCount((nodes as Collection<*>).size).build()
        else
            ValueGraphBuilder.from(graph).build<N, V>()
        for (node in nodes) {
            subgraph.addNode(node)
        }
        for (node in subgraph.nodes()) {
            for (successorNode in graph.successors(node)) {
                if (subgraph.nodes().contains(successorNode)) {
                    subgraph.putEdgeValue(
                            node, successorNode, graph.edgeValueOrDefault(node, successorNode, null))
                }
            }
        }
        return subgraph
    }

    /**
     * Returns the subgraph of `network` induced by `nodes`. This subgraph is a new graph
     * that contains all of the nodes in `nodes`, and all of the [edges][Network.edges]
     * from `network` for which the [incident nodes][Network.incidentNodes] are
     * both contained by `nodes`.
     *
     * @throws IllegalArgumentException if any element in `nodes` is not a node in the graph
     */
    fun <N, E> inducedSubgraph(
            network: Network<N, E>, nodes: Iterable<N>): MutableNetwork<N, E> {
        val subgraph = if (nodes is Collection<*>)
            NetworkBuilder.from(network).expectedNodeCount((nodes as Collection<*>).size).build()
        else
            NetworkBuilder.from(network).build<N, E>()
        for (node in nodes) {
            subgraph.addNode(node)
        }
        for (node in subgraph.nodes()) {
            for (edge in network.outEdges(node)) {
                val successorNode = network.incidentNodes(edge).adjacentNode(node)
                if (subgraph.nodes().contains(successorNode)) {
                    subgraph.addEdge(node, successorNode, edge)
                }
            }
        }
        return subgraph
    }

    /** Creates a mutable copy of `graph` with the same nodes and edges.  */
    fun <N> copyOf(graph: Graph<N>): MutableGraph<N> {
        val copy = GraphBuilder.from(graph).expectedNodeCount(graph.nodes().size).build<N>()
        for (node in graph.nodes()) {
            copy.addNode(node)
        }
        for (edge in graph.edges()) {
            copy.putEdge(edge.nodeU(), edge.nodeV())
        }
        return copy
    }

    /** Creates a mutable copy of `graph` with the same nodes, edges, and edge values.  */
    fun <N, V> copyOf(graph: ValueGraph<N, V>): MutableValueGraph<N, V> {
        val copy = ValueGraphBuilder.from(graph).expectedNodeCount(graph.nodes().size).build<N, V>()
        for (node in graph.nodes()) {
            copy.addNode(node)
        }
        for (edge in graph.edges()) {
            copy.putEdgeValue(
                    edge.nodeU(), edge.nodeV(), graph.edgeValueOrDefault(edge.nodeU(), edge.nodeV(), null))
        }
        return copy
    }

    /** Creates a mutable copy of `network` with the same nodes and edges.  */
    fun <N, E> copyOf(network: Network<N, E>): MutableNetwork<N, E> {
        val copy = NetworkBuilder.from(network)
                .expectedNodeCount(network.nodes().size)
                .expectedEdgeCount(network.edges().size)
                .build<N, E>()
        for (node in network.nodes()) {
            copy.addNode(node)
        }
        for (edge in network.edges()) {
            val endpointPair = network.incidentNodes(edge)
            copy.addEdge(endpointPair.nodeU(), endpointPair.nodeV(), edge)
        }
        return copy
    }


    internal fun checkNonNegative(value: Int): Int {
        checkArgument(value >= 0, "Not true that %s is non-negative.", value)
        return value
    }


    internal fun checkNonNegative(value: Long): Long {
        checkArgument(value >= 0, "Not true that %s is non-negative.", value)
        return value
    }


    internal fun checkPositive(value: Int): Int {
        checkArgument(value > 0, "Not true that %s is positive.", value)
        return value
    }


    internal fun checkPositive(value: Long): Long {
        checkArgument(value > 0, "Not true that %s is positive.", value)
        return value
    }

    /**
     * An enum representing the state of a node during DFS. `PENDING` means that the node is on
     * the stack of the DFS, while `COMPLETE` means that the node and all its successors have
     * been already explored. Any node that has not been explored will not have a state at all.
     */
    private enum class NodeVisitState {
        PENDING,
        COMPLETE
    }
}
