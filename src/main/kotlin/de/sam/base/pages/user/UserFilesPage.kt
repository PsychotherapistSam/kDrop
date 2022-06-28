package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.database.*
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.file.FilenameComparator
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
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

    var parent: FileDAO? = null
    var fileDTOs = listOf<FileDTO>()
    var breadcrumbs = arrayListOf<FileDTO>()

    override fun handle(ctx: Context) {
        breadcrumbs.clear()

        pageDiff = measureNanoTime {
            val parentFileId =
                if (ctx.pathParamMap().containsKey("fileId")) UUID.fromString(ctx.pathParam("fileId")) else null

            transaction {
                val user = logTimeSpent("finding the current user by id in the database") {
                    return@logTimeSpent UserDAO.findById(ctx.currentUserDTO!!.id)
                }
                if (user != null) {
                    logTimeSpent("finding the parent") {
                        parent = if (parentFileId != null) FileDAO.findById(parentFileId) else null
                    }

                    // if the parentFileId is null, we are in the root directory so we do not return a 404
                    if (parentFileId != null) {
                        // check if either the file does not exist or the user isn't the owner of the file and the file is not public
                        if (parent == null || parent!!.owner != user && parent!!.private) {
                            throw NotFoundResponse("File not found")
                        }
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

                    val filenameComparator = FilenameComparator()

                    logTimeSpent("getting the file list") {
                        fileDTOs = FileDAO
                            .find { FilesTable.owner eq user.id and FilesTable.parent.eq(parent?.id) }
                            .map { it.toFile() }
                            //.sortedBy { it.name }
                            .sortedWith { a, b ->
                                filenameComparator.compare(a.name, b.name)
                            }
                    }
                }
            }
        }
        if (ctx.queryParam("table") != null) {
            ctx.render(
                "components/files/fileListComp.kte",
                mapOf("fileDTOs" to fileDTOs)
            )
            return
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