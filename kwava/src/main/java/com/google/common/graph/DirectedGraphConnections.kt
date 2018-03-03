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
import com.google.common.base.Preconditions.checkState
import com.google.common.graph.GraphConstants.INNER_CAPACITY
import com.google.common.graph.GraphConstants.INNER_LOAD_FACTOR
import com.google.common.graph.Graphs.checkNonNegative
import com.google.common.graph.Graphs.checkPositive

import com.google.common.collect.AbstractIterator
import com.google.common.collect.ImmutableMap
import com.google.common.collect.UnmodifiableIterator
import java.util.AbstractSet
import java.util.Collections
import java.util.HashMap
import kotlin.collections.Map.Entry


/**
 * An implementation of [GraphConnections] for directed graphs.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <V> Value parameter type
</V></N> */
internal class DirectedGraphConnections<N, V> private constructor(
        adjacentNodeValues: Map<N, Any>, predecessorCount: Int, successorCount: Int) : GraphConnections<N, V> {

    // Every value in this map must either be an instance of PredAndSucc with a successorValue of
    // type V, PRED (representing predecessor), or an instance of type V (representing successor).
    private val adjacentNodeValues: MutableMap<N, Any>

    private var predecessorCount: Int = 0
    private var successorCount: Int = 0

    /**
     * A wrapper class to indicate a node is both a predecessor and successor while still providing
     * the successor value.
     */
    private class PredAndSucc internal constructor(private val successorValue: Any)

    init {
        this.adjacentNodeValues = checkNotNull(adjacentNodeValues)
        this.predecessorCount = checkNonNegative(predecessorCount)
        this.successorCount = checkNonNegative(successorCount)
        checkState(
                predecessorCount <= adjacentNodeValues.size && successorCount <= adjacentNodeValues.size)
    }

    override fun adjacentNodes(): Set<N> {
        return Collections.unmodifiableSet(adjacentNodeValues.keys)
    }

    override fun predecessors(): Set<N> {
        return object : AbstractSet<N>() {
            override fun iterator(): UnmodifiableIterator<N> {
                val entries = adjacentNodeValues.entries.iterator()
                return object : AbstractIterator<N>() {
                    override fun computeNext(): N? {
                        while (entries.hasNext()) {
                            val entry = entries.next()
                            if (isPredecessor(entry.value)) {
                                return entry.key
                            }
                        }
                        return endOfData()
                    }
                }
            }

            override fun size(): Int {
                return predecessorCount
            }

            override operator fun contains(obj: Any?): Boolean {
                return isPredecessor(adjacentNodeValues.get(obj))
            }
        }
    }

    override fun successors(): Set<N> {
        return object : AbstractSet<N>() {
            override fun iterator(): UnmodifiableIterator<N> {
                val entries = adjacentNodeValues.entries.iterator()
                return object : AbstractIterator<N>() {
                    override fun computeNext(): N? {
                        while (entries.hasNext()) {
                            val entry = entries.next()
                            if (isSuccessor(entry.value)) {
                                return entry.key
                            }
                        }
                        return endOfData()
                    }
                }
            }

            override fun size(): Int {
                return successorCount
            }

            override operator fun contains(obj: Any?): Boolean {
                return isSuccessor(adjacentNodeValues[obj])
            }
        }
    }

    override fun value(node: N): V? {
        val value = adjacentNodeValues[node]
        if (value === PRED) {
            return null
        }
        return if (value is PredAndSucc) {
            value.successorValue as V
        } else value as V
    }

    override fun removePredecessor(node: N) {
        val previousValue = adjacentNodeValues[node]
        if (previousValue === PRED) {
            adjacentNodeValues.remove(node)
            checkNonNegative(--predecessorCount)
        } else if (previousValue is PredAndSucc) {
            adjacentNodeValues[node] = previousValue.successorValue
            checkNonNegative(--predecessorCount)
        }
    }

    override fun removeSuccessor(node: Any): V? {
        val previousValue = adjacentNodeValues.get(node)
        if (previousValue == null || previousValue === PRED) {
            return null
        } else if (previousValue is PredAndSucc) {
            adjacentNodeValues[node as N] = PRED
            checkNonNegative(--successorCount)
            return previousValue.successorValue as V
        } else { // successor
            adjacentNodeValues.remove(node)
            checkNonNegative(--successorCount)
            return previousValue as V?
        }
    }

    override fun addPredecessor(node: N, unused: V) {
        val previousValue = adjacentNodeValues.put(node, PRED)
        if (previousValue == null) {
            checkPositive(++predecessorCount)
        } else if (previousValue is PredAndSucc) {
            // Restore previous PredAndSucc object.
            adjacentNodeValues[node] = previousValue
        } else if (previousValue !== PRED) { // successor
            // Do NOT use method parameter value 'unused'. In directed graphs, successors store the value.
            adjacentNodeValues[node] = PredAndSucc(previousValue)
            checkPositive(++predecessorCount)
        }
    }

    override fun addSuccessor(node: N, value: V): V? {
        val previousValue = adjacentNodeValues.put(node, value)
        if (previousValue == null) {
            checkPositive(++successorCount)
            return null
        } else if (previousValue is PredAndSucc) {
            adjacentNodeValues[node] = PredAndSucc(value)
            return previousValue.successorValue as V
        } else if (previousValue === PRED) {
            adjacentNodeValues[node] = PredAndSucc(value)
            checkPositive(++successorCount)
            return null
        } else { // successor
            return previousValue as V?
        }
    }

    companion object {

        private val PRED = Any()

        fun <N, V> of(): DirectedGraphConnections<N, V> {
            // We store predecessors and successors in the same map, so double the initial capacity.
            val initialCapacity = INNER_CAPACITY * 2
            return DirectedGraphConnections(
                    HashMap(initialCapacity, INNER_LOAD_FACTOR), 0, 0)
        }

        fun <N, V> ofImmutable(
                predecessors: Set<N>, successorValues: Map<N, V>): DirectedGraphConnections<N, V> {
            val adjacentNodeValues = HashMap<N, Any>()
            adjacentNodeValues.putAll(successorValues)
            for (predecessor in predecessors) {
                val value = adjacentNodeValues.put(predecessor, PRED)
                if (value != null) {
                    adjacentNodeValues[predecessor] = PredAndSucc(value)
                }
            }
            return DirectedGraphConnections(
                    ImmutableMap.copyOf(adjacentNodeValues), predecessors.size, successorValues.size)
        }

        private fun isPredecessor(value: Any): Boolean {
            return value === PRED || value is PredAndSucc
        }

        private fun isSuccessor(value: Any?): Boolean {
            return value !== PRED && value != null
        }
    }
}
