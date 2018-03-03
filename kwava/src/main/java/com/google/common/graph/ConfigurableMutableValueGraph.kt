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
import com.google.common.graph.GraphConstants.SELF_LOOPS_NOT_ALLOWED
import com.google.common.graph.Graphs.checkNonNegative
import com.google.common.graph.Graphs.checkPositive


/**
 * Configurable implementation of [MutableValueGraph] that supports both directed and
 * undirected graphs. Instances of this class should be constructed with [ValueGraphBuilder].
 *
 *
 * Time complexities for mutation methods are all O(1) except for `removeNode(N node)`,
 * which is in O(d_node) where d_node is the degree of `node`.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <V> Value parameter type
</V></N> */
internal class ConfigurableMutableValueGraph<N, V>
/** Constructs a mutable graph with the properties specified in `builder`.  */
(builder: AbstractGraphBuilder<in N>) : ConfigurableValueGraph<N, V>(builder), MutableValueGraph<N, V> {

    override fun addNode(node: N): Boolean {
        checkNotNull(node, "node")

        if (containsNode(node)) {
            return false
        }

        addNodeInternal(node)
        return true
    }

    /**
     * Adds `node` to the graph and returns the associated [GraphConnections].
     *
     * @throws IllegalStateException if `node` is already present
     */

    private fun addNodeInternal(node: N): GraphConnections<N, V> {
        val connections = newConnections()
        checkState(nodeConnections.put(node, connections) == null)
        return connections
    }

    override fun putEdgeValue(nodeU: N, nodeV: N, value: V): V? {
        checkNotNull(nodeU, "nodeU")
        checkNotNull(nodeV, "nodeV")
        checkNotNull(value, "value")

        if (!allowsSelfLoops()) {
            checkArgument(nodeU != nodeV, SELF_LOOPS_NOT_ALLOWED, nodeU)
        }

        var connectionsU: GraphConnections<N, V>? = nodeConnections.get(nodeU)
        if (connectionsU == null) {
            connectionsU = addNodeInternal(nodeU)
        }
        val previousValue = connectionsU.addSuccessor(nodeV, value)
        var connectionsV: GraphConnections<N, V>? = nodeConnections.get(nodeV)
        if (connectionsV == null) {
            connectionsV = addNodeInternal(nodeV)
        }
        connectionsV.addPredecessor(nodeU, value)
        if (previousValue == null) {
            checkPositive(++edgeCount)
        }
        return previousValue
    }

    override fun removeNode(node: N): Boolean {
        checkNotNull(node, "node")

        val connections = nodeConnections.get(node) ?: return false

        if (allowsSelfLoops()) {
            // Remove self-loop (if any) first, so we don't get CME while removing incident edges.
            if (connections.removeSuccessor(node) != null) {
                connections.removePredecessor(node)
                --edgeCount
            }
        }

        for (successor in connections.successors()) {
            nodeConnections.getWithoutCaching(successor).removePredecessor(node)
            --edgeCount
        }
        if (isDirected) { // In undirected graphs, the successor and predecessor sets are equal.
            for (predecessor in connections.predecessors()) {
                checkState(nodeConnections.getWithoutCaching(predecessor).removeSuccessor(node) != null)
                --edgeCount
            }
        }
        nodeConnections.remove(node)
        checkNonNegative(edgeCount)
        return true
    }

    override fun removeEdge(nodeU: N, nodeV: N): V? {
        checkNotNull(nodeU, "nodeU")
        checkNotNull(nodeV, "nodeV")

        val connectionsU = nodeConnections.get(nodeU)
        val connectionsV = nodeConnections.get(nodeV)
        if (connectionsU == null || connectionsV == null) {
            return null
        }

        val previousValue = connectionsU.removeSuccessor(nodeV)
        if (previousValue != null) {
            connectionsV.removePredecessor(nodeU)
            checkNonNegative(--edgeCount)
        }
        return previousValue
    }

    private fun newConnections(): GraphConnections<N, V> {
        return if (isDirected)
            DirectedGraphConnections.of()
        else
            UndirectedGraphConnections.of()
    }
}
