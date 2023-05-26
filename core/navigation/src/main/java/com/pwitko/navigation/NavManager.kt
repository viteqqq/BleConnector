package com.pwitko.navigation

import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit

object NavManager {

    private val commandRelay = PublishRelay.create<NavCommand>()
    val commands: Observable<NavCommand> get() = commandRelay.delay(0, TimeUnit.SECONDS)

    fun navigate(directions: NavCommand) {
        commandRelay.accept(directions)
    }
}