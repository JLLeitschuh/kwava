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

import com.google.common.annotations.Beta
import com.google.common.base.Function
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import com.google.errorprone.annotations.Immutable

/**
 * A [ValueGraph] whose elements and structural relationships will never change. Instances of
 * this class may be obtained with [.copyOf].
 *
 *
 * See the Guava User's Guide's [discussion
 * of the `Immutable*` types](https://github.com/google/guava/wiki/GraphsExplained#immutable-implementations) for more information on the properties and guarantees
 * provided by this class.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
</V></N> */
@Beta
@Immutable(containerOf = { "N", "V" })
// Extends ConfigurableValueGraph but uses ImmutableMaps.
class ImmutableValueGraph<N, V> private constructor(graph: ValueGraph<N, V>) : ConfigurableValueGraph<N, V>(ValueGraphBuilder.from<N, V>(graph), getNodeConnections<N, V>(graph), graph.edges().size) {

    override fun asGraph(): ImmutableGraph<N> {
        return ImmutableGraph(this) // safe because the view is effectively immutable
    }

    companion object {

        /** Returns an immutable copy of `graph`.  */
        fun <N, V> copyOf(graph: ValueGraph<N, V>): ImmutableValueGraph<N, V> {
            return if (graph is ImmutableValueGraph<*, *>)
                graph as ImmutableValueGraph<N, V>
            else
                ImmutableValueGraph(graph)
        }

        /**
         * Simply returns its argument.
         *
         */
        @Deprecated("no need to use this")
        fun <N, V> copyOf(graph: ImmutableValueGraph<N, V>): ImmutableValueGraph<N, V> {
            return checkNotNull(graph)
        }

        private fun <N, V> getNodeConnections(
                graph: ValueGraph<N, V>): ImmutableMap<N, GraphConnections<N, V>> {
            // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will have
            // whatever ordering the graph's nodes do, so ImmutableSortedMap is unnecessary even if the
            // input nodes are sorted.
            val nodeConnections = ImmutableMap.builder<N, GraphConnections<N, V>>()
            for (node in graph.nodes()) {
                nodeConnections.put(node, connectionsOf(graph, node))
            }
            return nodeConnections.build()
        }

        private fun <N, V> connectionsOf(
                graph: ValueGraph<N, V>, node: N): GraphConnections<N, V> {
            val successorNodeToValueFn = Function<N, V> { successorNode -> graph.edgeValueOrDefault(node, successorNode, null) }
            return if (graph.isDirected)
                DirectedGraphConnections.ofImmutable(
                        graph.predecessors(node), Maps.asMap(graph.successors(node), successorNodeToValueFn))
            else
                UndirectedGraphConnections.ofImmutable(
                        Maps.asMap(graph.adjacentNodes(node), successorNodeToValueFn))
        }
    }
}
