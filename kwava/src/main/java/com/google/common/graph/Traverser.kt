/*
 * Copyright (C) 2017 The Guava Authors
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

import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.collect.Iterators.singletonIterator

import com.google.common.annotations.Beta
import com.google.common.collect.AbstractIterator
import com.google.common.collect.Iterables
import com.google.common.collect.UnmodifiableIterator
import java.util.ArrayDeque
import java.util.Deque
import java.util.HashSet
import java.util.Queue

/**
 * Provides methods for traversing a graph.
 *
 * @author Jens Nyman
 * @param <N> Node parameter type
 * @since 23.1
</N> */
@Beta
abstract class Traverser<N>// Avoid subclasses outside of this class
private constructor() {

    /**
     * Returns an unmodifiable `Iterable` over the nodes reachable from `startNode`, in
     * the order of a breadth-first traversal. That is, all the nodes of depth 0 are returned, then
     * depth 1, then 2, and so on.
     *
     *
     * **Example:** The following graph with `startNode` `a` would return nodes in
     * the order `abcdef` (assuming successors are returned in alphabetical order).
     *
     * <pre>`b ---- a ---- d
     * |      |
     * |      |
     * e ---- c ---- f
    `</pre> *
     *
     *
     * The behavior of this method is undefined if the nodes, or the topology of the graph, change
     * while iteration is in progress.
     *
     *
     * The returned `Iterable` can be iterated over multiple times. Every iterator will
     * compute its next element on the fly. It is thus possible to limit the traversal to a certain
     * number of nodes as follows:
     *
     * <pre>`Iterables.limit(Traverser.forGraph(graph).breadthFirst(node), maxNumberOfNodes);
    `</pre> *
     *
     *
     * See [Wikipedia](https://en.wikipedia.org/wiki/Breadth-first_search) for more
     * info.
     *
     * @throws IllegalArgumentException if `startNode` is not an element of the graph
     */
    abstract fun breadthFirst(startNode: N): Iterable<N>

    /**
     * Returns an unmodifiable `Iterable` over the nodes reachable from `startNode`, in
     * the order of a depth-first pre-order traversal. "Pre-order" implies that nodes appear in the
     * `Iterable` in the order in which they are first visited.
     *
     *
     * **Example:** The following graph with `startNode` `a` would return nodes in
     * the order `abecfd` (assuming successors are returned in alphabetical order).
     *
     * <pre>`b ---- a ---- d
     * |      |
     * |      |
     * e ---- c ---- f
    `</pre> *
     *
     *
     * The behavior of this method is undefined if the nodes, or the topology of the graph, change
     * while iteration is in progress.
     *
     *
     * The returned `Iterable` can be iterated over multiple times. Every iterator will
     * compute its next element on the fly. It is thus possible to limit the traversal to a certain
     * number of nodes as follows:
     *
     * <pre>`Iterables.limit(
     * Traverser.forGraph(graph).depthFirstPreOrder(node), maxNumberOfNodes);
    `</pre> *
     *
     *
     * See [Wikipedia](https://en.wikipedia.org/wiki/Depth-first_search) for more info.
     *
     * @throws IllegalArgumentException if `startNode` is not an element of the graph
     */
    abstract fun depthFirstPreOrder(startNode: N): Iterable<N>

    /**
     * Returns an unmodifiable `Iterable` over the nodes reachable from `startNode`, in
     * the order of a depth-first post-order traversal. "Post-order" implies that nodes appear in the
     * `Iterable` in the order in which they are visited for the last time.
     *
     *
     * **Example:** The following graph with `startNode` `a` would return nodes in
     * the order `fcebda` (assuming successors are returned in alphabetical order).
     *
     * <pre>`b ---- a ---- d
     * |      |
     * |      |
     * e ---- c ---- f
    `</pre> *
     *
     *
     * The behavior of this method is undefined if the nodes, or the topology of the graph, change
     * while iteration is in progress.
     *
     *
     * The returned `Iterable` can be iterated over multiple times. Every iterator will
     * compute its next element on the fly. It is thus possible to limit the traversal to a certain
     * number of nodes as follows:
     *
     * <pre>`Iterables.limit(
     * Traverser.forGraph(graph).depthFirstPostOrder(node), maxNumberOfNodes);
    `</pre> *
     *
     *
     * See [Wikipedia](https://en.wikipedia.org/wiki/Depth-first_search) for more info.
     *
     * @throws IllegalArgumentException if `startNode` is not an element of the graph
     */
    abstract fun depthFirstPostOrder(startNode: N): Iterable<N>

    private class GraphTraverser<N> internal constructor(graph: SuccessorsFunction<N>) : Traverser<N>() {
        private val graph: SuccessorsFunction<N>

        init {
            this.graph = checkNotNull(graph)
        }

        override fun breadthFirst(startNode: N): Iterable<N> {
            checkNotNull(startNode)
            checkThatNodeIsInGraph(startNode)
            return object : Iterable<N> {
                override fun iterator(): Iterator<N> {
                    return BreadthFirstIterator(startNode)
                }
            }
        }

        override fun depthFirstPreOrder(startNode: N): Iterable<N> {
            checkNotNull(startNode)
            checkThatNodeIsInGraph(startNode)
            return object : Iterable<N> {
                override fun iterator(): Iterator<N> {
                    return DepthFirstIterator(startNode, Order.PREORDER)
                }
            }
        }

        override fun depthFirstPostOrder(startNode: N): Iterable<N> {
            checkNotNull(startNode)
            checkThatNodeIsInGraph(startNode)
            return object : Iterable<N> {
                override fun iterator(): Iterator<N> {
                    return DepthFirstIterator(startNode, Order.POSTORDER)
                }
            }
        }

        private fun checkThatNodeIsInGraph(startNode: N) {
            // successors() throws an IllegalArgumentException for nodes that are not an element of the
            // graph.
            graph.successors(startNode)
        }

        private inner class BreadthFirstIterator internal constructor(root: N) : UnmodifiableIterator<N>() {
            private val queue = ArrayDeque<N>()
            private val visited = HashSet<N>()

            init {
                queue.add(root)
                visited.add(root)
            }

            override fun hasNext(): Boolean {
                return !queue.isEmpty()
            }

            override fun next(): N {
                val current = queue.remove()
                for (neighbor in graph.successors(current)) {
                    if (visited.add(neighbor)) {
                        queue.add(neighbor)
                    }
                }
                return current
            }
        }

        private inner class DepthFirstIterator internal constructor(root: N, private val order: Order) : AbstractIterator<N>() {
            private val stack = ArrayDeque<NodeAndSuccessors>()
            private val visited = HashSet<N>()

            init {
                // our invariant is that in computeNext we call next on the iterator at the top first, so we
                // need to start with one additional item on that iterator
                stack.push(withSuccessors(root))
            }

            override fun computeNext(): N? {
                while (true) {
                    if (stack.isEmpty()) {
                        return endOfData()
                    }
                    val node = stack.first
                    val firstVisit = visited.add(node.node)
                    val lastVisit = !node.successorIterator.hasNext()
                    val produceNode = firstVisit && order == Order.PREORDER || lastVisit && order == Order.POSTORDER
                    if (lastVisit) {
                        stack.pop()
                    } else {
                        // we need to push a neighbor, but only if we haven't already seen it
                        val successor = node.successorIterator.next()
                        if (!visited.contains(successor)) {
                            stack.push(withSuccessors(successor))
                        }
                    }
                    if (produceNode) {
                        return node.node
                    }
                }
            }

            internal fun withSuccessors(node: N): NodeAndSuccessors {
                return NodeAndSuccessors(node, graph.successors(node))
            }

            /** A simple tuple of a node and a partially iterated [Iterator] of its successors.  */
            private inner class NodeAndSuccessors internal constructor(internal val node: N, successors: Iterable<N>) {
                internal val successorIterator: Iterator<N>

                init {
                    this.successorIterator = successors.iterator()
                }
            }
        }
    }

    private class TreeTraverser<N> internal constructor(tree: SuccessorsFunction<N>) : Traverser<N>() {
        private val tree: SuccessorsFunction<N>

        init {
            this.tree = checkNotNull(tree)
        }

        override fun breadthFirst(startNode: N): Iterable<N> {
            checkNotNull(startNode)
            checkThatNodeIsInTree(startNode)
            return object : Iterable<N> {
                override fun iterator(): Iterator<N> {
                    return BreadthFirstIterator(startNode)
                }
            }
        }

        override fun depthFirstPreOrder(startNode: N): Iterable<N> {
            checkNotNull(startNode)
            checkThatNodeIsInTree(startNode)
            return object : Iterable<N> {
                override fun iterator(): Iterator<N> {
                    return DepthFirstPreOrderIterator(startNode)
                }
            }
        }

        override fun depthFirstPostOrder(startNode: N): Iterable<N> {
            checkNotNull(startNode)
            checkThatNodeIsInTree(startNode)
            return object : Iterable<N> {
                override fun iterator(): Iterator<N> {
                    return DepthFirstPostOrderIterator(startNode)
                }
            }
        }

        private fun checkThatNodeIsInTree(startNode: N) {
            // successors() throws an IllegalArgumentException for nodes that are not an element of the
            // graph.
            tree.successors(startNode)
        }

        private inner class BreadthFirstIterator internal constructor(root: N) : UnmodifiableIterator<N>() {
            private val queue = ArrayDeque<N>()

            init {
                queue.add(root)
            }

            override fun hasNext(): Boolean {
                return !queue.isEmpty()
            }

            override fun next(): N {
                val current = queue.remove()
                Iterables.addAll(queue, tree.successors(current))
                return current
            }
        }

        private inner class DepthFirstPreOrderIterator internal constructor(root: N) : UnmodifiableIterator<N>() {
            private val stack = ArrayDeque<Iterator<N>>()

            init {
                stack.addLast(singletonIterator(checkNotNull(root)))
            }

            override fun hasNext(): Boolean {
                return !stack.isEmpty()
            }

            override fun next(): N {
                val iterator = stack.last // throws NoSuchElementException if empty
                val result = checkNotNull(iterator.next())
                if (!iterator.hasNext()) {
                    stack.removeLast()
                }
                val childIterator = tree.successors(result).iterator()
                if (childIterator.hasNext()) {
                    stack.addLast(childIterator)
                }
                return result
            }
        }

        private inner class DepthFirstPostOrderIterator internal constructor(root: N) : AbstractIterator<N>() {
            private val stack = ArrayDeque<NodeAndChildren>()

            init {
                stack.addLast(withChildren(root))
            }

            override fun computeNext(): N? {
                while (!stack.isEmpty()) {
                    val top = stack.last
                    if (top.childIterator.hasNext()) {
                        val child = top.childIterator.next()
                        stack.addLast(withChildren(child))
                    } else {
                        stack.removeLast()
                        return top.node
                    }
                }
                return endOfData()
            }

            internal fun withChildren(node: N): NodeAndChildren {
                return NodeAndChildren(node, tree.successors(node))
            }

            /** A simple tuple of a node and a partially iterated [Iterator] of its children.  */
            private inner class NodeAndChildren internal constructor(internal val node: N, children: Iterable<N>) {
                internal val childIterator: Iterator<N>

                init {
                    this.childIterator = children.iterator()
                }
            }
        }
    }

    private enum class Order {
        PREORDER,
        POSTORDER
    }

    companion object {

        /**
         * Creates a new traverser for the given general `graph`.
         *
         *
         * If `graph` is known to be tree-shaped, consider using [ ][.forTree] instead.
         *
         *
         * **Performance notes**
         *
         *
         *  * Traversals require *O(n)* time (where *n* is the number of nodes reachable from
         * the start node), assuming that the node objects have *O(1)* `equals()` and
         * `hashCode()` implementations.
         *  * While traversing, the traverser will use *O(n)* space (where *n* is the number
         * of nodes that have thus far been visited), plus *O(H)* space (where *H* is the
         * number of nodes that have been seen but not yet visited, that is, the "horizon").
         *
         *
         * @param graph [SuccessorsFunction] representing a general graph that may have cycles.
         */
        fun <N> forGraph(graph: SuccessorsFunction<N>): Traverser<N> {
            checkNotNull(graph)
            return GraphTraverser(graph)
        }

        /**
         * Creates a new traverser for a directed acyclic graph that has at most one path from the start
         * node to any node reachable from the start node, such as a tree.
         *
         *
         * Providing graphs that don't conform to the above description may lead to:
         *
         *
         *  * Traversal not terminating (if the graph has cycles)
         *  * Nodes being visited multiple times (if multiple paths exist from the start node to any
         * node reachable from it)
         *
         *
         * In these cases, use [.forGraph] instead.
         *
         *
         * **Performance notes**
         *
         *
         *  * Traversals require *O(n)* time (where *n* is the number of nodes reachable from
         * the start node).
         *  * While traversing, the traverser will use *O(H)* space (where *H* is the number
         * of nodes that have been seen but not yet visited, that is, the "horizon").
         *
         *
         *
         * **Examples**
         *
         *
         * This is a valid input graph (all edges are directed facing downwards):
         *
         * <pre>`a     b      c
         * / \   / \     |
         * /   \ /   \    |
         * d     e     f   g
         * |
         * |
         * h
        `</pre> *
         *
         *
         * This is **not** a valid input graph (all edges are directed facing downwards):
         *
         * <pre>`a     b
         * / \   / \
         * /   \ /   \
         * c     d     e
         * \   /
         * \ /
         * f
        `</pre> *
         *
         *
         * because there are two paths from `b` to `f` (`b->d->f` and `b->e->f`).
         *
         *
         * **Note on binary trees**
         *
         *
         * This method can be used to traverse over a binary tree. Given methods `leftChild(node)` and `rightChild(node)`, this method can be called as
         *
         * <pre>`Traverser.forTree(node -> ImmutableList.of(leftChild(node), rightChild(node)));
        `</pre> *
         *
         * @param tree [SuccessorsFunction] representing a directed acyclic graph that has at most
         * one path between any two nodes
         */
        fun <N> forTree(tree: SuccessorsFunction<N>): Traverser<N> {
            checkNotNull(tree)
            if (tree is BaseGraph<*>) {
                checkArgument((tree as BaseGraph<*>).isDirected, "Undirected graphs can never be trees.")
            }
            if (tree is Network<*, *>) {
                checkArgument((tree as Network<*, *>).isDirected, "Undirected networks can never be trees.")
            }
            return TreeTraverser(tree)
        }
    }
}
