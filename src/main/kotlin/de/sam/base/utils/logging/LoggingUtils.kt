package de.sam.base.utils.logging

import org.tinylog.kotlin.Logger

inline fun <R> logTimeSpent(message: String, block: () -> R): R {
    val start = System.nanoTime()
    val answer = block()
    val time = (System.nanoTime() - start) / 1000000.0
    if (time > 1000) {
        Logger.warn(message + " took " + time + "ms")
    } else {
        Logger.debug(message + " took " + time + "ms")
    }
    return answer
}