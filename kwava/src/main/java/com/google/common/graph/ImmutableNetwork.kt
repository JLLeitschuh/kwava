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
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import com.google.errorprone.annotations.Immutable

/**
 * A [Network] whose elements and structural relationships will never change. Instances of
 * this class may be obtained with [.copyOf].
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
 * @param <E> Edge parameter type
 * @since 20.0
</E></N> */
@Beta
@Immutable(containerOf = { "N", "E" })
// Extends ConfigurableNetwork but uses ImmutableMaps.
class ImmutableNetwork<N, E> private constructor(network: Network<N, E>) : ConfigurableNetwork<N, E>(NetworkBuilder.from<N, E>(network), getNodeConnections<N, E>(network), getEdgeToReferenceNode<N, E>(network)) {

    override fun asGraph(): ImmutableGraph<N> {
        return ImmutableGraph(super.asGraph()) // safe because the view is effectively immutable
    }

    companion object {

        /** Returns an immutable copy of `network`.  */
        fun <N, E> copyOf(network: Network<N, E>): ImmutableNetwork<N, E> {
            return if (network is ImmutableNetwork<*, *>)
                network as ImmutableNetwork<N, E>
            else
                ImmutableNetwork(network)
        }

        /**
         * Simply returns its argument.
         *
         */
        @Deprecated("no need to use this")
        fun <N, E> copyOf(network: ImmutableNetwork<N, E>): ImmutableNetwork<N, E> {
            return checkNotNull(network)
        }

        private fun <N, E> getNodeConnections(network: Network<N, E>): Map<N, NetworkConnections<N, E>> {
            // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will have
            // whatever ordering the network's nodes do, so ImmutableSortedMap is unnecessary even if the
            // input nodes are sorted.
            val nodeConnections = ImmutableMap.builder<N, NetworkConnections<N, E>>()
            for (node in network.nodes()) {
                nodeConnections.put(node, connectionsOf(network, node))
            }
            return nodeConnections.build()
        }

        private fun <N, E> getEdgeToReferenceNode(network: Network<N, E>): Map<E, N> {
            // ImmutableMap.Builder maintains the order of the elements as inserted, so the map will have
            // whatever ordering the network's edges do, so ImmutableSortedMap is unnecessary even if the
            // input edges are sorted.
            val edgeToReferenceNode = ImmutableMap.builder<E, N>()
            for (edge in network.edges()) {
                edgeToReferenceNode.put(edge, network.incidentNodes(edge).nodeU())
            }
            return edgeToReferenceNode.build()
        }

        private fun <N, E> connectionsOf(network: Network<N, E>, node: N): NetworkConnections<N, E> {
            if (network.isDirected) {
                val inEdgeMap = Maps.asMap(network.inEdges(node), sourceNodeFn(network))
                val outEdgeMap = Maps.asMap(network.outEdges(node), targetNodeFn(network))
                val selfLoopCount = network.edgesConnecting(node, node).size
                return if (network.allowsParallelEdges())
                    DirectedMultiNetworkConnections.ofImmutable(inEdgeMap, outEdgeMap, selfLoopCount)
                else
                    DirectedNetworkConnections.ofImmutable(inEdgeMap, outEdgeMap, selfLoopCount)
            } else {
                val incidentEdgeMap = Maps.asMap(network.incidentEdges(node), adjacentNodeFn(network, node))
                return if (network.allowsParallelEdges())
                    UndirectedMultiNetworkConnections.ofImmutable(incidentEdgeMap)
                else
                    UndirectedNetworkConnections.ofImmutable(incidentEdgeMap)
            }
        }

        private fun <N, E> sourceNodeFn(network: Network<N, E>): Function<E, N> {
            return Function { edge -> network.incidentNodes(edge).source() }
        }

        private fun <N, E> targetNodeFn(network: Network<N, E>): Function<E, N> {
            return Function { edge -> network.incidentNodes(edge).target() }
        }

        private fun <N, E> adjacentNodeFn(network: Network<N, E>, node: N): Function<E, N> {
            return Function { edge -> network.incidentNodes(edge).adjacentNode(node) }
        }
    }
}
