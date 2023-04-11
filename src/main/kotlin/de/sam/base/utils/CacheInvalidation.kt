package de.sam.base.utils

import java.util.*

class CacheInvalidation {
    companion object {
        val userTokens: MutableMap<UUID, Long> = mutableMapOf()
    }
}