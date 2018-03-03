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

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterators
import com.google.common.collect.UnmodifiableIterator
import java.util.AbstractSet


/**
 * A class to represent the set of edges connecting an (implicit) origin node to a target node.
 *
 *
 * The [.nodeToOutEdge] map means this class only works on networks without parallel edges.
 * See [MultiEdgesConnecting] for a class that works with parallel edges.
 *
 * @author James Sexton
 * @param <E> Edge parameter type
</E> */
internal class EdgesConnecting<E>(nodeToEdgeMap: Map<*, E>, targetNode: Any) : AbstractSet<E>() {

    private val nodeToOutEdge: Map<*, E>
    private val targetNode: Any


    private val connectingEdge: E?
        get() = nodeToOutEdge[targetNode]

    init {
        this.nodeToOutEdge = checkNotNull(nodeToEdgeMap)
        this.targetNode = checkNotNull(targetNode)
    }

    override fun iterator(): UnmodifiableIterator<E> {
        val connectingEdge = connectingEdge
        return if (connectingEdge == null)
            ImmutableSet.of<E>().iterator()
        else
            Iterators.singletonIterator(connectingEdge)
    }

    override fun size(): Int {
        return if (connectingEdge == null) 0 else 1
    }

    override operator fun contains(edge: Any?): Boolean {
        val connectingEdge = connectingEdge
        return connectingEdge != null && connectingEdge == edge
    }
}
