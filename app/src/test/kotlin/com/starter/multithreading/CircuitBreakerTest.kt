package com.starter.multithreading

import org.amshove.kluent.`should equal`
import org.junit.jupiter.api.Test

class CircuitBreakerTest{
    private fun anOpenCircuitBreaker() =
        CircuitBreaker(failedThreshold = 3, status = CircuitBreakerStatus.OPEN)

    private fun anOpenCircuitBreakerReadyToRetry() =
        CircuitBreaker(failedThreshold = 3,
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
            throw exceptionFromBlock
        }

        circuitBreaker.call {
            throw exceptionFromBlock
        }

        circuitBreaker.call {
            throw exceptionFromBlock
        }

        circuitBreaker.status `should equal` CircuitBreakerStatus.OPEN
    }

    @Test
    fun `it should return a default response when circuit is open`() {
        val circuitBreaker = anOpenCircuitBreaker()
        val exceptionFromBlock = RuntimeException("something went wrong")

        val breakerResponse = circuitBreaker.call {
            throw exceptionFromBlock
        }

        breakerResponse.responseStatus `should equal` CircuitBreakerResponseStatus.DEFAULT_RESPONSE
    }

    @Test
    fun `it should mark circuit breaker as partially close on success response when current state is open and ready to retry`() {
        val circuitBreaker = anOpenCircuitBreakerReadyToRetry()

        circuitBreaker.call {
            "This is a successful call"
        }

        circuitBreaker.status `should equal` CircuitBreakerStatus.PARTIAL_CLOSE
    }

    @Test
    fun `it should keep circuit breaker as open on failure response when current state is open and ready to retry`() {
        val circuitBreaker = anOpenCircuitBreakerReadyToRetry()

        circuitBreaker.call {
            throw RuntimeException("something went wrong")
        }

        circuitBreaker.status `should equal` CircuitBreakerStatus.OPEN
    }

    @Test
    fun `it should mark circuit breaker as close on 3 success responses when current state partial close`() {
        val circuitBreaker = anPartialCloseCircuit()

        circuitBreaker.call {
            "successful response"
        }
        circuitBreaker.call {
            "successful response"
        }
        circuitBreaker.call {
            "successful response"
        }

        circuitBreaker.status `should equal` CircuitBreakerStatus.CLOSE
    }

    @Test
    fun `it should mark circuit breaker as open on 3 failed responses when current state partial close`() {
        val circuitBreaker = anPartialCloseCircuit()

        circuitBreaker.call {
            throw RuntimeException("something went wrong")
        }
        circuitBreaker.call {
            throw RuntimeException("something went wrong")
        }
        circuitBreaker.call {
            throw RuntimeException("something went wrong")
        }

        circuitBreaker.status `should equal` CircuitBreakerStatus.OPEN
    }
}