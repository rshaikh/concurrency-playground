package com.starter

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")
}

var counter = 0
val mutex = Mutex()
fun main() = runBlocking {
//    val newSingleThreadContext = newSingleThreadContext("Counter-Context")
    withContext(Dispatchers.Default) {
        massiveRun {
            mutex.withLock { counter++ }

        }
    }
    println("Counter = $counter")
}
