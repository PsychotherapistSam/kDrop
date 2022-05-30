package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.files.KFile
import io.javalin.http.Context
import java.io.File
import java.util.*
import kotlin.system.measureNanoTime

class UserFilesPage : Page() {
    companion object {
        lateinit var ROUTE: String
    }

    override var name: String = "My Files"
    override var title: String = name
    override var pageDescription: String
        get() = name
        set(value) {}
    override var templateName: String = "user/files.kte"

    val files = arrayListOf<KFile>()

    val filesIndex = arrayListOf<KFile>()

    init {
        fun addFile(file: File) {
            filesIndex.add(file.toKFile())
            if (file.isDirectory) {
                file.listFiles().forEach {
                    addFile(it)
                }
            }
        }

        addFile(File("./"))
    }

    override fun handle(ctx: Context) {
        pageDiff = measureNanoTime {
            files.clear()
            if (ctx.pathParamMap().containsKey("fileId")) {
                val fileName = ctx.pathParam("fileId")
                files.addAll(filesIndex.filter { it.parent == fileName })
            } else {
                files.addAll(filesIndex.filter { it.parent == "." })
            }
        }
        super.handle(ctx)
    }
}

private fun File.toKFile(): KFile {
    return KFile(
        id = UUID.randomUUID(),
        name = this.name,
        parent = this.parentFile.let { it?.name },
        size = "ooga GB",
        lastModified = "now",
        isDirectory = this.isDirectory,
        children = this.listFiles().let { files -> files.orEmpty().map { b -> b.name } }
    )
}
