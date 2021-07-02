package com.starter.multithreading

class CircuitBreaker(
    val failedThreshold: Int = 3,
    var status: CircuitBreakerStatus = CircuitBreakerStatus.CLOSE,
    var shouldRetry: Boolean = false
) {
    private var successCountAfterPartialFailure = 0
    private var failureCountAfterPartialFailure = 0
    private var failedCount: Int = 0

    fun call(block: () -> Any): CircuitBreakerResponse {
        when (status) {
            CircuitBreakerStatus.OPEN -> {
                return if (shouldRetry) {
                    try {
                        val response = block()
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

    private fun registerFailureWhenCircuitWasOpen() {
        failedCount++
        status = if (failedCount == failedThreshold) CircuitBreakerStatus.OPEN else CircuitBreakerStatus.CLOSE
    }

    private fun shouldCloseAfterPartialClose() = successCountAfterPartialFailure == 3

    private fun shouldOpenAfterPartialClose() = failureCountAfterPartialFailure == 3

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