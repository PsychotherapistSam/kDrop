package de.sam.base.utils

import com.google.common.util.concurrent.RateLimiter
import java.io.OutputStream

class ThrottledOutputStream(
    var outputStream: OutputStream,
    var bytesPerSecond: Double
) : OutputStream() {
    private var rateLimiter = RateLimiter.create(bytesPerSecond)

    override fun write(b: Int) {
        rateLimiter.acquire()
        outputStream.write(b)
    }

    override fun write(b: ByteArray) {
        rateLimiter.acquire(b.size)
        super.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        rateLimiter.acquire(len)
        super.write(b, off, len)
    }

}
