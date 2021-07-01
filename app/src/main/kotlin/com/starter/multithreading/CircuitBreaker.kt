package com.starter.multithreading

class CircuitBreaker(
    val failedThreshold: Int = 3, var failedCount: Int = 0,
    var status: CircuitBreakerStatus = CircuitBreakerStatus.CLOSE,
    var shouldRetry: Boolean = false,
    var successCountAfterPartialFailure: Int = 0,
) {

    fun call(block: () -> Any): CircuitBreakerResponse {
        if (status == CircuitBreakerStatus.OPEN && !shouldRetry) {
            return CircuitBreakerResponse.defaultResponse()
        }

        if (status == CircuitBreakerStatus.OPEN && shouldRetry) {
            return try {
                val response = block()
                status = CircuitBreakerStatus.PARTIAL_CLOSE
                successCountAfterPartialFailure++
                CircuitBreakerResponse.success(response)
            } catch (ex: Exception) {
                failedCount++
                CircuitBreakerResponse.failure(ex)
            }
        }

        if (status == CircuitBreakerStatus.PARTIAL_CLOSE) {
            return try {
                val response = block()
                successCountAfterPartialFailure++
                if(successCountAfterPartialFailure == 3) {
                    status = CircuitBreakerStatus.CLOSE
                }
                CircuitBreakerResponse.success(response)
            } catch (ex: Exception) {
                failedCount++
                CircuitBreakerResponse.failure(ex)
            }
        }

        return try {
            val response = block()
            CircuitBreakerResponse.success(response)
        } catch (ex: Exception) {
            failedCount++
            status = if (failedCount == failedThreshold) CircuitBreakerStatus.OPEN else CircuitBreakerStatus.CLOSE
            CircuitBreakerResponse.failure(ex)
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