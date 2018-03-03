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
 * A builder for constructing instances of [MutableGraph] with user-defined properties.
 *
 *
 * A graph built by this class will have the following properties by default:
 *
 *
 *  * does not allow self-loops
 *  * orders [Graph.nodes] in the order in which the elements were added
 *
 *
 *
 * Example of use:
 *
 * <pre>`MutableGraph<String> graph = GraphBuilder.undirected().allowsSelfLoops(true).build();
 * graph.putEdge("bread", "bread");
 * graph.putEdge("chocolate", "peanut butter");
 * graph.putEdge("peanut butter", "jelly");
`</pre> *
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
class GraphBuilder<N>
/** Creates a new instance with the specified edge directionality.  */
private constructor(directed: Boolean) : AbstractGraphBuilder<N>(directed) {

    /**
     * Specifies whether the graph will allow self-loops (edges that connect a node to itself).
     * Attempting to add a self-loop to a graph that does not allow them will throw an [ ].
     */
    fun allowsSelfLoops(allowsSelfLoops: Boolean): GraphBuilder<N> {
        this.allowsSelfLoops = allowsSelfLoops
        return this
    }

    /**
     * Specifies the expected number of nodes in the graph.
     *
     * @throws IllegalArgumentException if `expectedNodeCount` is negative
     */
    fun expectedNodeCount(expectedNodeCount: Int): GraphBuilder<N> {
        this.expectedNodeCount = Optional.of(checkNonNegative(expectedNodeCount))
        return this
    }

    /** Specifies the order of iteration for the elements of [Graph.nodes].  */
    fun <N1 : N> nodeOrder(nodeOrder: ElementOrder<N1>): GraphBuilder<N1> {
        val newBuilder = cast<N1>()
        newBuilder.nodeOrder = checkNotNull(nodeOrder)
        return newBuilder
    }

    /** Returns an empty [MutableGraph] with the properties of this [GraphBuilder].  */
    fun <N1 : N> build(): MutableGraph<N1> {
        return ConfigurableMutableGraph(this)
    }

    private fun <N1 : N> cast(): GraphBuilder<N1> {
        return this as GraphBuilder<N1>
    }

    companion object {

        /** Returns a [GraphBuilder] for building directed graphs.  */
        fun directed(): GraphBuilder<Any> {
            return GraphBuilder(true)
        }

        /** Returns a [GraphBuilder] for building undirected graphs.  */
        fun undirected(): GraphBuilder<Any> {
            return GraphBuilder(false)
        }

        /**
         * Returns a [GraphBuilder] initialized with all properties queryable from `graph`.
         *
         *
         * The "queryable" properties are those that are exposed through the [Graph] interface,
         * such as [Graph.isDirected]. Other properties, such as [.expectedNodeCount],
         * are not set in the new builder.
         */
        fun <N> from(graph: Graph<N>): GraphBuilder<N> {
            return GraphBuilder<N>(graph.isDirected)
                    .allowsSelfLoops(graph.allowsSelfLoops())
                    .nodeOrder(graph.nodeOrder())
        }
    }
}
