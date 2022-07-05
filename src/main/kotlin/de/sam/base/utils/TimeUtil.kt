package de.sam.base.utils

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.ocpsoft.prettytime.PrettyTime
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


var formatter: PeriodFormatter = PeriodFormatterBuilder()
    .appendSeconds().appendSuffix(" seconds ago\n")
    .appendMinutes().appendSuffix(" minutes ago\n")
    .appendHours().appendSuffix(" hours ago\n")
    .appendDays().appendSuffix(" days ago\n")
    .appendWeeks().appendSuffix(" weeks ago\n")
    .appendMonths().appendSuffix(" months ago\n")
    .appendYears().appendSuffix(" years ago\n")
    .printZeroNever()
    .toFormatter()


val prettyTime = PrettyTime().apply {
    locale = Locale.ENGLISH
}

val patternFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-M-d H:m:s (z)")

fun DateTime.toRecentTimeString(): String? {
    return prettyTime.format(this.toDate())
}

fun DateTime.toReadableTimeString(): String? {
    return this.toString(patternFormatter)
}