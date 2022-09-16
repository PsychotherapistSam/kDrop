package de.sam.base.utils.string

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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

fun String.urlEncode(): String? {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
}

fun String.urlDecode(): String? {
    return URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
}
