package de.sam.base.utils.string

import java.util.*

 val String?.isUUID: Boolean
    get() {
        return try {
            UUID.fromString(this)
            true
        } catch (ex: IllegalArgumentException) {
            false
        }
    }
