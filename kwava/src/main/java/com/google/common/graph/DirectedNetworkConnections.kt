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
 * An implementation of [NetworkConnections] for directed networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
</E></N> */
internal class DirectedNetworkConnections<N, E> protected constructor(
        inEdgeMap: Map<E, N>, outEdgeMap: Map<E, N>, selfLoopCount: Int) : AbstractDirectedNetworkConnections<N, E>(inEdgeMap, outEdgeMap, selfLoopCount) {

    override fun predecessors(): Set<N> {
        return Collections.unmodifiableSet((inEdgeMap as BiMap<E, N>).values)
    }

    override fun successors(): Set<N> {
        return Collections.unmodifiableSet((outEdgeMap as BiMap<E, N>).values)
    }

    override fun edgesConnecting(node: N): Set<E> {
        return EdgesConnecting((outEdgeMap as BiMap<E, N>).inverse(), node)
    }

    companion object {

        fun <N, E> of(): DirectedNetworkConnections<N, E> {
            return DirectedNetworkConnections(
                    HashBiMap.create(EXPECTED_DEGREE), HashBiMap.create(EXPECTED_DEGREE), 0)
        }

        fun <N, E> ofImmutable(
                inEdges: Map<E, N>, outEdges: Map<E, N>, selfLoopCount: Int): DirectedNetworkConnections<N, E> {
            return DirectedNetworkConnections(
                    ImmutableBiMap.copyOf(inEdges), ImmutableBiMap.copyOf(outEdges), selfLoopCount)
        }
    }
}
