package com.example.frames_demo.datadog.utils

import java.util.LinkedList
import java.util.Queue
import kotlin.math.max

class EvictingQueue<T> private constructor(
    maxSize: Int,
    private val delegate: LinkedList<T>
) : Queue<T> by delegate {

    /**
     * Secondary constructor that initializes the [EvictingQueue] with the given [maxSize].
     *
     * @param maxSize the maximum number of elements the queue can hold.
     */
    constructor(maxSize: Int = Int.MAX_VALUE) : this(maxSize, LinkedList())

    override val size: Int
        get() = delegate.size
    private val maxSize: Int = max(0, maxSize)

    /**
     * Adds the specified [element] to the end of this queue.
     *
     * If the queue has reached its maximum capacity, the first (oldest) element is evicted (removed)
     * before the new element is added.
     *
     * This queue should never throw [IllegalStateException] due to capacity restriction of the [delegate] because it
     * uses [java.util.Queue.offer] to insert elements.
     *
     * @param element the element to be added.
     *
     * @return `true` if this collection changed as a result of the call (as specified by [java.util.Collection.add])
     */
    override fun add(element: T): Boolean {
        return this.offer(element)
    }

    /**
     * Adds the specified [element] to the end of this queue.
     *
     * If the queue has reached its maximum capacity, the first (oldest) element is evicted (removed)
     * before the new element is added.
     *
     * @param element the element to be added.
     *
     * @return `true` if this collection changed as a result of the call
     */
    override fun offer(element: T): Boolean {
        if (maxSize == 0) return false
        if (size >= maxSize) {
            delegate.poll()
        }

        @Suppress("UnsafeThirdPartyFunctionCall") // can't have NPE here
        return delegate.offer(element)
    }

    /**
     * Adds all of the elements in the specified [elements] collection to the end of this queue.
     *
     * If the number of elements in [elements] is greater than or equal to [maxSize], the queue is cleared first,
     * and only the last [maxSize] elements from [elements] are added.
     *
     * Otherwise, if adding [elements] would exceed the maximum capacity, the required number of oldest elements
     * are evicted from the front of the queue to make room.
     *
     * @param elements the collection of elements to be added.
     * @return `true` if the queue changed as a result of the call.
     */
    override fun addAll(elements: Collection<T>): Boolean {
        return when {
            maxSize == 0 -> false

            elements.size >= maxSize -> {
                clear()
                for ((index, element) in elements.withIndex()) {
                    if (index < elements.size - maxSize) continue
                    delegate.add(element)
                }
                true
            }

            else -> {
                val spaceLeft = maxSize - size
                for (index in 0 until elements.size - spaceLeft) {
                    delegate.poll()
                }

                delegate.addAll(elements)
            }
        }
    }
}
