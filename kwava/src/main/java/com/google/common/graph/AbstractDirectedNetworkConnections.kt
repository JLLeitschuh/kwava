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
import com.google.common.base.Preconditions.checkState
import com.google.common.collect.Iterables
import com.google.common.collect.Iterators
import com.google.common.collect.Sets
import com.google.common.collect.UnmodifiableIterator
import com.google.common.graph.Graphs.checkNonNegative
import com.google.common.graph.Graphs.checkPositive
import com.google.common.math.IntMath


/**
 * A base implementation of [NetworkConnections] for directed networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
</E></N> */
internal abstract class AbstractDirectedNetworkConnections<N, E>
private constructor(
        /** Keys are edges incoming to the origin node, values are the source node.  */
        private val inEdgeMap: MutableMap<E, N>,
        /** Keys are edges outgoing from the origin node, values are the target node.  */
        private val outEdgeMap: MutableMap<E, N>,
        private var selfLoopCount: Int = 0,
        overloadBreaker: Unit) : NetworkConnections<N, E> {

    protected constructor(
            inEdgeMap: Map<E, N>,
            outEdgeMap: Map<E, N>,
            selfLoopCount: Int = 0):
            this(inEdgeMap.toMutableMap(), outEdgeMap.toMutableMap(), selfLoopCount, Unit)

    init {
        checkNonNegative(selfLoopCount)
        checkState(selfLoopCount <= inEdgeMap.size && selfLoopCount <= outEdgeMap.size)
    }

    override fun adjacentNodes(): Set<N> {
        return Sets.union(predecessors(), successors())
    }

    override fun incidentEdges(): Set<E> {
        return object : AbstractSet<E>() {

            override fun iterator(): UnmodifiableIterator<E> {
                val incidentEdges = if (selfLoopCount == 0)
                    Iterables.concat(inEdgeMap.keys, outEdgeMap.keys)
                else
                    Sets.union(inEdgeMap.keys, outEdgeMap.keys)
                return Iterators.unmodifiableIterator(incidentEdges.iterator())
            }

            override val size: Int
                get() = IntMath.saturatedAdd(inEdgeMap.size, outEdgeMap.size - selfLoopCount)

            override operator fun contains(obj: E): Boolean {
                return inEdgeMap.containsKey(obj) || outEdgeMap.containsKey(obj)
            }
        }
    }

    override fun inEdges(): Set<E> {
        return inEdgeMap.keys
    }

    override fun outEdges(): Set<E> {
        return outEdgeMap.keys
    }

    override fun adjacentNode(edge: E): N {
        // Since the reference node is defined to be 'source' for directed graphs,
        // we can assume this edge lives in the set of outgoing edges.
        return checkNotNull(outEdgeMap[edge])
    }

    override fun removeInEdge(edge: E, isSelfLoop: Boolean): N {
        if (isSelfLoop) {
            checkNonNegative(--selfLoopCount)
        }
        val previousNode = inEdgeMap.remove(edge)
        return checkNotNull(previousNode)
    }

    override fun removeOutEdge(edge: E): N {
        val previousNode = outEdgeMap.remove(edge)
        return checkNotNull(previousNode)
    }

    override fun addInEdge(edge: E, node: N, isSelfLoop: Boolean) {
        if (isSelfLoop) {
            checkPositive(++selfLoopCount)
        }
        val previousNode = inEdgeMap.put(edge, node)
        checkState(previousNode == null)
    }

    override fun addOutEdge(edge: E, node: N) {
        val previousNode = outEdgeMap.put(edge, node)
        checkState(previousNode == null)
    }
}
