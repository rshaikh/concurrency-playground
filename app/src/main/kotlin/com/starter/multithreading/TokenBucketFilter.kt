package com.starter.multithreading

import kotlin.concurrent.thread

class TokenBucketFilter(val maxTokens: Long) {
    private var lastRequestTime = System.currentTimeMillis()
    private var possibleTokens: Long = 0
    fun getToken() {
        synchronized(this) {
            possibleTokens += (System.currentTimeMillis() - lastRequestTime) / 1000
            if (possibleTokens > maxTokens) {
                possibleTokens = maxTokens
            }
            if (possibleTokens == 0L) {
                Thread.sleep(1000)
            } else {
                possibleTokens--
            }
            lastRequestTime = System.currentTimeMillis()
            println("Granting ${Thread.currentThread().name} token at ${System.currentTimeMillis() / 1000}")
        }
    }
}

fun main() {
    val tokenBucketFilter = TokenBucketFilter(5)
    Thread.sleep(10000)
    val threads = (0..10).map {
        thread(start = false) {
            tokenBucketFilter.getToken()
        }
    }
    threads.forEach { it.start()}
    threads.forEach { it.join()}
}