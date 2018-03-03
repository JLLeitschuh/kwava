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
import com.google.common.graph.GraphConstants.DEFAULT_EDGE_COUNT
import com.google.common.graph.GraphConstants.DEFAULT_NODE_COUNT
import com.google.common.graph.GraphConstants.EDGE_NOT_IN_GRAPH
import com.google.common.graph.GraphConstants.NODE_NOT_IN_GRAPH

import com.google.common.collect.ImmutableSet
import java.util.TreeMap


/**
 * Configurable implementation of [Network] that supports the options supplied by [ ].
 *
 *
 * This class maintains a map of nodes to [NetworkConnections]. This class also maintains a
 * map of edges to reference nodes. The reference node is defined to be the edge's source node on
 * directed graphs, and an arbitrary endpoint of the edge on undirected graphs.
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
 * @param <E> Edge parameter type
</E></N> */
internal open class ConfigurableNetwork<N, E>
/**
 * Constructs a graph with the properties specified in `builder`, initialized with the given
 * node and edge maps.
 */
@JvmOverloads constructor(
        builder: NetworkBuilder<in N, in E>,
        nodeConnections: Map<N, NetworkConnections<N, E>> = builder.nodeOrder.createMap(
                builder.expectedNodeCount.or(DEFAULT_NODE_COUNT)),
        edgeToReferenceNode: Map<E, N> = builder.edgeOrder.createMap(builder.expectedEdgeCount.or(DEFAULT_EDGE_COUNT))) : AbstractNetwork<N, E>() {
    override val isDirected: Boolean
    private val allowsParallelEdges: Boolean
    private val allowsSelfLoops: Boolean
    private val nodeOrder: ElementOrder<N>
    private val edgeOrder: ElementOrder<E>

    protected val nodeConnections: MapIteratorCache<N, NetworkConnections<N, E>>

    // We could make this a Map<E, EndpointPair<N>>. It would make incidentNodes(edge) slightly
    // faster, but also make Networks consume 5 to 20+% (increasing with average degree) more memory.
    protected val edgeToReferenceNode: MapIteratorCache<E, N> // referenceNode == source if directed

    init {
        this.isDirected = builder.directed
        this.allowsParallelEdges = builder.allowsParallelEdges
        this.allowsSelfLoops = builder.allowsSelfLoops
        this.nodeOrder = builder.nodeOrder.cast()
        this.edgeOrder = builder.edgeOrder.cast()
        // Prefer the heavier "MapRetrievalCache" for nodes if lookup is expensive. This optimizes
        // methods that access the same node(s) repeatedly, such as Graphs.removeEdgesConnecting().
        this.nodeConnections = if (nodeConnections is TreeMap<*, *>)
            MapRetrievalCache(nodeConnections)
        else
            MapIteratorCache(nodeConnections)
        this.edgeToReferenceNode = MapIteratorCache(edgeToReferenceNode)
    }

    override fun nodes(): Set<N> {
        return nodeConnections.unmodifiableKeySet()
    }

    override fun edges(): Set<E> {
        return edgeToReferenceNode.unmodifiableKeySet()
    }

    override fun allowsParallelEdges(): Boolean {
        return allowsParallelEdges
    }

    override fun allowsSelfLoops(): Boolean {
        return allowsSelfLoops
    }

    override fun nodeOrder(): ElementOrder<N> {
        return nodeOrder
    }

    override fun edgeOrder(): ElementOrder<E> {
        return edgeOrder
    }

    override fun incidentEdges(node: N): Set<E> {
        return checkedConnections(node).incidentEdges()
    }

    override fun incidentNodes(edge: E): EndpointPair<N> {
        val nodeU = checkedReferenceNode(edge)
        val nodeV = nodeConnections.get(nodeU).adjacentNode(edge)
        return EndpointPair.of(this, nodeU, nodeV)
    }

    override fun adjacentNodes(node: N): Set<N> {
        return checkedConnections(node).adjacentNodes()
    }

    override fun edgesConnecting(nodeU: N, nodeV: N): Set<E> {
        val connectionsU = checkedConnections(nodeU)
        if (!allowsSelfLoops && nodeU === nodeV) { // just an optimization, only check reference equality
            return ImmutableSet.of()
        }
        checkArgument(containsNode(nodeV), NODE_NOT_IN_GRAPH, nodeV)
        return connectionsU.edgesConnecting(nodeV)
    }

    override fun inEdges(node: N): Set<E> {
        return checkedConnections(node).inEdges()
    }

    override fun outEdges(node: N): Set<E> {
        return checkedConnections(node).outEdges()
    }

    override fun predecessors(node: N): Set<N> {
        return checkedConnections(node).predecessors()
    }

    override fun successors(node: N): Set<N> {
        return checkedConnections(node).successors()
    }

    protected fun checkedConnections(node: N): NetworkConnections<N, E> {
        val connections = nodeConnections.get(node)
        if (connections == null) {
            checkNotNull(node)
            throw IllegalArgumentException(String.format(NODE_NOT_IN_GRAPH, node))
        }
        return connections
    }

    protected fun checkedReferenceNode(edge: E): N {
        val referenceNode = edgeToReferenceNode.get(edge)
        if (referenceNode == null) {
            checkNotNull(edge)
            throw IllegalArgumentException(String.format(EDGE_NOT_IN_GRAPH, edge))
        }
        return referenceNode
    }

    protected fun containsNode(node: N): Boolean {
        return nodeConnections.containsKey(node)
    }

    protected fun containsEdge(edge: E): Boolean {
        return edgeToReferenceNode.containsKey(edge)
    }
}
/** Constructs a graph with the properties specified in `builder`.  */
