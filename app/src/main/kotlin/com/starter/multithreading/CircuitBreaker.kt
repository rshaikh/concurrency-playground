package com.starter.multithreading

import java.time.Duration

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

    fun call(block: () -> Any): CircuitBreakerResponse {
        when (status) {
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
        failedCount++
        status = if (failedCount == failedThreshold) CircuitBreakerStatus.OPEN else CircuitBreakerStatus.CLOSE
    }

    private fun shouldCloseAfterPartialClose() = successCountAfterPartialFailure == successThreshold

    private fun shouldOpenAfterPartialClose() = failureCountAfterPartialFailure == failedThreshold

    private fun registerPartialFailure() {
        failureCountAfterPartialFailure++
    }

    private fun registerPartialSuccess() {
        status = CircuitBreakerStatus.PARTIAL_CLOSE
        successCountAfterPartialFailure++
    }

    private fun openCircuit() {
        status = CircuitBreakerStatus.OPEN
        resetCounters()
    }

    private fun closeCircuit() {
        status = CircuitBreakerStatus.CLOSE
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