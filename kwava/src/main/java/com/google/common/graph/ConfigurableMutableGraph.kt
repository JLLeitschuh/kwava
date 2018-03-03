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

import com.google.common.graph.GraphConstants.Presence

/**
 * Configurable implementation of [MutableGraph] that supports both directed and undirected
 * graphs. Instances of this class should be constructed with [GraphBuilder].
 *
 *
 * Time complexities for mutation methods are all O(1) except for `removeNode(N node)`,
 * which is in O(d_node) where d_node is the degree of `node`.
 *
 * @author James Sexton
 * @param <N> Node parameter type
</N> */
internal class ConfigurableMutableGraph<N>
/** Constructs a [MutableGraph] with the properties specified in `builder`.  */
(builder: AbstractGraphBuilder<in N>) : ForwardingGraph<N>(), MutableGraph<N> {
    private val backingValueGraph: MutableValueGraph<N, Presence>

    init {
        this.backingValueGraph = ConfigurableMutableValueGraph(builder)
    }

    override fun delegate(): BaseGraph<N> {
        return backingValueGraph
    }

    override fun addNode(node: N): Boolean {
        return backingValueGraph.addNode(node)
    }

    override fun putEdge(nodeU: N, nodeV: N): Boolean {
        return backingValueGraph.putEdgeValue(nodeU, nodeV, Presence.EDGE_EXISTS) == null
    }

    override fun removeNode(node: N): Boolean {
        return backingValueGraph.removeNode(node)
    }

    override fun removeEdge(nodeU: N, nodeV: N): Boolean {
        return backingValueGraph.removeEdge(nodeU, nodeV) != null
    }
}
