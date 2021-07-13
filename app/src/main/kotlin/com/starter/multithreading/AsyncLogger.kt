package com.starter.multithreading

import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class AsyncLogger(private val appender: Appender) {
    private val q = ConcurrentLinkedQueue<String>()
    init {
        appender.startReadingFrom(q)
    }
    fun info(message: String) {
        q.add(message)
    }
}

interface Appender {
    fun append(message: String)
    fun startReadingFrom(q: Queue<String>)
}

class FileAppender(private val fileName: String): Appender {
    private val file = File(fileName)
    override fun append(message: String) {
        file.appendText("logged at: ${System.currentTimeMillis()} $message\n")
    }

    override fun startReadingFrom(q: Queue<String>) {
        thread(start = true, isDaemon = true, name = "Consumer 1") {
            while (true) {
                q.poll()?.let { append("logged by: ${Thread.currentThread().name} $it") }
            }
        }
        thread(start = true, isDaemon = true, name = "Consumer 2") {
            while (true) {
                q.poll()?.let { append("logged by: ${Thread.currentThread().name} $it") }
            }
        }
        thread(start = true, isDaemon = true, name = "Consumer 3") {
            while (true) {
                q.poll()?.let { append("logged by: ${Thread.currentThread().name} $it") }
            }
        }
    }
}
fun main() {
    val logger = AsyncLogger(FileAppender("async-logger.log"))

    val threads = (0..100).map {
        thread(name = "T-$it", start = true) {
            logger.info("from: ${Thread.currentThread().name} event occurred at: ${System.currentTimeMillis()}")
        }
    }
    threads.forEach {
        it.join()
    }

    Thread.sleep(5000)
}