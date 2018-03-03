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
import com.google.common.graph.GraphConstants.INNER_CAPACITY
import com.google.common.graph.GraphConstants.INNER_LOAD_FACTOR

import com.google.common.collect.ImmutableMap
import java.util.Collections
import java.util.HashMap

/**
 * An implementation of [GraphConnections] for undirected graphs.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
</V></N> */
internal class UndirectedGraphConnections<N, V> private constructor(adjacentNodeValues: Map<N, V>) : GraphConnections<N, V> {
    private val adjacentNodeValues: MutableMap<N, V>

    init {
        this.adjacentNodeValues = checkNotNull(adjacentNodeValues)
    }

    override fun adjacentNodes(): Set<N> {
        return Collections.unmodifiableSet(adjacentNodeValues.keys)
    }

    override fun predecessors(): Set<N> {
        return adjacentNodes()
    }

    override fun successors(): Set<N> {
        return adjacentNodes()
    }

    override fun value(node: N): V {
        return adjacentNodeValues[node]
    }

    override fun removePredecessor(node: N) {
        val unused = removeSuccessor(node)
    }

    override fun removeSuccessor(node: N): V {
        return adjacentNodeValues.remove(node)
    }

    override fun addPredecessor(node: N, value: V) {
        val unused = addSuccessor(node, value)
    }

    override fun addSuccessor(node: N, value: V): V {
        return adjacentNodeValues.put(node, value)
    }

    companion object {

        fun <N, V> of(): UndirectedGraphConnections<N, V> {
            return UndirectedGraphConnections(HashMap(INNER_CAPACITY, INNER_LOAD_FACTOR))
        }

        fun <N, V> ofImmutable(adjacentNodeValues: Map<N, V>): UndirectedGraphConnections<N, V> {
            return UndirectedGraphConnections(ImmutableMap.copyOf(adjacentNodeValues))
        }
    }
}
