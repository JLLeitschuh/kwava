/*
 * Copyright (C) 2017 The Guava Authors
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
import com.google.common.base.Preconditions.checkState

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterators
import com.google.common.collect.Sets
import com.google.common.collect.UnmodifiableIterator
import com.google.common.math.IntMath
import com.google.common.primitives.Ints


/**
 * This class provides a skeletal implementation of [BaseGraph].
 *
 *
 * The methods implemented in this class should not be overridden unless the subclass admits a
 * more efficient implementation.
 *
 * @author James Sexton
 * @param N Node parameter type
 * */
abstract class AbstractBaseGraph<N> : BaseGraph<N> {

    /**
     * Returns the number of edges in this graph; used to calculate the size of [.edges]. This
     * implementation requires O(|N|) time. Classes extending this one may manually keep track of the
     * number of edges as the graph is updated, and override this method for better performance.
     */
    protected open fun edgeCount(): Long {
        var degreeSum = 0L
        for (node in nodes()) {
            degreeSum += degree(node).toLong()
        }
        // According to the degree sum formula, this is equal to twice the number of edges.
        checkState(degreeSum and 1 == 0L)
        return degreeSum.ushr(1)
    }

    /**
     * An implementation of [BaseGraph.edges] defined in terms of [.nodes] and [ ][.successors].
     */
    override fun edges(): Set<EndpointPair<N>> {
        return object : AbstractSet<EndpointPair<N>>() {

            override fun iterator(): UnmodifiableIterator<EndpointPair<N>> {
                return EndpointPairIterator.of(this@AbstractBaseGraph)
            }

            override val size: Int
                get() = Ints.saturatedCast(edgeCount())

            // Mostly safe: We check contains(u) before calling successors(u), so we perform unsafe
            // operations only in weird cases like checking for an EndpointPair<ArrayList> in a
            // Graph<LinkedList>.
            override operator fun contains(obj: EndpointPair<N>): Boolean {
                val endpointPair = obj as EndpointPair<*>?
                return (isDirected == endpointPair!!.isOrdered
                        && nodes().contains(endpointPair.nodeU())
                        && successors(endpointPair.nodeU() as N).contains(endpointPair.nodeV() as N))
            }
        }
    }

    override fun incidentEdges(node: N): Set<EndpointPair<N>> {
        checkNotNull(node)
        checkArgument(nodes().contains(node), "Node $node is not an element of this graph.")
        return IncidentEdgeSet.of(this, node)
    }

    override fun degree(node: N): Int {
        if (isDirected) {
            return IntMath.saturatedAdd(predecessors(node).size, successors(node).size)
        } else {
            val neighbors = adjacentNodes(node)
            val selfLoopCount = if (allowsSelfLoops() && neighbors.contains(node)) 1 else 0
            return IntMath.saturatedAdd(neighbors.size, selfLoopCount)
        }
    }

    override fun inDegree(node: N): Int {
        return if (isDirected) predecessors(node).size else degree(node)
    }

    override fun outDegree(node: N): Int {
        return if (isDirected) successors(node).size else degree(node)
    }

    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean {
        checkNotNull(nodeU)
        checkNotNull(nodeV)
        return nodes().contains(nodeU) && successors(nodeU).contains(nodeV)
    }

    private abstract class IncidentEdgeSet<N>
    private constructor(
            protected val graph: BaseGraph<N>,
            protected val node: N) : AbstractSet<EndpointPair<N>>() {


        private class Directed<N>
        internal constructor(graph: BaseGraph<N>, node: N) : IncidentEdgeSet<N>(graph, node) {

            override fun iterator(): UnmodifiableIterator<EndpointPair<N>> {
                return Iterators.unmodifiableIterator(
                        Iterators.concat(
                                Iterators.transform(
                                        graph.predecessors(node).iterator()
                                ) { predecessor -> EndpointPair.ordered(predecessor, node) },
                                Iterators.transform(
                                        // filter out 'node' from successors (already covered by predecessors, above)
                                        Sets.difference(graph.successors(node), ImmutableSet.of(node)).iterator()
                                ) { successor -> EndpointPair.ordered(node, successor) }))
            }

            override val size: Int
                get() = graph.inDegree(node) + graph.outDegree(node) - if (graph.successors(node).contains(node)) 1 else 0

            override operator fun contains(obj: EndpointPair<N>): Boolean {
                val endpointPair = obj as EndpointPair<*>?
                if (!endpointPair!!.isOrdered) {
                    return false
                }

                val source = endpointPair.source()
                val target = endpointPair.target()
                return node == source && graph.successors(node).contains(target) || node == target && graph.predecessors(node).contains(source)
            }
        }

        private class Undirected<N>
        internal constructor(graph: BaseGraph<N>, node: N) : IncidentEdgeSet<N>(graph, node) {

            override fun iterator(): UnmodifiableIterator<EndpointPair<N>> {
                return Iterators.unmodifiableIterator(
                        Iterators.transform(
                                graph.adjacentNodes(node).iterator()
                        ) { adjacentNode -> EndpointPair.unordered(node, adjacentNode) })
            }

            override val size: Int
                get() = graph.adjacentNodes(node).size

            override operator fun contains(obj: EndpointPair<N>): Boolean {
                val endpointPair = obj as EndpointPair<*>?
                if (endpointPair!!.isOrdered) {
                    return false
                }
                val adjacent = graph.adjacentNodes(node)
                val nodeU = endpointPair.nodeU()
                val nodeV = endpointPair.nodeV()

                return node == nodeV && adjacent.contains(nodeU) || node == nodeU && adjacent.contains(nodeV)
            }
        }

        companion object {
            @JvmStatic
            fun <N> of(graph: BaseGraph<N>, node: N): IncidentEdgeSet<N> {
                return if (graph.isDirected) Directed(graph, node) else Undirected(graph, node)
            }
        }
    }
}
