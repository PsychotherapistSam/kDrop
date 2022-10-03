package de.sam.base.utils

import io.javalin.validation.Validator

fun <T> Validator<T>.getFirstError(): Pair<Validator<T>, String?> {
    if (this.errors().isNotEmpty()) {
        val errors = this.errors()
        var errorMessage = errors.values.first()[0].message

        errorMessage = when (errorMessage) {
            "TYPE_CONVERSION_FAILED" -> "The value you entered is not valid."
            else -> errorMessage
        }

        return Pair(this, errorMessage)
    }
    return Pair(this, null)
}