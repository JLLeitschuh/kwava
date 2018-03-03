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

import com.google.common.base.Preconditions.checkNotNull

import com.google.common.annotations.Beta
import com.google.common.base.Function
import com.google.common.base.Functions
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import com.google.common.graph.GraphConstants.Presence
import com.google.errorprone.annotations.Immutable

/**
 * A [Graph] whose elements and structural relationships will never change. Instances of this
 * class may be obtained with [.copyOf].
 *
 *
 * See the Guava User's Guide's [discussion
 * of the `Immutable*` types](https://github.com/google/guava/wiki/GraphsExplained#immutable-implementations) for more information on the properties and guarantees
 * provided by this class.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @since 20.0
</N> */
@Beta
@Immutable(containerOf = { "N" })
class ImmutableGraph<N> internal constructor(private// The backing graph must be immutable.
                                             val backingGraph: BaseGraph<N>) : ForwardingGraph<N>() {

    override fun delegate(): BaseGraph<N> {
        return backingGraph
    }

    companion object {

        /** Returns an immutable copy of `graph`.  */
        fun <N> copyOf(graph: Graph<N>): ImmutableGraph<N> {
            return if (graph is ImmutableGraph<*>)
                graph as ImmutableGraph<N>
            else
                ImmutableGraph(
                        ConfigurableValueGraph(
                                GraphBuilder.from(graph), getNodeConnections(graph), graph.edges().size.toLong()))
        }

        /**
         * Simply returns its argument.
         *
         */
        @Deprecated("no need to use this")
        fun <N> copyOf(graph: ImmutableGraph<N>): ImmutableGraph<N> {
            return checkNotNull(graph)
        }

        private fun <N> getNodeConnections(
                graph: Graph<N>): ImmutableMap<N, GraphConnections<N, Presence>> {
            // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will have
            // whatever ordering the graph's nodes do, so ImmutableSortedMap is unnecessary even if the
            // input nodes are sorted.
            val nodeConnections = ImmutableMap.builder<N, GraphConnections<N, Presence>>()
            for (node in graph.nodes()) {
                nodeConnections.put(node, connectionsOf(graph, node))
            }
            return nodeConnections.build()
        }

        private fun <N> connectionsOf(graph: Graph<N>, node: N): GraphConnections<N, Presence> {
            val edgeValueFn = Functions.constant(Presence.EDGE_EXISTS)
            return if (graph.isDirected)
                DirectedGraphConnections.ofImmutable(
                        graph.predecessors(node), Maps.asMap(graph.successors(node), edgeValueFn))
            else
                UndirectedGraphConnections.ofImmutable(
                        Maps.asMap(graph.adjacentNodes(node), edgeValueFn))
        }
    }
}
