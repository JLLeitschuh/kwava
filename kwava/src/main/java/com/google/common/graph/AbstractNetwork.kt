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

import com.google.common.graph.GraphConstants.MULTIPLE_EDGES_CONNECTING

import com.google.common.annotations.Beta
import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterators
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.google.common.math.IntMath
import java.util.Optional


/**
 * This class provides a skeletal implementation of [Network]. It is recommended to extend
 * this class rather than implement [Network] directly.
 *
 *
 * The methods implemented in this class should not be overridden unless the subclass admits a
 * more efficient implementation.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
</E></N> */
@Beta
abstract class AbstractNetwork<N, E> : Network<N, E> {

    override fun asGraph(): Graph<N> {
        return object : AbstractGraph<N>() {

            override val isDirected: Boolean
                get() = this@AbstractNetwork.isDirected

            override fun nodes(): Set<N> {
                return this@AbstractNetwork.nodes()
            }

            override fun edges(): Set<EndpointPair<N>> {
                return if (allowsParallelEdges()) {
                    super.edges() // Defer to AbstractGraph implementation.
                } else object : AbstractSet<EndpointPair<N>>() {
                    override fun iterator(): Iterator<EndpointPair<N>> {
                        return Iterators.transform(
                                this@AbstractNetwork.edges().iterator()
                        ) { edge -> incidentNodes(edge) }
                    }

                    override val size: Int
                        get() = this@AbstractNetwork.edges().size


                    // Mostly safe: We check contains(u) before calling successors(u), so we perform unsafe
                    // operations only in weird cases like checking for an EndpointPair<ArrayList> in a
                    // Network<LinkedList>.
                    override operator fun contains(obj: EndpointPair<N>): Boolean {
                        val endpointPair = obj as EndpointPair<*>?
                        return (isDirected == endpointPair!!.isOrdered
                                && nodes().contains(endpointPair.nodeU())
                                && successors(endpointPair.nodeU() as N).contains(endpointPair.nodeV() as N))
                    }
                }

                // Optimized implementation assumes no parallel edges (1:1 edge to EndpointPair mapping).
            }

            override fun nodeOrder(): ElementOrder<N> {
                return this@AbstractNetwork.nodeOrder()
            }

            override fun allowsSelfLoops(): Boolean {
                return this@AbstractNetwork.allowsSelfLoops()
            }

            override fun adjacentNodes(node: N): Set<N> {
                return this@AbstractNetwork.adjacentNodes(node)
            }

            override fun predecessors(node: N): Set<N> {
                return this@AbstractNetwork.predecessors(node)
            }

            override fun successors(node: N): Set<N> {
                return this@AbstractNetwork.successors(node)
            }

            // DO NOT override the AbstractGraph *degree() implementations.
        }
    }

    override fun degree(node: N): Int {
        return if (isDirected) {
            IntMath.saturatedAdd(inEdges(node).size, outEdges(node).size)
        } else {
            IntMath.saturatedAdd(incidentEdges(node).size, edgesConnecting(node, node).size)
        }
    }

    override fun inDegree(node: N): Int {
        return if (isDirected) inEdges(node).size else degree(node)
    }

    override fun outDegree(node: N): Int {
        return if (isDirected) outEdges(node).size else degree(node)
    }

    override fun adjacentEdges(edge: E): Set<E> {
        val endpointPair = incidentNodes(edge) // Verifies that edge is in this network.
        val endpointPairIncidentEdges = Sets.union(incidentEdges(endpointPair.nodeU()), incidentEdges(endpointPair.nodeV()))
        return Sets.difference(endpointPairIncidentEdges, ImmutableSet.of(edge))
    }

    override fun edgesConnecting(nodeU: N, nodeV: N): Set<E> {
        val outEdgesU = outEdges(nodeU)
        val inEdgesV = inEdges(nodeV)
        return if (outEdgesU.size <= inEdgesV.size)
            Sets.filter(outEdgesU, connectedPredicate(nodeU, nodeV))
        else
            Sets.filter(inEdgesV, connectedPredicate(nodeV, nodeU))
    }

    private fun connectedPredicate(nodePresent: N, nodeToCheck: N): Predicate<E> {
        return Predicate { edge -> incidentNodes(edge).adjacentNode(nodePresent) == nodeToCheck }
    }

    override fun edgeConnecting(nodeU: N, nodeV: N): Optional<E> {
        val edgesConnecting = edgesConnecting(nodeU, nodeV)
        when (edgesConnecting.size) {
            0 -> return Optional.empty()
            1 -> return Optional.of(edgesConnecting.iterator().next())
            else -> throw IllegalArgumentException(String.format(MULTIPLE_EDGES_CONNECTING, nodeU, nodeV))
        }
    }

    override fun edgeConnectingOrNull(nodeU: N, nodeV: N): E? {
        return edgeConnecting(nodeU, nodeV).orElse(null)
    }

    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean {
        return !edgesConnecting(nodeU, nodeV).isEmpty()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj !is Network<*, *>) {
            return false
        }
        val other = obj as Network<*, *>?

        return (isDirected == other!!.isDirected
                && nodes() == other.nodes()
                && edgeIncidentNodesMap(this) == edgeIncidentNodesMap<*, *>(other))
    }

    override fun hashCode(): Int {
        return edgeIncidentNodesMap(this).hashCode()
    }

    /** Returns a string representation of this network.  */
    override fun toString(): String {
        return ("isDirected: "
                + isDirected
                + ", allowsParallelEdges: "
                + allowsParallelEdges()
                + ", allowsSelfLoops: "
                + allowsSelfLoops()
                + ", nodes: "
                + nodes()
                + ", edges: "
                + edgeIncidentNodesMap(this))
    }

    private fun <N, E> edgeIncidentNodesMap(network: Network<N, E>): Map<E, EndpointPair<N>> {
        val edgeToIncidentNodesFn = Function<E, EndpointPair<N>> { edge -> network.incidentNodes(edge) }
        return Maps.asMap(network.edges(), edgeToIncidentNodesFn)
    }
}
