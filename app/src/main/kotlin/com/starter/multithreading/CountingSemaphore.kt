package com.starter.multithreading

import kotlin.concurrent.thread

class CountingSemaphore(val maxCount: Int) {
    var usedPermits = 0
    fun acquire() {
        synchronized(this) {
            while (usedPermits == maxCount) {
                wait()
            }
            usedPermits++
            notify()
        }
    }

    fun release() {
        synchronized(this) {
            while (usedPermits == 0) {
                wait()
            }
            usedPermits--
            notify()
        }
    }
}

fun main() {
    val countingSemaphore = CountingSemaphore(1)
    val t1 = thread(start = false) {
        (0..5).forEach {
            countingSemaphore.acquire()
            println("Ping $it")
        }

    }
    val t2 = thread(start = false) {
        (0..5).forEach {
            countingSemaphore.release()
            println("Pong $it")
        }
    }
    t1.start()
    t2.start()

    t1.join()
    t2.join()
}