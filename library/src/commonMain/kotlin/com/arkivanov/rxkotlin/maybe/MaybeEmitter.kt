package com.arkivanov.rxkotlin.maybe

import com.badoo.reaktive.completable.CompletableEmitter

interface MaybeEmitter<in T> : CompletableEmitter {

    fun onSuccess(value: T)
}