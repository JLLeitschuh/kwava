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

/**
 * A class to allow [Graph] implementations to be backed by a [BaseGraph]. This is not
 * currently planned to be released as a general-purpose forwarding class.
 *
 * @author James Sexton
 */
internal abstract class ForwardingGraph<N> : AbstractGraph<N>() {

    override val isDirected: Boolean
        get() = delegate().isDirected

    protected abstract fun delegate(): BaseGraph<N>

    override fun nodes(): Set<N> {
        return delegate().nodes()
    }

    /**
     * Defer to [AbstractGraph.edges] (based on [.successors]) for full edges()
     * implementation.
     */
    override fun edgeCount(): Long {
        return delegate().edges().size.toLong()
    }

    override fun allowsSelfLoops(): Boolean {
        return delegate().allowsSelfLoops()
    }

    override fun nodeOrder(): ElementOrder<N> {
        return delegate().nodeOrder()
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

    override fun degree(node: N): Int {
        return delegate().degree(node)
    }

    override fun inDegree(node: N): Int {
        return delegate().inDegree(node)
    }

    override fun outDegree(node: N): Int {
        return delegate().outDegree(node)
    }

    override fun hasEdgeConnecting(nodeU: N, nodeV: N): Boolean {
        return delegate().hasEdgeConnecting(nodeU, nodeV)
    }
}
