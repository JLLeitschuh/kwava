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
import com.google.common.graph.Graphs.checkNonNegative

import com.google.common.annotations.Beta
import com.google.common.base.Optional

/**
 * A builder for constructing instances of [MutableNetwork] with user-defined properties.
 *
 *
 * A network built by this class will have the following properties by default:
 *
 *
 *  * does not allow parallel edges
 *  * does not allow self-loops
 *  * orders [Network.nodes] and [Network.edges] in the order in which the
 * elements were added
 *
 *
 *
 * Example of use:
 *
 * <pre>`MutableNetwork<String, Integer> flightNetwork =
 * NetworkBuilder.directed().allowsParallelEdges(true).build();
 * flightNetwork.addEdge("LAX", "ATL", 3025);
 * flightNetwork.addEdge("LAX", "ATL", 1598);
 * flightNetwork.addEdge("ATL", "LAX", 2450);
`</pre> *
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
class NetworkBuilder<N, E>
/** Creates a new instance with the specified edge directionality.  */
private constructor(directed: Boolean) : AbstractGraphBuilder<N>(directed) {
    internal var allowsParallelEdges = false
    internal var edgeOrder: ElementOrder<in E> = ElementOrder.insertion()
    internal var expectedEdgeCount = Optional.absent<Int>()

    /**
     * Specifies whether the network will allow parallel edges. Attempting to add a parallel edge to a
     * network that does not allow them will throw an [UnsupportedOperationException].
     */
    fun allowsParallelEdges(allowsParallelEdges: Boolean): NetworkBuilder<N, E> {
        this.allowsParallelEdges = allowsParallelEdges
        return this
    }

    /**
     * Specifies whether the network will allow self-loops (edges that connect a node to itself).
     * Attempting to add a self-loop to a network that does not allow them will throw an [ ].
     */
    fun allowsSelfLoops(allowsSelfLoops: Boolean): NetworkBuilder<N, E> {
        this.allowsSelfLoops = allowsSelfLoops
        return this
    }

    /**
     * Specifies the expected number of nodes in the network.
     *
     * @throws IllegalArgumentException if `expectedNodeCount` is negative
     */
    fun expectedNodeCount(expectedNodeCount: Int): NetworkBuilder<N, E> {
        this.expectedNodeCount = Optional.of(checkNonNegative(expectedNodeCount))
        return this
    }

    /**
     * Specifies the expected number of edges in the network.
     *
     * @throws IllegalArgumentException if `expectedEdgeCount` is negative
     */
    fun expectedEdgeCount(expectedEdgeCount: Int): NetworkBuilder<N, E> {
        this.expectedEdgeCount = Optional.of(checkNonNegative(expectedEdgeCount))
        return this
    }

    /** Specifies the order of iteration for the elements of [Network.nodes].  */
    fun <N1 : N> nodeOrder(nodeOrder: ElementOrder<N1>): NetworkBuilder<N1, E> {
        val newBuilder = cast<N1, E>()
        newBuilder.nodeOrder = checkNotNull(nodeOrder)
        return newBuilder
    }

    /** Specifies the order of iteration for the elements of [Network.edges].  */
    fun <E1 : E> edgeOrder(edgeOrder: ElementOrder<E1>): NetworkBuilder<N, E1> {
        val newBuilder = cast<N, E1>()
        newBuilder.edgeOrder = checkNotNull(edgeOrder)
        return newBuilder
    }

    /** Returns an empty [MutableNetwork] with the properties of this [NetworkBuilder].  */
    fun <N1 : N, E1 : E> build(): MutableNetwork<N1, E1> {
        return ConfigurableMutableNetwork(this)
    }

    private fun <N1 : N, E1 : E> cast(): NetworkBuilder<N1, E1> {
        return this as NetworkBuilder<N1, E1>
    }

    companion object {

        /** Returns a [NetworkBuilder] for building directed networks.  */
        fun directed(): NetworkBuilder<Any, Any> {
            return NetworkBuilder(true)
        }

        /** Returns a [NetworkBuilder] for building undirected networks.  */
        fun undirected(): NetworkBuilder<Any, Any> {
            return NetworkBuilder(false)
        }

        /**
         * Returns a [NetworkBuilder] initialized with all properties queryable from `network`.
         *
         *
         * The "queryable" properties are those that are exposed through the [Network] interface,
         * such as [Network.isDirected]. Other properties, such as [ ][.expectedNodeCount], are not set in the new builder.
         */
        fun <N, E> from(network: Network<N, E>): NetworkBuilder<N, E> {
            return NetworkBuilder<N, E>(network.isDirected)
                    .allowsParallelEdges(network.allowsParallelEdges())
                    .allowsSelfLoops(network.allowsSelfLoops())
                    .nodeOrder(network.nodeOrder())
                    .edgeOrder(network.edgeOrder())
        }
    }
}
