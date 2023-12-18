package de.sam.base.utils

import com.neutrine.krate.rateLimiter
import com.neutrine.krate.storage.memory.memoryStateStorageWithEviction
import org.koin.core.component.KoinComponent
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.hours

class RateLimiter : KoinComponent {
    val authentication = rateLimiter(maxRate = 5) {
        maxRateTimeUnit = ChronoUnit.HOURS
        stateStorage = memoryStateStorageWithEviction {
            ttlAfterLastAccess = 2.hours
        }
    }
    val share = rateLimiter(maxRate = 15) {
        maxRateTimeUnit = ChronoUnit.MINUTES
        stateStorage = memoryStateStorageWithEviction {
            ttlAfterLastAccess = 2.hours
        }
    }
}