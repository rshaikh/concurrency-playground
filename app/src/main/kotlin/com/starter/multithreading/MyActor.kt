package com.starter.multithreading

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class MyActor(var counter: Int) {
    private val q = ConcurrentLinkedQueue<String>()
    init {
        thread(start = true, isDaemon = true) {
            while (true) {
                q.poll()?.let {
                    when(it) {
                        "INCREMENT_BY_1" -> {
                            counter++
                        }
                        "DECREMENT_BY_1" -> {
                            counter--
                        }
                        else -> {
                            println("Unknown message")
                        }
                    }
                }
            }
        }
    }

    fun incrementByOne() {
//        counter++
        q.add("INCREMENT_BY_1")
    }

    fun decrementByOne() {
//        counter--
        q.add("DECREMENT_BY_1")
    }
}

fun main() {
    val actor = MyActor(0)
    val threadsIncrementingValue = (1..200).map {
        thread(start = true) {
            Thread.sleep(1000)
            actor.incrementByOne()
        }
    }
    val threadsDecrementingValue = (1..100).map {
        thread(start = true) {
            Thread.sleep(2000)
            actor.decrementByOne()
        }
    }
    threadsIncrementingValue.forEach {
        it.join()
    }
    threadsDecrementingValue.forEach {
        it.join()
    }
    // Counter value should be exactly 100
    println("Counter is: ${actor.counter}")
}