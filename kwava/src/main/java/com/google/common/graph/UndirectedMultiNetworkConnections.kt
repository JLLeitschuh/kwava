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
import com.google.common.graph.GraphConstants.INNER_CAPACITY
import com.google.common.graph.GraphConstants.INNER_LOAD_FACTOR

import com.google.common.collect.HashMultiset
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Multiset
import com.google.errorprone.annotations.concurrent.LazyInit
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.Collections
import java.util.HashMap


/**
 * An implementation of [NetworkConnections] for undirected networks with parallel edges.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
</E></N> */
internal class UndirectedMultiNetworkConnections<N, E> private constructor(incidentEdges: Map<E, N>) : AbstractUndirectedNetworkConnections<N, E>(incidentEdges) {

    @LazyInit
    @Transient
    private var adjacentNodesReference: Reference<Multiset<N>>? = null

    override fun adjacentNodes(): Set<N> {
        return Collections.unmodifiableSet(adjacentNodesMultiset().elementSet())
    }

    private fun adjacentNodesMultiset(): Multiset<N> {
        var adjacentNodes = getReference(adjacentNodesReference)
        if (adjacentNodes == null) {
            adjacentNodes = HashMultiset.create(incidentEdgeMap.values)
            adjacentNodesReference = SoftReference(adjacentNodes)
        }
        return adjacentNodes
    }

    override fun edgesConnecting(node: N): Set<E> {
        return object : MultiEdgesConnecting<E>(incidentEdgeMap, node) {
            override fun size(): Int {
                return adjacentNodesMultiset().count(node)
            }
        }
    }

    override fun removeInEdge(edge: E, isSelfLoop: Boolean): N? {
        return if (!isSelfLoop) {
            removeOutEdge(edge)
        } else null
    }

    override fun removeOutEdge(edge: E): N {
        val node = super.removeOutEdge(edge)
        val adjacentNodes = getReference(adjacentNodesReference)
        if (adjacentNodes != null) {
            checkState(adjacentNodes.remove(node))
        }
        return node
    }

    override fun addInEdge(edge: E, node: N, isSelfLoop: Boolean) {
        if (!isSelfLoop) {
            addOutEdge(edge, node)
        }
    }

    override fun addOutEdge(edge: E, node: N) {
        super.addOutEdge(edge, node)
        val adjacentNodes = getReference(adjacentNodesReference)
        if (adjacentNodes != null) {
            checkState(adjacentNodes.add(node))
        }
    }

    companion object {

        fun <N, E> of(): UndirectedMultiNetworkConnections<N, E> {
            return UndirectedMultiNetworkConnections(
                    HashMap(INNER_CAPACITY, INNER_LOAD_FACTOR))
        }

        fun <N, E> ofImmutable(incidentEdges: Map<E, N>): UndirectedMultiNetworkConnections<N, E> {
            return UndirectedMultiNetworkConnections(ImmutableMap.copyOf(incidentEdges))
        }


        private fun <T> getReference(reference: Reference<T>?): T? {
            return reference?.get()
        }
    }
}
