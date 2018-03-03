/*
 * Copyright (C) 2014 The Guava Authors
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
 * A subinterface of [Network] which adds mutation methods. When mutation is not required,
 * users should prefer the [Network] interface.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
</E></N> */
@Beta
interface MutableNetwork<N, E> : Network<N, E> {

    /**
     * Adds `node` if it is not already present.
     *
     *
     * **Nodes must be unique**, just as `Map` keys must be. They must also be non-null.
     *
     * @return `true` if the network was modified as a result of this call
     */

    fun addNode(node: N): Boolean

    /**
     * Adds `edge` connecting `nodeU` to `nodeV`. In an undirected network, the edge
     * will also connect `nodeV` to `nodeU`.
     *
     *
     * **Edges must be unique**, just as `Map` keys must be. They must also be non-null.
     *
     *
     * If `nodeU` and `nodeV` are not already present in this graph, this method will
     * silently [add][.addNode] `nodeU` and `nodeV` to the graph.
     *
     *
     * If `edge` already connects `nodeU` to `nodeV` (in the specified order if
     * this network [.isDirected], else in any order), then this method will have no effect.
     *
     * @return `true` if the network was modified as a result of this call
     * @throws IllegalArgumentException if `edge` already exists and does not connect `nodeU` to `nodeV`, or if the introduction of the edge would violate [     ][.allowsParallelEdges] or [.allowsSelfLoops]
     */

    fun addEdge(nodeU: N, nodeV: N, edge: E): Boolean

    /**
     * Removes `node` if it is present; all edges incident to `node` will also be removed.
     *
     * @return `true` if the network was modified as a result of this call
     */

    fun removeNode(node: N): Boolean

    /**
     * Removes `edge` from this network, if it is present.
     *
     * @return `true` if the network was modified as a result of this call
     */

    fun removeEdge(edge: E): Boolean
}
