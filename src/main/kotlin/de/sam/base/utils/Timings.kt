package de.sam.base.utils

import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.system.measureTimeMillis


@OptIn(ExperimentalContracts::class)
inline fun prolongAtLeast(ms: Long, randomTime: Long = 200, block: () -> Unit): Unit? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var test: Unit? = null
    val executionTime = measureTimeMillis {
        test = block()
    }

    val waitTime = ms - (randomTime / 2) + Random().nextInt(200)

    val timeDiff: Long = waitTime - executionTime
    if (timeDiff > 0) Thread.sleep(timeDiff)
    return test
}
