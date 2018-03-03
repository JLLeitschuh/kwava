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

import com.google.common.graph.GraphConstants.EXPECTED_DEGREE

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableBiMap
import java.util.Collections

/**
 * An implementation of [NetworkConnections] for undirected networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
</E></N> */
internal class UndirectedNetworkConnections<N, E> protected constructor(incidentEdgeMap: Map<E, N>) : AbstractUndirectedNetworkConnections<N, E>(incidentEdgeMap) {

    override fun adjacentNodes(): Set<N> {
        return Collections.unmodifiableSet((incidentEdgeMap as BiMap<E, N>).values)
    }

    override fun edgesConnecting(node: N): Set<E> {
        return EdgesConnecting((incidentEdgeMap as BiMap<E, N>).inverse(), node)
    }

    companion object {

        fun <N, E> of(): UndirectedNetworkConnections<N, E> {
            return UndirectedNetworkConnections(HashBiMap.create(EXPECTED_DEGREE))
        }

        fun <N, E> ofImmutable(incidentEdges: Map<E, N>): UndirectedNetworkConnections<N, E> {
            return UndirectedNetworkConnections(ImmutableBiMap.copyOf(incidentEdges))
        }
    }
}
