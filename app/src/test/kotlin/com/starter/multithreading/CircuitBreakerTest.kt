package com.starter.multithreading

import org.amshove.kluent.`should equal`
import org.junit.jupiter.api.Test
import java.time.Duration

class CircuitBreakerTest {
    private fun anOpenCircuitBreaker() =
        CircuitBreaker(failedThreshold = 3, initialStatus = CircuitBreakerStatus.OPEN)

    private fun anOpenCircuitBreakerReadyToRetryAfterOneSecond() =
        CircuitBreaker(
            failedThreshold = 3,
            initialStatus = CircuitBreakerStatus.OPEN,
            shouldRetryAfter = Duration.ofSeconds(1)
        )

    private fun anPartialCloseCircuit() =
        CircuitBreaker(initialStatus = CircuitBreakerStatus.PARTIAL_CLOSE)

    @Test
    fun `it should return success response from upstream`() {
        val circuitBreaker = CircuitBreaker()
        val blockResponse = "This is a successful call"

        val breakerResponse = circuitBreaker.call {
            blockResponse
        }

        breakerResponse.responseStatus `should equal` CircuitBreakerResponseStatus.SUCCESS
        breakerResponse.response!! `should equal` blockResponse
    }

    @Test
    fun `it should return failed response from upstream`() {
        val circuitBreaker = CircuitBreaker()
        val exceptionFromBlock = RuntimeException("something went wrong")

        val breakerResponse = circuitBreaker.call {
            throw exceptionFromBlock
        }

        breakerResponse.responseStatus `should equal` CircuitBreakerResponseStatus.FAILURE
        breakerResponse.ex!! `should equal` exceptionFromBlock
    }

    @Test
    fun `it should mark circuit breaker status as open when 3 failed response from upstream`() {
        val circuitBreaker = CircuitBreaker()
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

        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.OPEN
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
        val circuitBreaker = anOpenCircuitBreakerReadyToRetryAfterOneSecond()
        Thread.sleep(1000)

        circuitBreaker.call {
            "This is a successful call"
        }

        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.PARTIAL_CLOSE
    }

    @Test
    fun `it should keep circuit breaker as open on failure response when current state is open and ready to retry`() {
        val circuitBreaker = anOpenCircuitBreakerReadyToRetryAfterOneSecond()
        Thread.sleep(1000)

        circuitBreaker.call {
            throw RuntimeException("something went wrong")
        }

        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.OPEN
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

        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.CLOSE
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

        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.OPEN
    }

    @Test
    fun `it should automatically retry connection after specified duration`() {
        val circuitBreaker = CircuitBreaker(
            failedThreshold = 3,
            initialStatus = CircuitBreakerStatus.OPEN,
            shouldRetryAfter = Duration.ofSeconds(2)
        )
        val success = "some success response"
        Thread.sleep(2010)
        val breakerResponse = circuitBreaker.call {
            success
        }
        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.PARTIAL_CLOSE
        breakerResponse.response!! `should equal` success
    }

    @Test
    fun `integration test - all scenarios in one place`() {
        val circuitBreaker =
            CircuitBreaker(failedThreshold = 3, successThreshold = 3, shouldRetryAfter = Duration.ofSeconds(2))
        val success = "some success response"
        val exception = java.lang.RuntimeException("something went wrong")

        //Four successful calls; should keep status as CLOSE
        circuitBreaker.call { success }
        circuitBreaker.call { success }
        circuitBreaker.call { success }
        circuitBreaker.call { success }
        circuitBreaker.call { success }
        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.CLOSE

        //Three failed calls should change the status to OPEN
        circuitBreaker.call { throw exception }
        circuitBreaker.call { throw exception }
        circuitBreaker.call { throw exception }
        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.OPEN

        //Any calls after circuit is OPEN should return DEFAULT_RESPONSE and status should stay as OPEN
        circuitBreaker.call { throw exception }.responseStatus `should equal` CircuitBreakerResponseStatus.DEFAULT_RESPONSE
        circuitBreaker.call { throw exception }.responseStatus `should equal` CircuitBreakerResponseStatus.DEFAULT_RESPONSE
        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.OPEN

        //Failed call after retry duration should go through and return the actual response from upstream with exception
        //keep status as OPEN
        Thread.sleep(2000)
        val call = circuitBreaker.call { throw exception }
        call.responseStatus `should equal` CircuitBreakerResponseStatus.FAILURE
        call.ex!! `should equal` exception
        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.OPEN

        //Next call should return default response
        circuitBreaker.call { throw exception }.responseStatus `should equal` CircuitBreakerResponseStatus.DEFAULT_RESPONSE
        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.OPEN

        // Wait for 2 seconds; it should retry and change status to PARTIAL_CLOSE
        Thread.sleep(2000)
        circuitBreaker.call { success }.response!! `should equal` success
        circuitBreaker.call { success }.response!! `should equal` success
        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.PARTIAL_CLOSE

        //Third success call should change the status to CLOSE and return response from upstream
        circuitBreaker.call { success }.response!! `should equal` success
        circuitBreaker.stats.get().status `should equal` CircuitBreakerStatus.CLOSE
    }
}