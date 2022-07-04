package de.sam.base.utils.file

import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun zipFiles(files: List<Pair<File, String>>, target: File): Boolean {
    if (!target.exists()) {
        if (!target.parentFile.exists()) {
            target.parentFile.mkdirs()
        }
        target.createNewFile()
    }
    val zipOutputStream = ZipOutputStream(BufferedOutputStream(target.outputStream()))
    zipOutputStream.use {
        files.forEach {
            zipOutputStream.putNextEntry(ZipEntry(it.second))
            zipOutputStream.write(it.first.readBytes())
            zipOutputStream.closeEntry()
        }
    }
    return true
}