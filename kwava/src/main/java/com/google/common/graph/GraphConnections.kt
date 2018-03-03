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
 * An interface for representing and manipulating an origin node's adjacent nodes and edge values in
 * a [Graph].
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
</V></N> */
internal interface GraphConnections<N, V> {

    fun adjacentNodes(): Set<N>

    fun predecessors(): Set<N>

    fun successors(): Set<N>

    /**
     * Returns the value associated with the edge connecting the origin node to `node`, or null
     * if there is no such edge.
     */

    fun value(node: N): V

    /** Remove `node` from the set of predecessors.  */
    fun removePredecessor(node: N)

    /**
     * Remove `node` from the set of successors. Returns the value previously associated with
     * the edge connecting the two nodes.
     */

    fun removeSuccessor(node: N): V

    /**
     * Add `node` as a predecessor to the origin node. In the case of an undirected graph, it
     * also becomes a successor. Associates `value` with the edge connecting the two nodes.
     */
    fun addPredecessor(node: N, value: V)

    /**
     * Add `node` as a successor to the origin node. In the case of an undirected graph, it also
     * becomes a predecessor. Associates `value` with the edge connecting the two nodes. Returns
     * the value previously associated with the edge connecting the two nodes.
     */

    fun addSuccessor(node: N, value: V): V
}
