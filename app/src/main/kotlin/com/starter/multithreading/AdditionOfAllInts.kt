package com.starter.multithreading

import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

val MAX_NUM = Int.MAX_VALUE.toLong()

class SumUpExample(private val start:Long, private val end: Long) {
    var total: Long = 0
    fun add() {
        total = (start..end).sum()
    }
}
fun main() {
    val timeTwoThreads = measureTimeMillis {
        val sumUpExample1 = SumUpExample(1, MAX_NUM / 2)
        val sumUpExample2 = SumUpExample(MAX_NUM / 2 + 1, MAX_NUM)
        val thread1 = thread {
            sumUpExample1.add()
        }
        val thread2 = thread {
            sumUpExample2.add()
        }
        thread1.join()
        thread2.join()
        println("Total is ${sumUpExample1.total + sumUpExample2.total}")
    }
    println("Time taken for two threads: $timeTwoThreads")

    val timeSingleThread = measureTimeMillis {
        val sumUpExample1 = SumUpExample(1, MAX_NUM).add()
        println("Total is $sumUpExample1")
    }
    println("Time taken for single thread: $timeSingleThread")

}