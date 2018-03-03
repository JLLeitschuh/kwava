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

import com.google.common.annotations.Beta
import com.google.common.base.Function
import com.google.common.collect.Maps
import java.util.Optional


/**
 * This class provides a skeletal implementation of [ValueGraph]. It is recommended to extend
 * this class rather than implement [ValueGraph] directly.
 *
 *
 * The methods implemented in this class should not be overridden unless the subclass admits a
 * more efficient implementation.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
</V></N> */
@Beta
abstract class AbstractValueGraph<N, V> : AbstractBaseGraph<N>(), ValueGraph<N, V> {

    override fun asGraph(): Graph<N> {
        return object : AbstractGraph<N>() {

            override val isDirected: Boolean
                get() = this@AbstractValueGraph.isDirected

            override fun nodes(): Set<N> {
                return this@AbstractValueGraph.nodes()
            }

            override fun edges(): Set<EndpointPair<N>> {
                return this@AbstractValueGraph.edges()
            }

            override fun allowsSelfLoops(): Boolean {
                return this@AbstractValueGraph.allowsSelfLoops()
            }

            override fun nodeOrder(): ElementOrder<N> {
                return this@AbstractValueGraph.nodeOrder()
            }

            override fun adjacentNodes(node: N): Set<N> {
                return this@AbstractValueGraph.adjacentNodes(node)
            }

            override fun predecessors(node: N): Set<N> {
                return this@AbstractValueGraph.predecessors(node)
            }

            override fun successors(node: N): Set<N> {
                return this@AbstractValueGraph.successors(node)
            }

            override fun degree(node: N): Int {
                return this@AbstractValueGraph.degree(node)
            }

            override fun inDegree(node: N): Int {
                return this@AbstractValueGraph.inDegree(node)
            }

            override fun outDegree(node: N): Int {
                return this@AbstractValueGraph.outDegree(node)
            }
        }
    }

    override fun edgeValue(nodeU: N, nodeV: N): Optional<V> {
        return Optional.ofNullable(edgeValueOrDefault(nodeU, nodeV, null))
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj !is ValueGraph<*, *>) {
            return false
        }
        val other = obj as ValueGraph<*, *>?

        return (isDirected == other!!.isDirected
                && nodes() == other.nodes()
                && edgeValueMap(this) == edgeValueMap<*, *>(other))
    }

    override fun hashCode(): Int {
        return edgeValueMap(this).hashCode()
    }

    /** Returns a string representation of this graph.  */
    override fun toString(): String {
        return ("isDirected: "
                + isDirected
                + ", allowsSelfLoops: "
                + allowsSelfLoops()
                + ", nodes: "
                + nodes()
                + ", edges: "
                + edgeValueMap(this))
    }

    private fun <N, V> edgeValueMap(graph: ValueGraph<N, V>): Map<EndpointPair<N>, V> {
        val edgeToValueFn = Function<EndpointPair<N>, V> { edge -> graph.edgeValueOrDefault(edge.nodeU(), edge.nodeV(), null) }
        return Maps.asMap(graph.edges(), edgeToValueFn)
    }
}
