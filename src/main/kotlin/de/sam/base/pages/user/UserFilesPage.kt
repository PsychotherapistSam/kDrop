package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.database.*
import de.sam.base.utils.currentUser
import io.javalin.http.Context
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.logTimeSpent
import org.jetbrains.exposed.sql.transactions.transaction
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

    //   val files = arrayListOf<KFile>()

    // val filesIndex = arrayListOf<KFile>()


    init {
        /* fun addFile(file: File) {
             filesIndex.add(file.toKFile())
             if (file.isDirectory) {
                 file.listFiles().forEach {
                     addFile(it)
                 }
             }
         }

         addFile(File("./"))*/


    }

    var parent: FileDAO? = null

    var files = listOf<File>()
    var breadcrumbs = arrayListOf<File>()

    override fun handle(ctx: Context) {
        breadcrumbs.clear()

        pageDiff = measureNanoTime {
            val parentFileId =
                if (ctx.pathParamMap().containsKey("fileId")) UUID.fromString(ctx.pathParam("fileId")) else null

            transaction {
                val user = UserDAO.findById(ctx.currentUser!!.id)
                if (user != null) {
                    logTimeSpent("finding the parent") {
                        parent = if (parentFileId != null) FileDAO.findById(parentFileId) else null
                    }
                    logTimeSpent("the breadcrumb traversal") {
                        // recursive list parents for breadcrumb
                        var breadcrumb = parent
                        while (breadcrumb != null) {
                            breadcrumbs.add(breadcrumb.toFile())
                            breadcrumb = breadcrumb.parent
                        }

                        // reverse list because the traversal is backwards
                        breadcrumbs.reverse()
                    }

                    logTimeSpent("getting the file list") {
                        files = FileDAO
                            .find { FilesTable.owner eq user.id and FilesTable.parent.eq(parent?.id) }
                            .map { it.toFile() }

                    }
                }
            }

            //TODO: add option of only refreshing the table (this would also fix the bug I had beforehand witwh the context menu not working when I only replace the table and not everything else)
            /*    files.clear()
                if (ctx.pathParamMap().containsKey("fileId")) {
                    val fileName = ctx.pathParam("fileId")
                    files.addAll(filesIndex.filter { it.parent == fileName })
                } else {
                    files.addAll(filesIndex.filter { it.parent == "." })
                }*/
        }
        super.handle(ctx)
    }
}

/*
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
*/