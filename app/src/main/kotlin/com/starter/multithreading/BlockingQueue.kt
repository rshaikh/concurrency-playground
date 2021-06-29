package com.starter.multithreading

import kotlin.concurrent.thread

class BlockingQueue(val capacity: Int) {
    var lock = Any()
    val array: Array<Int?> = arrayOfNulls<Int>(capacity)
    private var size = 0
    private var head = 0
    private var tail = 0

    fun enqueue(item: Int) {
        synchronized(lock) {
            while (size == capacity) {
                lock.wait()
            }
            if (tail == capacity) {
                tail = 0
            }
            array[tail] = item
            size++
            tail++

            lock.notifyAll()
        }
    }

    fun dequeue(): Int? {
        var item: Int?
        synchronized(lock) {
            while (size == 0) {
                lock.wait()
            }
            if (head == capacity) {
                head = 0
            }
            item = array[head]
            array[head] = null
            head++
            size--

            lock.notifyAll()
        }

        return item
    }
}

fun main() {
    val q = BlockingQueue(5)

    val t1 = thread(start = false) {
        (0..50).forEach {
            q.enqueue(it)
            println("enqueued $it")
        }
    }

    val t2 = thread(start = false) {
        (0..25).forEach {
            println("thread 2 dequeued ${q.dequeue()}")
        }
    }

    val t3 = thread(start = false) {
        (0..25).forEach {
            println("thread 3 dequeued ${q.dequeue()}")
        }
    }

    t1.start()
    Thread.sleep(4000)
    t2.start()

    t2.join()

    t3.start()
    t1.join()
    t3.join()
}