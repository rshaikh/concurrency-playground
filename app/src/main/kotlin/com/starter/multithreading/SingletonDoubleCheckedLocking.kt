package com.starter.multithreading

class SingletonDoubleCheckedLocking {

    // in kotlin object is a language level construct, it will be called only once
    // this implementation is for demonstration purpose only
    companion object {
        @Volatile
        var sInstance: SingletonDoubleCheckedLocking? = null

        fun getInstance(): SingletonDoubleCheckedLocking {
            // assignment - to trigger latest read
            var inst = sInstance

            if(inst == null) {
                synchronized(SingletonDoubleCheckedLocking::class.java) {
                    // assignment - to trigger latest read
                    inst = sInstance
                    if(inst == null) {
                        sInstance = SingletonDoubleCheckedLocking()
                        inst = sInstance
                    }
                }
            }
            return inst!!
        }
    }
}