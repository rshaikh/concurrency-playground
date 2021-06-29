package com.starter

import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors

fun main() {
    val tickers = listOf("GOOG", "AMZN", "APPL", "GOOG", "AMZN", "APPL", "FOO")
    val handler = CoroutineExceptionHandler { context, ex ->
        println("Caught in ${context[CoroutineName]} ${ex.message}")
    }
    Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher().use { context ->
        runBlocking {
                measureTime {
                    val prices = mutableListOf<Deferred<String>>()
                    for (ticker in tickers) {
                        prices += async(context + CoroutineName(ticker) + handler + SupervisorJob()) { getQuote(ticker) }
                    }
                    try {
                        for (price in prices) {
                            println("price is : ${price.await()}")
                        }
                    } catch (ex: Exception) {
                        println("ERROR: ${ex.message}")
                    }
                }
        }
    }

}

fun getIpAddress(): String {
    return java.net.URL("https://api.ipify.org/").readText()
}

suspend fun getQuote(ticker: String): String {
    println("ticker $ticker, thread: ${Thread.currentThread()}")
    delay(1000)
    return java.net.URL("http://localhost:8000/$ticker").readText()
}

suspend fun measureTime(block: suspend () -> Unit) {
    val startTime = LocalDateTime.now()
    block()
    val endTime = LocalDateTime.now()
    println("${ChronoUnit.MILLIS.between(startTime, endTime)} millis")
}