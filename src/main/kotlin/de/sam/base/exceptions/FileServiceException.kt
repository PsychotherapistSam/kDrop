package de.sam.base.exceptions

class FileServiceException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
