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

import com.google.common.annotations.Beta


/**
 * A subinterface of [ValueGraph] which adds mutation methods. When mutation is not required,
 * users should prefer the [ValueGraph] interface.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 * @since 20.0
</V></N> */
@Beta
interface MutableValueGraph<N, V> : ValueGraph<N, V> {

    /**
     * Adds `node` if it is not already present.
     *
     *
     * **Nodes must be unique**, just as `Map` keys must be. They must also be non-null.
     *
     * @return `true` if the graph was modified as a result of this call
     */

    fun addNode(node: N): Boolean

    /**
     * Adds an edge connecting `nodeU` to `nodeV` if one is not already present; associate
     * that edge with `value`. In an undirected graph, the edge will also connect `nodeV`
     * to `nodeU`.
     *
     *
     * Values do not have to be unique. However, values must be non-null.
     *
     *
     * If `nodeU` and `nodeV` are not already present in this graph, this method will
     * silently [add][.addNode] `nodeU` and `nodeV` to the graph.
     *
     * @return the value previously associated with the edge connecting `nodeU` to `nodeV`, or null if there was no such edge.
     * @throws IllegalArgumentException if the introduction of the edge would violate [     ][.allowsSelfLoops]
     */

    fun putEdgeValue(nodeU: N, nodeV: N, value: V): V

    /**
     * Removes `node` if it is present; all edges incident to `node` will also be removed.
     *
     * @return `true` if the graph was modified as a result of this call
     */

    fun removeNode(node: N): Boolean

    /**
     * Removes the edge connecting `nodeU` to `nodeV`, if it is present.
     *
     * @return the value previously associated with the edge connecting `nodeU` to `nodeV`, or null if there was no such edge.
     */

    fun removeEdge(nodeU: N, nodeV: N): V
}
