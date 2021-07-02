package com.starter.multithreading

import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class CircuitBreaker(
    val failedThreshold: Int = 3,
    val successThreshold: Int = 3,
    var status: CircuitBreakerStatus = CircuitBreakerStatus.CLOSE,
    val shouldRetryAfter: Duration = Duration.ofSeconds(1)
) {
    private var successCountAfterPartialFailure = 0
    private var failureCountAfterPartialFailure = 0
    private var failedCount: Int = 0
    private var lastAttempt = System.currentTimeMillis()
    private val lock = ReentrantLock()
    fun call(block: () -> Any): CircuitBreakerResponse {
        when (status) {
            CircuitBreakerStatus.OPEN -> {
                return if (shouldRetry()) {
                    try {
                        val response = executeBlock(block)
                        lock.lock()
                        registerPartialSuccess()
                        lock.unlock()
                        CircuitBreakerResponse.success(response)
                    } catch (ex: Exception) {
                        lock.lock()
                        registerPartialFailure()
                        lock.unlock()
                        CircuitBreakerResponse.failure(ex)
                    }
                } else {
                    CircuitBreakerResponse.defaultResponse()
                }
            }
            CircuitBreakerStatus.PARTIAL_CLOSE -> return try {
                val response = block()
                lock.lock()
                registerPartialSuccess()
                if (shouldCloseAfterPartialClose()) {
                    closeCircuit()
                }
                lock.unlock()
                CircuitBreakerResponse.success(response)
            } catch (ex: Exception) {
                lock.lock()
                registerPartialFailure()
                if (shouldOpenAfterPartialClose()) {
                    openCircuit()
                }
                lock.unlock()
                CircuitBreakerResponse.failure(ex)
            }
            else -> return try {
                val response = block()
                CircuitBreakerResponse.success(response)
            } catch (ex: Exception) {
                lock.lock()
                registerFailureWhenCircuitWasOpen()
                lock.unlock()
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
        failedCount++
        if (failedCount == failedThreshold) {
            status = CircuitBreakerStatus.OPEN
            println("Circuit open at: ${getSeconds()} --> $status -> $failedCount thread: ${Thread.currentThread().name}")
        }
    }

    private fun shouldCloseAfterPartialClose() = successCountAfterPartialFailure == successThreshold

    private fun shouldOpenAfterPartialClose() = failureCountAfterPartialFailure == failedThreshold

    private fun registerPartialFailure() {
        failureCountAfterPartialFailure++
    }

    private fun registerPartialSuccess() {
        status = CircuitBreakerStatus.PARTIAL_CLOSE
        successCountAfterPartialFailure++
        println("Circuit PARTIAL_CLOSE at: ${getSeconds()} --> $status thread: ${Thread.currentThread().name}")
    }

    private fun openCircuit() {
        status = CircuitBreakerStatus.OPEN
        resetCounters()
        println("Circuit OPEN at: ${getSeconds()} --> $status thread: ${Thread.currentThread().name}")
    }

    private fun closeCircuit() {
        status = CircuitBreakerStatus.CLOSE
        println("Circuit CLOSE at: ${getSeconds()} --> $status thread: ${Thread.currentThread().name}")
        resetCounters()
    }

    private fun resetCounters() {
        failedCount = 0
        successCountAfterPartialFailure = 0
        failureCountAfterPartialFailure = 0
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
        (0..6).forEach {
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