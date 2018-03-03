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

import java.util.Optional

/**
 * A class to allow [Network] implementations to be backed by a provided delegate. This is not
 * currently planned to be released as a general-purpose forwarding class.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 */
internal abstract class ForwardingNetwork<N, E> : AbstractNetwork<N, E>() {

    override val isDirected: Boolean
        get() = delegate().isDirected

    protected abstract fun delegate(): Network<N, E>

    override fun nodes(): Set<N> {
        return delegate().nodes()
    }

    override fun edges(): Set<E> {
        return delegate().edges()
    }

    override fun allowsParallelEdges(): Boolean {
        return delegate().allowsParallelEdges()
    }

    override fun allowsSelfLoops(): Boolean {
        return delegate().allowsSelfLoops()
    }

    override fun nodeOrder(): ElementOrder<N> {
        return delegate().nodeOrder()
    }

    override fun edgeOrder(): ElementOrder<E> {
        return delegate().edgeOrder()
    }

    override fun adjacentNodes(node: N): Set<N> {
        return delegate().adjacentNodes(node)
    }

    override fun predecessors(node: N): Set<N> {
        return delegate().predecessors(node)
    }

    override fun successors(node: N): Set<N> {
        return delegate().successors(node)
    }

    override fun incidentEdges(node: N): Set<E> {
        return delegate().incidentEdges(node)
    }

    override fun inEdges(node: N): Set<E> {
        return delegate().inEdges(node)
    }

    override fun outEdges(node: N): Set<E> {
        return delegate().outEdges(node)
    }

    override fun incidentNodes(edge: E): EndpointPair<N> {
        return delegate().incidentNodes(edge)
    }

    override fun adjacentEdges(edge: E): Set<E> {
        return delegate().adjacentEdges(edge)
    }

    override fun degree(node: N): Int {
        return delegate().degree(node)
    }

    override fun inDegree(node: N): Int {
        return delegate().inDegree(node)
    }

    override fun outDegree(node: N): Int {
        return delegate().outDegree(node)
    }

    override fun edgesConnecting(nodeU: N, nodeV: N): Set<E> {
        return delegate().edgesConnecting(nodeU, nodeV)
    }

    override fun edgeConnecting(nodeU: N, nodeV: N): Optional<E> {
        return delegate().edgeConnecting(nodeU, nodeV)
    }

    override fun edgeConnectingOrNull(nodeU: N, nodeV: N): E? {
        return delegate().edgeConnectingOrNull(nodeU, nodeV)
    }

    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean {
        return delegate().hasEdgeConnecting(nodeU, nodeV)
    }
}
