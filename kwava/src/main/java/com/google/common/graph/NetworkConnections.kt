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
 * An interface for representing and manipulating an origin node's adjacent nodes and incident edges
 * in a [Network].
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
</E></N> */
internal interface NetworkConnections<N, E> {

    fun adjacentNodes(): Set<N>

    fun predecessors(): Set<N>

    fun successors(): Set<N>

    fun incidentEdges(): Set<E>

    fun inEdges(): Set<E>

    fun outEdges(): Set<E>

    /**
     * Returns the set of edges connecting the origin node to `node`. For networks without
     * parallel edges, this set cannot be of size greater than one.
     */
    fun edgesConnecting(node: N): Set<E>

    /**
     * Returns the node that is adjacent to the origin node along `edge`.
     *
     *
     * In the directed case, `edge` is assumed to be an outgoing edge.
     */
    fun adjacentNode(edge: E): N

    /**
     * Remove `edge` from the set of incoming edges. Returns the former predecessor node.
     *
     *
     * In the undirected case, returns `null` if `isSelfLoop` is true.
     */

    fun removeInEdge(edge: E, isSelfLoop: Boolean): N

    /** Remove `edge` from the set of outgoing edges. Returns the former successor node.  */

    fun removeOutEdge(edge: E): N

    /**
     * Add `edge` to the set of incoming edges. Implicitly adds `node` as a predecessor.
     */
    fun addInEdge(edge: E, node: N, isSelfLoop: Boolean)

    /** Add `edge` to the set of outgoing edges. Implicitly adds `node` as a successor.  */
    fun addOutEdge(edge: E, node: N)
}
