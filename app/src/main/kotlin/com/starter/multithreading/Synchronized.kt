package com.starter.multithreading

internal object Demonstration {
    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        IncorrectSynchronization.runExample()
    }
}
/*
A classic newbie mistake is to synchronize on an object and then somewhere in the code reassign the object.
As an example, look at the code below. We synchronize on a Boolean object in the first thread but sleep before
we call wait() on the object. While the first thread is asleep, the second thread goes on to change the flag's value.
When the first thread wakes up and attempts to invoke wait(), it is met with a IllegalMonitorState exception!
The object the first thread synchronized on before going to sleep has been changed, and now it is attempting to
call wait() on an entirely different object without having synchronized on it.
 */
internal class IncorrectSynchronization {
    var flag: Boolean = true
    @Throws(InterruptedException::class)
    fun example() {
        val t1 = Thread {
            synchronized(flag) {
                try {
                    while (flag) {
                        println("First thread about to sleep")
                        Thread.sleep(5000)
                        println("Woke up and about to invoke wait()")
                        flag.wait()
                    }
                } catch (ie: InterruptedException) {
                }
            }
        }
        val t2 = Thread {
            synchronized(flag) {
                flag = false
                println("Boolean assignment done., notifying now")
                flag.notify()
            }
//            flag = false
//            println("Boolean assignment done.")
        }
        t1.start()
        Thread.sleep(1000)
        t2.start()
        t1.join()
        t2.join()
    }

    companion object {
        @Throws(InterruptedException::class)
        fun runExample() {
            val incorrectSynchronization = IncorrectSynchronization()
            incorrectSynchronization.example()
        }
    }
}
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
public fun Any.wait() = (this as java.lang.Object).wait()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
public fun Any.notify() = (this as java.lang.Object).notify()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
public fun Any.notifyAll() = (this as java.lang.Object).notifyAll()