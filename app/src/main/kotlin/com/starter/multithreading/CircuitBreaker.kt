package com.starter.multithreading

import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class CircuitBreaker(
    val failedThreshold: Int = 3,
    val successThreshold: Int = 3,
    private var initialStatus: CircuitBreakerStatus = CircuitBreakerStatus.CLOSE,
    val shouldRetryAfter: Duration = Duration.ofSeconds(1)
) {
    private var lastAttempt = System.currentTimeMillis()
    var stats = AtomicReference(
        CircuitBreakerStats(
            failedCount = 0,
            successCountAfterPartialFailure = 0,
            failureCountAfterPartialFailure = 0,
            status = initialStatus
        )
    )

    fun call(block: () -> Any): CircuitBreakerResponse {
        when (stats.get().status) {
            CircuitBreakerStatus.OPEN -> {
                return if (shouldRetry()) {
                    try {
                        val response = executeBlock(block)
                        registerPartialSuccess()
                        CircuitBreakerResponse.success(response)
                    } catch (ex: Exception) {
                        registerPartialFailure()
                        CircuitBreakerResponse.failure(ex)
                    }
                } else {
                    CircuitBreakerResponse.defaultResponse()
                }
            }
            CircuitBreakerStatus.PARTIAL_CLOSE -> return try {
                val response = block()
                registerPartialSuccess()
                if (shouldCloseAfterPartialClose()) {
                    closeCircuit()
                }
                CircuitBreakerResponse.success(response)
            } catch (ex: Exception) {
                registerPartialFailure()
                if (shouldOpenAfterPartialClose()) {
                    openCircuit()
                }
                CircuitBreakerResponse.failure(ex)
            }
            else -> return try {
                val response = block()
                CircuitBreakerResponse.success(response)
            } catch (ex: Exception) {
                registerFailureWhenCircuitWasOpen()
                CircuitBreakerResponse.failure(ex)
            }
        }
    }

    private fun shouldRetry() = (System.currentTimeMillis() - lastAttempt) / 1000 >= shouldRetryAfter.seconds

    private fun executeBlock(block: () -> Any): Any {
        lastAttempt = System.currentTimeMillis()
        return block()
    }

    private fun registerFailureWhenCircuitWasOpen() {
//        failedCount++
//        if (failedCount == failedThreshold) {
//            status = CircuitBreakerStatus.OPEN
//            println("Circuit open at: ${getSeconds()} --> $status -> $failedCount thread: ${Thread.currentThread().name}")
//        }
        val previousStats = stats.get()
        val newFailedCount = previousStats.failedCount + 1
        val newStats = previousStats.copy(
            failedCount = newFailedCount,
            status = if(newFailedCount == failedThreshold) CircuitBreakerStatus.OPEN else previousStats.status
        )
        while (true) {
            if (stats.compareAndSet(previousStats, newStats)) {
                println("Circuit open at: ${getSeconds()} --> ${newStats.status} -> ${newStats.failedCount} thread: ${Thread.currentThread().name}")
                return
            }
        }
    }

    private fun shouldCloseAfterPartialClose() = stats.get().successCountAfterPartialFailure == successThreshold

    private fun shouldOpenAfterPartialClose() = stats.get().failureCountAfterPartialFailure == failedThreshold

    private fun registerPartialFailure() {
//        failureCountAfterPartialFailure++

        val previousStats = stats.get()
        val newStats = previousStats.copy(
            failureCountAfterPartialFailure = previousStats.failureCountAfterPartialFailure + 1
        )
        while (true) {
            if (stats.compareAndSet(previousStats, newStats)) {
                return
            }
        }
    }

    private fun registerPartialSuccess() {
//        status = CircuitBreakerStatus.PARTIAL_CLOSE
//        successCountAfterPartialFailure++
//        println("Circuit PARTIAL_CLOSE at: ${getSeconds()} --> $status thread: ${Thread.currentThread().name}")

        val previousStats = stats.get()
        val newStats = previousStats.copy(
            status = CircuitBreakerStatus.PARTIAL_CLOSE,
            successCountAfterPartialFailure = previousStats.successCountAfterPartialFailure + 1
        )
        while (true) {
            if (stats.compareAndSet(previousStats, newStats)) {
                println("Circuit PARTIAL_CLOSE at: ${getSeconds()} --> ${newStats.status} thread: ${Thread.currentThread().name}")
                return
            }
        }
    }

    private fun openCircuit() {
//        status = CircuitBreakerStatus.OPEN
//        resetCounters()
//        println("Circuit OPEN at: ${getSeconds()} --> $status thread: ${Thread.currentThread().name}")
        val previousStats = stats.get()
        val newStats = previousStats.copy(
            status = CircuitBreakerStatus.OPEN,
            failedCount = 0,
            successCountAfterPartialFailure = 0,
            failureCountAfterPartialFailure = 0
        )
        while (true) {
            if (stats.compareAndSet(previousStats, newStats)) {
                println("Circuit OPEN at: ${getSeconds()} --> ${newStats.status} thread: ${Thread.currentThread().name}")
                return
            }
        }
    }

    private fun closeCircuit() {
//        status = CircuitBreakerStatus.CLOSE
//        println("Circuit CLOSE at: ${getSeconds()} --> $status thread: ${Thread.currentThread().name}")
//        resetCounters()

        val previousStats = stats.get()
        val newStats = previousStats.copy(
            status = CircuitBreakerStatus.CLOSE,
            failedCount = 0,
            successCountAfterPartialFailure = 0,
            failureCountAfterPartialFailure = 0
        )
        while (true) {
            if (stats.compareAndSet(previousStats, newStats)) {
                println("Circuit CLOSE at: ${getSeconds()} --> ${newStats.status} thread: ${Thread.currentThread().name}")
                return
            }
        }
    }
}

class CircuitBreakerResponse(
    val responseStatus: CircuitBreakerResponseStatus,
    val response: Any? = null,
    val ex: Exception? = null
) {
    companion object {
        fun success(response: Any): CircuitBreakerResponse {
            return CircuitBreakerResponse(CircuitBreakerResponseStatus.SUCCESS, response)
        }

        fun failure(ex: Exception): CircuitBreakerResponse {
            return CircuitBreakerResponse(CircuitBreakerResponseStatus.FAILURE, ex = ex)
        }

        fun defaultResponse(): CircuitBreakerResponse {
            return CircuitBreakerResponse(CircuitBreakerResponseStatus.DEFAULT_RESPONSE)
        }
    }
}

enum class CircuitBreakerResponseStatus {
    SUCCESS,
    FAILURE,
    DEFAULT_RESPONSE
}

enum class CircuitBreakerStatus {
    OPEN,
    CLOSE,
    PARTIAL_CLOSE
}

data class CircuitBreakerStats(
    val failedCount: Int = 0,
    val successCountAfterPartialFailure: Int = 0,
    val failureCountAfterPartialFailure: Int = 0,
    val status: CircuitBreakerStatus
)

fun main() {
    val circuitBreaker = CircuitBreaker(shouldRetryAfter = Duration.ofSeconds(3))

    val t1 = thread(start = false, name = "t1") {
        (0..10).forEach {
            Thread.sleep(2000)
            circuitBreaker.call { "do something" }
        }
    }

    val t2 = thread(start = false, name = "t2") {
        (0..10).forEach {
            Thread.sleep(2000)
            circuitBreaker.call { "do something" }
        }
    }

    val t3 = thread(start = false, name = "t3") {
        (0..5).forEach {
            Thread.sleep(1000)
            circuitBreaker.call { throw RuntimeException("something went wrong") }
        }
    }

    t1.start()
    t2.start()
    t3.start()

    t1.join()
    t2.join()
    t3.join()
}

fun getSeconds() = System.currentTimeMillis() / 1000

/*
OUTPUT ANALYSIS
The above code prints following output
notice that it's printing success when circuit state is OPEN, which should not be the case

t2 -> call 0 success at: 1625221544976 circuit status: CLOSE
t1 -> call 0 success at: 1625221544976 circuit status: CLOSE
t2 -> call 1 success at: 1625221545980 circuit status: CLOSE
t1 -> call 1 success at: 1625221545980 circuit status: CLOSE
t2 -> call 2 success at: 1625221546980 circuit status: OPEN
t1 -> call 2 success at: 1625221546980 circuit status: OPEN
t2 -> call 4 success at: 1625221548983 circuit status: OPEN
t1 -> call 5 success at: 1625221549983 circuit status: PARTIAL_CLOSE
t2 -> call 5 success at: 1625221549983 circuit status: PARTIAL_CLOSE

with lock it prints foll0wing output
Circuit open at: 1625225784 --> OPEN -> 3 thread: t3
Circuit PARTIAL_CLOSE at: 1625225786 --> PARTIAL_CLOSE thread: t1
Circuit PARTIAL_CLOSE at: 1625225786 --> PARTIAL_CLOSE thread: t2
Circuit PARTIAL_CLOSE at: 1625225788 --> PARTIAL_CLOSE thread: t1
Circuit CLOSE at: 1625225788 --> CLOSE thread: t1
Circuit PARTIAL_CLOSE at: 1625225788 --> PARTIAL_CLOSE thread: t2
Circuit PARTIAL_CLOSE at: 1625225790 --> PARTIAL_CLOSE thread: t1
Circuit PARTIAL_CLOSE at: 1625225790 --> PARTIAL_CLOSE thread: t2
Circuit CLOSE at: 1625225790 --> CLOSE thread: t2
 */