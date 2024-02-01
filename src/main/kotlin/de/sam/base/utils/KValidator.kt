package de.sam.base.utils

class KValidator<T>(private var obj: T?) {
    private val errors = mutableListOf<String>()

    fun check(innerCheck: (T) -> Boolean, errorMsg: String): KValidator<T> {
        if (obj == null) {
            errors.add("Null value not permitted")
        } else if (!innerCheck(obj!!)) {
            errors.add(errorMsg)
        }
        return this
    }

    fun isValid(): Boolean {
        return errors.isEmpty()
    }

    fun errors(): List<String> {
        return errors
    }
}