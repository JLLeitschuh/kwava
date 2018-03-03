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
 * An implementation of [NetworkConnections] for directed networks with parallel edges.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
</E></N> */
internal class DirectedMultiNetworkConnections<N, E> private constructor(
        inEdges: Map<E, N>, outEdges: Map<E, N>, selfLoopCount: Int) : AbstractDirectedNetworkConnections<N, E>(inEdges, outEdges, selfLoopCount) {

    @LazyInit
    @Transient
    private var predecessorsReference: Reference<Multiset<N>>? = null

    @LazyInit
    @Transient
    private var successorsReference: Reference<Multiset<N>>? = null

    override fun predecessors(): Set<N> {
        return Collections.unmodifiableSet(predecessorsMultiset().elementSet())
    }

    private fun predecessorsMultiset(): Multiset<N> {
        var predecessors = getReference(predecessorsReference)
        if (predecessors == null) {
            predecessors = HashMultiset.create(inEdgeMap.values)
            predecessorsReference = SoftReference(predecessors)
        }
        return predecessors
    }

    override fun successors(): Set<N> {
        return Collections.unmodifiableSet(successorsMultiset().elementSet())
    }

    private fun successorsMultiset(): Multiset<N> {
        var successors = getReference(successorsReference)
        if (successors == null) {
            successors = HashMultiset.create(outEdgeMap.values)
            successorsReference = SoftReference(successors)
        }
        return successors
    }

    override fun edgesConnecting(node: N): Set<E> {
        return object : MultiEdgesConnecting<E>(outEdgeMap, node) {
            override fun size(): Int {
                return successorsMultiset().count(node)
            }
        }
    }

    override fun removeInEdge(edge: E, isSelfLoop: Boolean): N {
        val node = super.removeInEdge(edge, isSelfLoop)
        val predecessors = getReference(predecessorsReference)
        if (predecessors != null) {
            checkState(predecessors.remove(node))
        }
        return node
    }

    override fun removeOutEdge(edge: E): N {
        val node = super.removeOutEdge(edge)
        val successors = getReference(successorsReference)
        if (successors != null) {
            checkState(successors.remove(node))
        }
        return node
    }

    override fun addInEdge(edge: E, node: N, isSelfLoop: Boolean) {
        super.addInEdge(edge, node, isSelfLoop)
        val predecessors = getReference(predecessorsReference)
        if (predecessors != null) {
            checkState(predecessors.add(node))
        }
    }

    override fun addOutEdge(edge: E, node: N) {
        super.addOutEdge(edge, node)
        val successors = getReference(successorsReference)
        if (successors != null) {
            checkState(successors.add(node))
        }
    }

    companion object {

        fun <N, E> of(): DirectedMultiNetworkConnections<N, E> {
            return DirectedMultiNetworkConnections(
                    HashMap(INNER_CAPACITY, INNER_LOAD_FACTOR),
                    HashMap(INNER_CAPACITY, INNER_LOAD_FACTOR),
                    0)
        }

        fun <N, E> ofImmutable(
                inEdges: Map<E, N>, outEdges: Map<E, N>, selfLoopCount: Int): DirectedMultiNetworkConnections<N, E> {
            return DirectedMultiNetworkConnections(
                    ImmutableMap.copyOf(inEdges), ImmutableMap.copyOf(outEdges), selfLoopCount)
        }


        private fun <T> getReference(reference: Reference<T>?): T? {
            return reference?.get()
        }
    }
}
