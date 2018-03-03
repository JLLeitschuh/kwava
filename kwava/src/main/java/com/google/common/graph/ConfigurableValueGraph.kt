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

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.graph.GraphConstants.DEFAULT_NODE_COUNT
import com.google.common.graph.Graphs.checkNonNegative
import java.util.TreeMap


/**
 * Configurable implementation of [ValueGraph] that supports the options supplied by [ ].
 *
 *
 * This class maintains a map of nodes to [GraphConnections].
 *
 *
 * Collection-returning accessors return unmodifiable views: the view returned will reflect
 * changes to the graph (if the graph is mutable) but may not be modified by the user.
 *
 *
 * The time complexity of all collection-returning accessors is O(1), since views are returned.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <V> Value parameter type
</V></N> */
internal open class ConfigurableValueGraph<N, V>
/**
 * Constructs a graph with the properties specified in `builder`, initialized with the given
 * node map.
 */
@JvmOverloads constructor(
        builder: AbstractGraphBuilder<in N>,
        nodeConnections: Map<N, GraphConnections<N, V>> = builder.nodeOrder.createMap(
                builder.expectedNodeCount.or(DEFAULT_NODE_COUNT)),
        edgeCount: Long = 0L) : AbstractValueGraph<N, V>() {
    override val isDirected: Boolean
    private val allowsSelfLoops: Boolean
    private val nodeOrder: ElementOrder<N>

    protected val nodeConnections: MapIteratorCache<N, GraphConnections<N, V>>

    protected var edgeCount: Long = 0 // must be updated when edges are added or removed

    init {
        this.isDirected = builder.directed
        this.allowsSelfLoops = builder.allowsSelfLoops
        this.nodeOrder = builder.nodeOrder.cast()
        // Prefer the heavier "MapRetrievalCache" for nodes if lookup is expensive.
        this.nodeConnections = if (nodeConnections is TreeMap<*, *>)
            MapRetrievalCache(nodeConnections)
        else
            MapIteratorCache(nodeConnections)
        this.edgeCount = checkNonNegative(edgeCount)
    }

    override fun nodes(): Set<N> {
        return nodeConnections.unmodifiableKeySet()
    }

    override fun allowsSelfLoops(): Boolean {
        return allowsSelfLoops
    }

    override fun nodeOrder(): ElementOrder<N> {
        return nodeOrder
    }

    override fun adjacentNodes(node: N): Set<N> {
        return checkedConnections(node).adjacentNodes()
    }

    override fun predecessors(node: N): Set<N> {
        return checkedConnections(node).predecessors()
    }

    override fun successors(node: N): Set<N> {
        return checkedConnections(node).successors()
    }

    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean {
        checkNotNull(nodeU)
        checkNotNull(nodeV)
        val connectionsU = nodeConnections.get(nodeU)
        return connectionsU != null && connectionsU.successors().contains(nodeV)
    }

    override fun edgeValueOrDefault(nodeU: N, nodeV: N, defaultValue: V): V {
        checkNotNull(nodeU)
        checkNotNull(nodeV)
        val connectionsU = nodeConnections.get(nodeU)
        val value = connectionsU?.value(nodeV)
        return value ?: defaultValue
    }

    override fun edgeCount(): Long {
        return edgeCount
    }

    protected fun checkedConnections(node: N): GraphConnections<N, V> {
        val connections = nodeConnections.get(node)
        if (connections == null) {
            checkNotNull(node)
            throw IllegalArgumentException("Node $node is not an element of this graph.")
        }
        return connections
    }

    protected fun containsNode(node: N): Boolean {
        return nodeConnections.containsKey(node)
    }
}
/** Constructs a graph with the properties specified in `builder`.  */
