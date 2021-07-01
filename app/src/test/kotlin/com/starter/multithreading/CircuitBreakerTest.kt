package com.starter.multithreading

import org.amshove.kluent.`should equal`
import org.junit.jupiter.api.Test

class CircuitBreakerTest{
    private fun anOpenCircuitBreaker() =
        CircuitBreaker(failedThreshold = 3, failedCount = 3, status = CircuitBreakerStatus.OPEN)

    private fun anOpenCircuitBreakerReadyToRetry() =
        CircuitBreaker(failedThreshold = 3,
            failedCount = 3,
            status = CircuitBreakerStatus.OPEN,
            shouldRetry = true)

    private fun aClosedCircuitBreaker() = CircuitBreaker()

    private fun anPartialCloseCircuit() =
        CircuitBreaker(status = CircuitBreakerStatus.PARTIAL_CLOSE)


    @Test
    fun `it should return success response from upstream`() {
        val circuitBreaker = aClosedCircuitBreaker()
        val blockResponse = "This is a successful call"

        val breakerResponse = circuitBreaker.call {
            println("block has been called, returning success")
            blockResponse
        }

        breakerResponse.responseStatus `should equal` CircuitBreakerResponseStatus.SUCCESS
        breakerResponse.response!! `should equal` blockResponse
    }

    @Test
    fun `it should return failed response from upstream`() {
        val circuitBreaker = aClosedCircuitBreaker()
        val exceptionFromBlock = RuntimeException("something went wrong")

        val breakerResponse = circuitBreaker.call {
            println("block has been called, returning success")
            throw exceptionFromBlock
        }

        breakerResponse.responseStatus `should equal` CircuitBreakerResponseStatus.FAILURE
        breakerResponse.ex!! `should equal` exceptionFromBlock
    }

    @Test
    fun `it should mark circuit breaker status as open when 3 failed response from upstream`() {
        val circuitBreaker = aClosedCircuitBreaker()
        val exceptionFromBlock = RuntimeException("something went wrong")

        circuitBreaker.call {
            println("block has been called, returning success")
            throw exceptionFromBlock
        }

        circuitBreaker.call {
            println("block has been called, returning success")
            throw exceptionFromBlock
        }

        circuitBreaker.call {
            println("block has been called, returning success")
            throw exceptionFromBlock
        }

        circuitBreaker.status `should equal` CircuitBreakerStatus.OPEN
    }

    @Test
    fun `it should return a default response when circuit is open`() {
        val circuitBreaker = anOpenCircuitBreaker()
        val exceptionFromBlock = RuntimeException("something went wrong")

        val breakerResponse = circuitBreaker.call {
            println("block has been called, returning success")
            throw exceptionFromBlock
        }

        breakerResponse.responseStatus `should equal` CircuitBreakerResponseStatus.DEFAULT_RESPONSE
    }

    @Test
    fun `it should mark circuit breaker as partially close on success response when current state is open and ready to retry`() {
        val circuitBreaker = anOpenCircuitBreakerReadyToRetry()

        circuitBreaker.call {
            println("block has been called, returning success")
            "This is a successful call"
        }

        circuitBreaker.status `should equal` CircuitBreakerStatus.PARTIAL_CLOSE
    }

    @Test
    fun `it should keep circuit breaker as open on failure response when current state is open and ready to retry`() {
        val circuitBreaker = anOpenCircuitBreakerReadyToRetry()

        circuitBreaker.call {
            println("block has been called, returning success")
            throw RuntimeException("something went wrong")
        }

        circuitBreaker.status `should equal` CircuitBreakerStatus.OPEN
    }

    @Test
    fun `it should mark circuit breaker as close on 3 success responses when current state partial open`() {
        val circuitBreaker = anPartialCloseCircuit()

        circuitBreaker.call {
            println("block has been called, returning success")
            "successful response"
        }
        circuitBreaker.call {
            println("block has been called, returning success")
            "successful response"
        }
        circuitBreaker.call {
            println("block has been called, returning success")
            "successful response"
        }

        circuitBreaker.status `should equal` CircuitBreakerStatus.CLOSE
    }
}