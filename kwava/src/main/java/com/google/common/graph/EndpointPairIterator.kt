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

import com.google.common.base.Preconditions.checkState

import com.google.common.collect.AbstractIterator
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets

/**
 * A class to facilitate the set returned by [Graph.edges].
 *
 * @author James Sexton
 */
internal abstract class EndpointPairIterator<N> private constructor(private val graph: BaseGraph<N>) : AbstractIterator<EndpointPair<N>>() {
    private val nodeIterator: Iterator<N>

    protected var node: N? = null // null is safe as an initial value because graphs don't allow null nodes
    protected var successorIterator: Iterator<N> = ImmutableSet.of<N>().iterator()

    init {
        this.nodeIterator = graph.nodes().iterator()
    }

    /**
     * Called after [.successorIterator] is exhausted. Advances [.node] to the next node
     * and updates [.successorIterator] to iterate through the successors of [.node].
     */
    protected fun advance(): Boolean {
        checkState(!successorIterator.hasNext())
        if (!nodeIterator.hasNext()) {
            return false
        }
        node = nodeIterator.next()
        successorIterator = graph.successors(node).iterator()
        return true
    }

    /**
     * If the graph is directed, each ordered [source, target] pair will be visited once if there is
     * an edge connecting them.
     */
    private class Directed<N> private constructor(graph: BaseGraph<N>) : EndpointPairIterator<N>(graph) {

        override fun computeNext(): EndpointPair<N>? {
            while (true) {
                if (successorIterator.hasNext()) {
                    return EndpointPair.ordered(node, successorIterator.next())
                }
                if (!advance()) {
                    return endOfData()
                }
            }
        }
    }

    /**
     * If the graph is undirected, each unordered [node, otherNode] pair (except self-loops) will be
     * visited twice if there is an edge connecting them. To avoid returning duplicate [ ]s, we keep track of the nodes that we have visited. When processing endpoint
     * pairs, we skip if the "other node" is in the visited set, as shown below:
     *
     * <pre>
     * Nodes = {N1, N2, N3, N4}
     * N2           __
     * /  \         |  |
     * N1----N3      N4__|
     *
     * Visited Nodes = {}
     * EndpointPair [N1, N2] - return
     * EndpointPair [N1, N3] - return
     * Visited Nodes = {N1}
     * EndpointPair [N2, N1] - skip
     * EndpointPair [N2, N3] - return
     * Visited Nodes = {N1, N2}
     * EndpointPair [N3, N1] - skip
     * EndpointPair [N3, N2] - skip
     * Visited Nodes = {N1, N2, N3}
     * EndpointPair [N4, N4] - return
     * Visited Nodes = {N1, N2, N3, N4}
    </pre> *
     */
    private class Undirected<N> private constructor(graph: BaseGraph<N>) : EndpointPairIterator<N>(graph) {
        private var visitedNodes: MutableSet<N>? = null

        init {
            this.visitedNodes = Sets.newHashSetWithExpectedSize(graph.nodes().size)
        }

        override fun computeNext(): EndpointPair<N>? {
            while (true) {
                while (successorIterator.hasNext()) {
                    val otherNode = successorIterator.next()
                    if (!visitedNodes!!.contains(otherNode)) {
                        return EndpointPair.unordered(node, otherNode)
                    }
                }
                // Add to visited set *after* processing neighbors so we still include self-loops.
                visitedNodes!!.add(node)
                if (!advance()) {
                    visitedNodes = null
                    return endOfData()
                }
            }
        }
    }

    companion object {

        fun <N> of(graph: BaseGraph<N>): EndpointPairIterator<N> {
            return if (graph.isDirected) Directed(graph) else Undirected(graph)
        }
    }
}