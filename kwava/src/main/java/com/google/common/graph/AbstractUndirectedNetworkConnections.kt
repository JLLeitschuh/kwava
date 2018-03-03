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

import java.util.Collections

/**
 * A base implementation of [NetworkConnections] for undirected networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
</E></N> */
internal abstract class AbstractUndirectedNetworkConnections<N, E> protected constructor(incidentEdgeMap: Map<E, N>) : NetworkConnections<N, E> {
    /** Keys are edges incident to the origin node, values are the node at the other end.  */
    protected val incidentEdgeMap: MutableMap<E, N>

    init {
        this.incidentEdgeMap = checkNotNull(incidentEdgeMap)
    }

    override fun predecessors(): Set<N> {
        return adjacentNodes()
    }

    override fun successors(): Set<N> {
        return adjacentNodes()
    }

    override fun incidentEdges(): Set<E> {
        return Collections.unmodifiableSet(incidentEdgeMap.keys)
    }

    override fun inEdges(): Set<E> {
        return incidentEdges()
    }

    override fun outEdges(): Set<E> {
        return incidentEdges()
    }

    override fun adjacentNode(edge: E): N {
        return checkNotNull(incidentEdgeMap[edge])
    }

    override fun removeInEdge(edge: E, isSelfLoop: Boolean): N? {
        return if (!isSelfLoop) {
            removeOutEdge(edge)
        } else null
    }

    override fun removeOutEdge(edge: E): N {
        val previousNode = incidentEdgeMap.remove(edge)
        return checkNotNull(previousNode)
    }

    override fun addInEdge(edge: E, node: N, isSelfLoop: Boolean) {
        if (!isSelfLoop) {
            addOutEdge(edge, node)
        }
    }

    override fun addOutEdge(edge: E, node: N) {
        val previousNode = incidentEdgeMap.put(edge, node)
        checkState(previousNode == null)
    }
}
