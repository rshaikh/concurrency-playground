package com.starter.multithreading

import java.util.*


object DemoThreadUnsafe {
    // We'll use this to randomly sleep our threads
    var random = Random(System.currentTimeMillis())
    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        // create object of unsafe counter
        val badCounter = ThreadUnsafeCounter()

        // setup thread1 to increment the badCounter 200 times
        val thread1 = Thread {
            for (i in 0..99) {
                badCounter.increment()
                sleepRandomlyForLessThan10Secs()
            }
        }

        // setup thread2 to decrement the badCounter 200 times
        val thread2 = Thread {
            for (i in 0..99) {
                badCounter.decrement()
                sleepRandomlyForLessThan10Secs()
            }
        }

        // run both threads
        thread1.start()
        thread2.start()

        // wait for t1 and t2 to complete.
        thread1.join()
        thread2.join()

        // print final value of counter
        badCounter.printFinalCounterValue()
    }

    fun sleepRandomlyForLessThan10Secs() {
        try {
            Thread.sleep(random.nextInt(10).toLong())
        } catch (ie: InterruptedException) {
        }
    }
}

internal class ThreadUnsafeCounter {
    var count = 0
    fun increment() {
        count++
    }

    fun decrement() {
        count--
    }

    fun printFinalCounterValue() {
        println("counter is: $count")
    }
}
