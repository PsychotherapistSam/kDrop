package de.sam.base.utils

import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.HttpStatus
import org.tinylog.kotlin.Logger
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

object CustomSeekableWriter {
    private const val chunkSize = 128000 * 32 * 4

    fun write(ctx: Context, inputStream: InputStream, contentType: String, totalBytes: Long) = ctx.async {
        val uncompressedStream = ctx.res().outputStream
        if (ctx.header(Header.RANGE) == null) {
            ctx.header(Header.CONTENT_TYPE, contentType)
            ctx.header(Header.CONTENT_LENGTH, totalBytes.toString())
            try {
                inputStream.transferTo(uncompressedStream)
            } catch (e: EOFException) {
                Logger.error { "EOFException while streaming file: " + e.message }
            } catch (e: IOException) {
                Logger.error { "IOException while streaming file: " + e.message }
            }
            inputStream.close()
            return@async
        }

        val requestedRange = ctx.header(Header.RANGE)!!.split("=")[1].split("-").filter { it.isNotEmpty() }
        val from = requestedRange[0].toLong()
        val to = when {
            from + chunkSize > totalBytes -> totalBytes - 1 // chunk bigger than file, write all
            requestedRange.size == 2 -> requestedRange[1].toLong() // chunk smaller than file, to/from specified
            else -> from + chunkSize -1 //totalBytes - 1 // chunk smaller than file, to/from not specified -> return full header (jdownloader2 compatability)
        }
        ctx.status(HttpStatus.PARTIAL_CONTENT)
        ctx.header(Header.CONTENT_TYPE, contentType)
        ctx.header(Header.ACCEPT_RANGES, "bytes")
        ctx.header(Header.CONTENT_RANGE, "bytes $from-$to/$totalBytes")
        ctx.header(Header.CONTENT_LENGTH, "${min(to - from + 1, totalBytes)}")
        uncompressedStream.write(inputStream, from, to)
    }

    private fun OutputStream.write(
        inputStream: InputStream,
        from: Long,
        to: Long,
        buffer: ByteArray = ByteArray(1024)
    ) = inputStream.use {
        var toSkip = from
        while (toSkip > 0) {
            val skipped = it.skip(toSkip)
            toSkip -= skipped
        }
        var bytesLeft = to - from + 1
        while (bytesLeft != 0L) {
            val read = it.read(buffer, 0, buffer.size.toLong().coerceAtMost(bytesLeft).toInt())
            try {
                this.write(buffer, 0, read)
                this.flush()
            } catch (_: EOFException) {
                break
            }
            bytesLeft -= read
        }
    }
}