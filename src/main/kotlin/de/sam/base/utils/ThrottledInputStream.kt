package de.sam.base.utils

import com.google.common.util.concurrent.RateLimiter
import java.io.InputStream

class ThrottledInputStream(
    var inputStream: InputStream,
    var bytesPerSecond: Double
) : InputStream() {
    private var rateLimiter = RateLimiter.create(bytesPerSecond)

    override fun read(): Int {
        rateLimiter.acquire()
        return inputStream.read()
    }

    override fun read(b: ByteArray): Int {
        val read = inputStream.read(b)
        rateLimiter.acquire(read)
        return read
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = inputStream.read(b, off, len)
        rateLimiter.acquire(read)
        return read
    }

}