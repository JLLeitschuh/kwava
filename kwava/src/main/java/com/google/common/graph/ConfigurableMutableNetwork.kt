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

import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.base.Preconditions.checkState
import com.google.common.graph.GraphConstants.PARALLEL_EDGES_NOT_ALLOWED
import com.google.common.graph.GraphConstants.REUSING_EDGE
import com.google.common.graph.GraphConstants.SELF_LOOPS_NOT_ALLOWED

import com.google.common.collect.ImmutableList


/**
 * Configurable implementation of [MutableNetwork] that supports both directed and undirected
 * graphs. Instances of this class should be constructed with [NetworkBuilder].
 *
 *
 * Time complexities for mutation methods are all O(1) except for `removeNode(N node)`,
 * which is in O(d_node) where d_node is the degree of `node`.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
</E></N> */
internal class ConfigurableMutableNetwork<N, E>
/** Constructs a mutable graph with the properties specified in `builder`.  */
(builder: NetworkBuilder<in N, in E>) : ConfigurableNetwork<N, E>(builder), MutableNetwork<N, E> {

    override fun addNode(node: N): Boolean {
        checkNotNull(node, "node")

        if (containsNode(node)) {
            return false
        }

        addNodeInternal(node)
        return true
    }

    /**
     * Adds `node` to the graph and returns the associated [NetworkConnections].
     *
     * @throws IllegalStateException if `node` is already present
     */

    private fun addNodeInternal(node: N): NetworkConnections<N, E> {
        val connections = newConnections()
        checkState(nodeConnections.put(node, connections) == null)
        return connections
    }

    override fun addEdge(nodeU: N, nodeV: N, edge: E): Boolean {
        checkNotNull(nodeU, "nodeU")
        checkNotNull(nodeV, "nodeV")
        checkNotNull(edge, "edge")

        if (containsEdge(edge)) {
            val existingIncidentNodes = incidentNodes(edge)
            val newIncidentNodes = EndpointPair.of(this, nodeU, nodeV)
            checkArgument(
                    existingIncidentNodes == newIncidentNodes,
                    REUSING_EDGE,
                    edge,
                    existingIncidentNodes,
                    newIncidentNodes)
            return false
        }
        var connectionsU: NetworkConnections<N, E>? = nodeConnections.get(nodeU)
        if (!allowsParallelEdges()) {
            checkArgument(
                    !(connectionsU != null && connectionsU.successors().contains(nodeV)),
                    PARALLEL_EDGES_NOT_ALLOWED,
                    nodeU,
                    nodeV)
        }
        val isSelfLoop = nodeU == nodeV
        if (!allowsSelfLoops()) {
            checkArgument(!isSelfLoop, SELF_LOOPS_NOT_ALLOWED, nodeU)
        }

        if (connectionsU == null) {
            connectionsU = addNodeInternal(nodeU)
        }
        connectionsU.addOutEdge(edge, nodeV)
        var connectionsV: NetworkConnections<N, E>? = nodeConnections.get(nodeV)
        if (connectionsV == null) {
            connectionsV = addNodeInternal(nodeV)
        }
        connectionsV.addInEdge(edge, nodeU, isSelfLoop)
        edgeToReferenceNode.put(edge, nodeU)
        return true
    }

    override fun removeNode(node: N): Boolean {
        checkNotNull(node, "node")

        val connections = nodeConnections.get(node) ?: return false

        // Since views are returned, we need to copy the edges that will be removed.
        // Thus we avoid modifying the underlying view while iterating over it.
        for (edge in ImmutableList.copyOf(connections.incidentEdges())) {
            removeEdge(edge)
        }
        nodeConnections.remove(node)
        return true
    }

    override fun removeEdge(edge: E): Boolean {
        checkNotNull(edge, "edge")

        val nodeU = edgeToReferenceNode.get(edge) ?: return false

        val connectionsU = nodeConnections.get(nodeU)
        val nodeV = connectionsU.adjacentNode(edge)
        val connectionsV = nodeConnections.get(nodeV)
        connectionsU.removeOutEdge(edge)
        connectionsV.removeInEdge(edge, allowsSelfLoops() && nodeU == nodeV)
        edgeToReferenceNode.remove(edge)
        return true
    }

    private fun newConnections(): NetworkConnections<N, E> {
        return if (isDirected)
            if (allowsParallelEdges())
                DirectedMultiNetworkConnections.of()
            else
                DirectedNetworkConnections.of()
        else if (allowsParallelEdges())
            UndirectedMultiNetworkConnections.of()
        else
            UndirectedNetworkConnections.of()
    }
}
