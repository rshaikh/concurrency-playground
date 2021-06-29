package com.starter.multithreading

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class BlockingQueueWithMutex(val capacity: Int) {
    var lock = ReentrantLock()
    val array: Array<Int?> = arrayOfNulls<Int>(capacity)
    private var size = 0
    private var head = 0
    private var tail = 0

    fun enqueue(item: Int) {
        lock.lock()
        while (size == capacity) {
            lock.unlock()
            lock.lock()
        }
        if (tail == capacity) {
            tail = 0
        }
        array[tail] = item
        size++
        tail++

        lock.unlock()
    }

    fun dequeue(): Int? {
        lock.lock()
        while (size == 0) {
            lock.unlock()
            lock.lock()
        }
        if (head == capacity) {
            head = 0
        }
        val item: Int? = array[head]
        array[head] = null
        head++
        size--

        lock.unlock()

        return item
    }
}

fun main() {
    val q = BlockingQueueWithMutex(5)

    val t1 = thread(start = false, isDaemon = true) {
        (0..50).forEach {
            q.enqueue(it)
            println("enqueued $it")
        }
    }

    val t2 = thread(start = false, isDaemon = true) {
        (0..25).forEach {
            println("thread 2 dequeued ${q.dequeue()}")
        }
    }

    val t3 = thread(start = false, isDaemon = true) {
        (0..25).forEach {
            println("thread 3 dequeued ${q.dequeue()}")
        }
    }

    t1.start()
    Thread.sleep(4000)
    t2.start()
    t3.start()

    t1.join()
    t2.join()
    t3.join()
}