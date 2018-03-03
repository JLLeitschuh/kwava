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
 * A builder for constructing instances of [MutableValueGraph] with user-defined properties.
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
 * <pre>`MutableValueGraph<String, Double> graph =
 * ValueGraphBuilder.undirected().allowsSelfLoops(true).build();
 * graph.putEdgeValue("San Francisco", "San Francisco", 0.0);
 * graph.putEdgeValue("San Jose", "San Jose", 0.0);
 * graph.putEdgeValue("San Francisco", "San Jose", 48.4);
`</pre> *
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @since 20.0
 */
@Beta
class ValueGraphBuilder<N, V>
/** Creates a new instance with the specified edge directionality.  */
private constructor(directed: Boolean) : AbstractGraphBuilder<N>(directed) {

    /**
     * Specifies whether the graph will allow self-loops (edges that connect a node to itself).
     * Attempting to add a self-loop to a graph that does not allow them will throw an [ ].
     */
    fun allowsSelfLoops(allowsSelfLoops: Boolean): ValueGraphBuilder<N, V> {
        this.allowsSelfLoops = allowsSelfLoops
        return this
    }

    /**
     * Specifies the expected number of nodes in the graph.
     *
     * @throws IllegalArgumentException if `expectedNodeCount` is negative
     */
    fun expectedNodeCount(expectedNodeCount: Int): ValueGraphBuilder<N, V> {
        this.expectedNodeCount = Optional.of(checkNonNegative(expectedNodeCount))
        return this
    }

    /** Specifies the order of iteration for the elements of [Graph.nodes].  */
    fun <N1 : N> nodeOrder(nodeOrder: ElementOrder<N1>): ValueGraphBuilder<N1, V> {
        val newBuilder = cast<N1, V>()
        newBuilder.nodeOrder = checkNotNull(nodeOrder)
        return newBuilder
    }

    /**
     * Returns an empty [MutableValueGraph] with the properties of this [ ].
     */
    fun <N1 : N, V1 : V> build(): MutableValueGraph<N1, V1> {
        return ConfigurableMutableValueGraph(this)
    }

    private fun <N1 : N, V1 : V> cast(): ValueGraphBuilder<N1, V1> {
        return this as ValueGraphBuilder<N1, V1>
    }

    companion object {

        /** Returns a [ValueGraphBuilder] for building directed graphs.  */
        fun directed(): ValueGraphBuilder<Any, Any> {
            return ValueGraphBuilder(true)
        }

        /** Returns a [ValueGraphBuilder] for building undirected graphs.  */
        fun undirected(): ValueGraphBuilder<Any, Any> {
            return ValueGraphBuilder(false)
        }

        /**
         * Returns a [ValueGraphBuilder] initialized with all properties queryable from `graph`.
         *
         *
         * The "queryable" properties are those that are exposed through the [ValueGraph]
         * interface, such as [ValueGraph.isDirected]. Other properties, such as [ ][.expectedNodeCount], are not set in the new builder.
         */
        fun <N, V> from(graph: ValueGraph<N, V>): ValueGraphBuilder<N, V> {
            return ValueGraphBuilder<N, V>(graph.isDirected)
                    .allowsSelfLoops(graph.allowsSelfLoops())
                    .nodeOrder(graph.nodeOrder())
        }
    }
}
