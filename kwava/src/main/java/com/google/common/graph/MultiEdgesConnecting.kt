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

import com.google.common.collect.AbstractIterator
import com.google.common.collect.UnmodifiableIterator
import java.util.AbstractSet
import kotlin.collections.Map.Entry


/**
 * A class to represent the set of edges connecting an (implicit) origin node to a target node.
 *
 *
 * The [.outEdgeToNode] map allows this class to work on networks with parallel edges. See
 * [EdgesConnecting] for a class that is more efficient but forbids parallel edges.
 *
 * @author James Sexton
 * @param <E> Edge parameter type
</E> */
internal abstract class MultiEdgesConnecting<E>(outEdgeToNode: Map<E, *>, targetNode: Any) : AbstractSet<E>() {

    private val outEdgeToNode: Map<E, *>
    private val targetNode: Any

    init {
        this.outEdgeToNode = checkNotNull(outEdgeToNode)
        this.targetNode = checkNotNull(targetNode)
    }

    override fun iterator(): UnmodifiableIterator<E> {
        val entries = outEdgeToNode.entries.iterator()
        return object : AbstractIterator<E>() {
            override fun computeNext(): E? {
                while (entries.hasNext()) {
                    val entry = entries.next()
                    if (targetNode == entry.value) {
                        return entry.key
                    }
                }
                return endOfData()
            }
        }
    }

    override operator fun contains(edge: Any?): Boolean {
        return targetNode == outEdgeToNode[edge]
    }
}
