package com.starter.multithreading

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class ConcurrentCacheWithExpiry(private val expiryInMillis: Int) {
//    private val entries = mutableMapOf<String, TimedEntry>()
    private val entries = ConcurrentHashMap<String, TimedEntry>()

    fun computeAndCache(key: String, compute: () -> String): String? {
        val timedEntry = entries[key]
        if(timedEntry == null || timedEntry.isExpired(expiryInMillis)) {
            //TODO: remove and compute is not atomic
            entries.remove(key)
            return entries.computeIfAbsent(key) {
                println("message from ${Thread.currentThread().name} not found.. making the call ${System.currentTimeMillis() / 1000}")
                TimedEntry(compute())
            }.value
        }
        return timedEntry.value
    }

    /*fun shutDown() {
        executorService.shutdown()
    }*/
}

class TimedEntry(val value: String) {
    private val lastAccessedTimeStamp = System.currentTimeMillis()

    fun isExpired(expiryInMillis: Int): Boolean {
        return System.currentTimeMillis() - lastAccessedTimeStamp > expiryInMillis
    }
}

fun main() {
    val cache = ConcurrentCacheWithExpiry(2000)
    fun getRandomNumberUsingNextInt(min: Int, max: Int): Int {
        val random = Random()
        return random.nextInt(max - min) + min;
    }

    val t1 = thread(start = false, name = "t1") {
        val result = cache.computeAndCache("key") {
            println("t1 executed")
            "value cached from: t1"
        }
        println("result from t1: $result")
    }

    val t2 = thread(start = false, name = "t2") {
        val result = cache.computeAndCache("key") {
            println("this should not be printed")
            "value cached from: t2"
        }
        println("result from t2: $result")
        return@thread
    }

    val t3 = thread(start = false, name = "t3") {
        val result = cache.computeAndCache("key") {
            println("t3 executed")
            "value cached from: t3"
        }
        println("result from t3: $result")
    }
    val threads = (0..100).map {
        thread(start = false, name = "T-$it") {
            Thread.sleep(getRandomNumberUsingNextInt(100, 200).toLong())
            val result = cache.computeAndCache("key") {
                println("T-$it executed")
                "value cached from: T-$it"
            }
            println("result from T-$it: $result timestamp: ${System.currentTimeMillis()}")
        }
    }

    t1.start()
    Thread.sleep(1000)
    t2.start()
    // it should make a fresh call again as previous value is expired
    Thread.sleep(1000)
    t3.start()

    t1.join()
    t2.join()
    t3.join()

    threads.forEach {
        it.start()
        it.join()
    }
}
