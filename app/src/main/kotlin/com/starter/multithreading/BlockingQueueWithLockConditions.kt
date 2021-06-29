package com.starter.multithreading

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class BlockingQueueWithLockConditions(val capacity: Int) {
    var lock = ReentrantLock()
    val q = MyLinkList()
    val notFull = lock.newCondition()
    val notEmpty = lock.newCondition()

    fun enqueue(item: Int) {
        lock.lock()
        try{
            while(q.size == capacity) {
                notFull.await()
            }
            q.add(item)
            notEmpty.signalAll()
        }finally {
            lock.unlock()
        }
    }

    fun dequeue(): Int? {
        var item: Int? = null
        lock.lock()
        try{
            while(q.size == 0) {
                notEmpty.await()
            }
            item = q.remove()
            notFull.signalAll()
        }finally {
            lock.unlock()
        }
        return item
    }
}

fun main() {
    val q = BlockingQueueWithLockConditions(5)

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