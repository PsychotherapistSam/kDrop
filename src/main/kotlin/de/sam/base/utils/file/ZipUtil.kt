package de.sam.base.utils.file

import java.io.File
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun zipFiles(files: List<Pair<File, String>>, target: File): Boolean {
    if (!target.exists()) {
        if (!target.parentFile.exists()) {
            target.parentFile.mkdirs()
        }
        target.createNewFile()
    }

    val zipOutputStream = ZipOutputStream(target.outputStream())

    // by default, we do not want a compression level
    zipOutputStream.setLevel(Deflater.NO_COMPRESSION)
    try {
        zipOutputStream.use {
            files.forEach {
                zipOutputStream.putNextEntry(ZipEntry(it.second))

                // uses less memory than zipOutputStream.write(it.first.readBytes())
                it.first.inputStream().use { inputStream ->
                    inputStream.copyTo(zipOutputStream)
                }
                zipOutputStream.closeEntry()
            }
        }
    } catch (e: Exception) {
        target.delete()
        e.printStackTrace()
        return false
    } finally {
        zipOutputStream.close()
    }
    return true
}