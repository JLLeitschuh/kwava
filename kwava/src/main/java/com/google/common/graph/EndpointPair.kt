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
import com.google.common.graph.GraphConstants.NOT_AVAILABLE_ON_UNDIRECTED

import com.google.common.annotations.Beta
import com.google.common.base.Objects
import com.google.common.collect.Iterators
import com.google.common.collect.UnmodifiableIterator
import com.google.errorprone.annotations.Immutable


/**
 * An immutable pair representing the two endpoints of an edge in a graph. The [EndpointPair]
 * of a directed edge is an ordered pair of nodes ([.source] and [.target]). The
 * [EndpointPair] of an undirected edge is an unordered pair of nodes ([.nodeU] and
 * [.nodeV]).
 *
 *
 * The edge is a self-loop if, and only if, the two endpoints are equal.
 *
 * @author James Sexton
 * @since 20.0
 */
@Beta
@Immutable(containerOf = { "N" })
abstract class EndpointPair<N> private constructor(nodeU: N, nodeV: N) : Iterable<N> {
    private val nodeU: N
    private val nodeV: N

    /**
     * Returns `true` if this [EndpointPair] is an ordered pair (i.e. represents the
     * endpoints of a directed edge).
     */
    abstract val isOrdered: Boolean

    init {
        this.nodeU = checkNotNull(nodeU)
        this.nodeV = checkNotNull(nodeV)
    }

    /**
     * If this [EndpointPair] [.isOrdered], returns the node which is the source.
     *
     * @throws UnsupportedOperationException if this [EndpointPair] is not ordered
     */
    abstract fun source(): N

    /**
     * If this [EndpointPair] [.isOrdered], returns the node which is the target.
     *
     * @throws UnsupportedOperationException if this [EndpointPair] is not ordered
     */
    abstract fun target(): N

    /**
     * If this [EndpointPair] [.isOrdered] returns the [.source]; otherwise,
     * returns an arbitrary (but consistent) endpoint of the origin edge.
     */
    fun nodeU(): N {
        return nodeU
    }

    /**
     * Returns the node [adjacent][.adjacentNode] to [.nodeU] along the origin
     * edge. If this [EndpointPair] [.isOrdered], this is equal to [.target].
     */
    fun nodeV(): N {
        return nodeV
    }

    /**
     * Returns the node that is adjacent to `node` along the origin edge.
     *
     * @throws IllegalArgumentException if this [EndpointPair] does not contain `node`
     */
    fun adjacentNode(node: Any): N {
        return if (node == nodeU) {
            nodeV
        } else if (node == nodeV) {
            nodeU
        } else {
            throw IllegalArgumentException("EndpointPair " + this + " does not contain node " + node)
        }
    }

    /** Iterates in the order [.nodeU], [.nodeV].  */
    override fun iterator(): UnmodifiableIterator<N> {
        return Iterators.forArray(nodeU, nodeV)
    }

    /**
     * Two ordered [EndpointPair]s are equal if their [.source] and [.target]
     * are equal. Two unordered [EndpointPair]s are equal if they contain the same nodes. An
     * ordered [EndpointPair] is never equal to an unordered [EndpointPair].
     */
    abstract override fun equals(obj: Any?): Boolean

    /**
     * The hashcode of an ordered [EndpointPair] is equal to `Objects.hashCode(source(),
     * target())`. The hashcode of an unordered [EndpointPair] is equal to `nodeU().hashCode() + nodeV().hashCode()`.
     */
    abstract override fun hashCode(): Int

    private class Ordered<N> private constructor(source: N, target: N) : EndpointPair<N>(source, target) {

        override val isOrdered: Boolean
            get() = true

        override fun source(): N {
            return nodeU()
        }

        override fun target(): N {
            return nodeV()
        }

        override fun equals(obj: Any?): Boolean {
            if (obj === this) {
                return true
            }
            if (obj !is EndpointPair<*>) {
                return false
            }

            val other = obj as EndpointPair<*>?
            return if (isOrdered != other!!.isOrdered) {
                false
            } else source() == other.source() && target() == other.target()

        }

        override fun hashCode(): Int {
            return Objects.hashCode(source(), target())
        }

        override fun toString(): String {
            return "<" + source() + " -> " + target() + ">"
        }
    }

    private class Unordered<N> private constructor(nodeU: N, nodeV: N) : EndpointPair<N>(nodeU, nodeV) {

        override val isOrdered: Boolean
            get() = false

        override fun source(): N {
            throw UnsupportedOperationException(NOT_AVAILABLE_ON_UNDIRECTED)
        }

        override fun target(): N {
            throw UnsupportedOperationException(NOT_AVAILABLE_ON_UNDIRECTED)
        }

        override fun equals(obj: Any?): Boolean {
            if (obj === this) {
                return true
            }
            if (obj !is EndpointPair<*>) {
                return false
            }

            val other = obj as EndpointPair<*>?
            if (isOrdered != other!!.isOrdered) {
                return false
            }

            // Equivalent to the following simple implementation:
            // boolean condition1 = nodeU().equals(other.nodeU()) && nodeV().equals(other.nodeV());
            // boolean condition2 = nodeU().equals(other.nodeV()) && nodeV().equals(other.nodeU());
            // return condition1 || condition2;
            return if (nodeU() == other.nodeU()) { // check condition1
                // Here's the tricky bit. We don't have to explicitly check for condition2 in this case.
                // Why? The second half of condition2 requires that nodeV equals other.nodeU.
                // We already know that nodeU equals other.nodeU. Combined with the earlier statement,
                // and the transitive property of equality, this implies that nodeU equals nodeV.
                // If nodeU equals nodeV, condition1 == condition2, so checking condition1 is sufficient.
                nodeV() == other.nodeV()
            } else nodeU() == other.nodeV() && nodeV() == other.nodeU()
// check condition2
        }

        override fun hashCode(): Int {
            return nodeU().hashCode() + nodeV().hashCode()
        }

        override fun toString(): String {
            return "[" + nodeU() + ", " + nodeV() + "]"
        }
    }

    companion object {

        /** Returns an [EndpointPair] representing the endpoints of a directed edge.  */
        fun <N> ordered(source: N, target: N): EndpointPair<N> {
            return Ordered(source, target)
        }

        /** Returns an [EndpointPair] representing the endpoints of an undirected edge.  */
        fun <N> unordered(nodeU: N, nodeV: N): EndpointPair<N> {
            // Swap nodes on purpose to prevent callers from relying on the "ordering" of an unordered pair.
            return Unordered(nodeV, nodeU)
        }

        /** Returns an [EndpointPair] representing the endpoints of an edge in `graph`.  */
        internal fun <N> of(graph: Graph<*>, nodeU: N, nodeV: N): EndpointPair<N> {
            return if (graph.isDirected) ordered(nodeU, nodeV) else unordered(nodeU, nodeV)
        }

        /** Returns an [EndpointPair] representing the endpoints of an edge in `network`.  */
        internal fun <N> of(network: Network<*, *>, nodeU: N, nodeV: N): EndpointPair<N> {
            return if (network.isDirected) ordered(nodeU, nodeV) else unordered(nodeU, nodeV)
        }
    }
}
