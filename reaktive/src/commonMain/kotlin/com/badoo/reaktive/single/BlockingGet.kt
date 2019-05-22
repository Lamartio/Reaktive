package com.badoo.reaktive.single

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.utils.Lock
import com.badoo.reaktive.utils.atomicreference.AtomicReference
import com.badoo.reaktive.utils.synchronized

fun <T> Single<T>.blockingGet(): T {
    val lock = Lock()
    val condition = lock.newCondition()
    val result = AtomicReference<BlockingGetResult<T>?>(null, true)
    val upstreamDisposable = AtomicReference<Disposable?>(null, true)

    subscribe(
        object : SingleObserver<T> {
            override fun onSubscribe(disposable: Disposable) {
                upstreamDisposable.value = disposable
            }

            override fun onSuccess(value: T) {
                lock.synchronized {
                    result.value = BlockingGetResult.Success(value)
                    condition.signal()
                }
            }

            override fun onError(error: Throwable) {
                lock.synchronized {
                    result.value = BlockingGetResult.Error(error)
                    condition.signal()
                }
            }
        }
    )

    lock.synchronized {
        while (result.value == null) {
            try {
                condition.await()
            } catch (e: Throwable) {
                upstreamDisposable.value?.dispose()
                throw e
            }
        }
    }

    result.value!!.also {
        when (it) {
            is BlockingGetResult.Success -> return it.value
            is BlockingGetResult.Error -> throw it.error
        }
    }
}

private sealed class BlockingGetResult<out T> {
    class Success<out T>(val value: T) : BlockingGetResult<T>()
    class Error(val error: Throwable) : BlockingGetResult<Nothing>()
}
