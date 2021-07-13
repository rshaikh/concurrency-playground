package com.starter.multithreading

import java.util.concurrent.atomic.AtomicReference

class SingletonAtomicReference {

    // in kotlin object is a language level construct, it will be called only once
    // this implementation is for demonstration purpose only
    companion object {
        var sInstance: AtomicReference<SingletonAtomicReference> = AtomicReference(null)

        fun getInstance(): SingletonAtomicReference {
            var singleton = sInstance.get()

            if(singleton == null) {
                singleton = SingletonAtomicReference()

                if(!sInstance.compareAndSet(null, singleton)) {
                    singleton = sInstance.get()
                }
            }
            return singleton
        }
    }
}