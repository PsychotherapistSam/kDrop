package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.database.FileDAO
import de.sam.base.database.FileDTO
import de.sam.base.database.FilesTable
import de.sam.base.database.toFileDTO
import de.sam.base.utils.file.sorting.FileSortingDirection
import de.sam.base.utils.isLoggedIn
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse
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

    var sortByName: String = FileSortingDirection.sortDirections.first().prettyName
    var sortBy: String = FileSortingDirection.sortDirections.first().name

    override fun handle(ctx: Context) {
        breadcrumbs.clear()

        val sortingDirection = FileSortingDirection.sortDirections.first {
            it.name == (ctx.queryParam("sort") ?: "name")
        }

        pageDiff = measureNanoTime {
            val parentFileId =
                if (ctx.pathParamMap().containsKey("fileId")) UUID.fromString(ctx.pathParam("fileId")) else null

            transaction {
                logTimeSpent("finding the parent") {
                    parent = if (parentFileId != null) FileDAO.findById(parentFileId) else null
                }
                /*val user = logTimeSpent("finding the current user by id in the database") {
                    return@logTimeSpent UserDAO.findById(currentUserDTO!!.id)
                }*/

                // if the parentFileId is null, we are in the root directory so we do not return a 404
                if (parentFileId != null) {
                    // check if either the file does not exist or the user isn't the owner of the file and the file is not public
                    if (parent != null && parent!!.private && (currentUserDTO == null || !parent!!.toFileDTO()
                            .isOwnedByUserId(currentUserDTO!!.id))
                    ) {
                        throw NotFoundResponse("File not found")
                        //   if (parent == null || parent!!.toFileDTO().canBeViewedByUserId() && parent!!.private) {
                    }
                }

                // TODO: if !folder

                if (parent == null || parent!!.isFolder) {
                    if (!ctx.isLoggedIn) {
                        throw UnauthorizedResponse("You need to be logged in to access this resource.")
                    }

                    logTimeSpent("the breadcrumb traversal") {
                        // recursive list parents for breadcrumb
                        var breadcrumb = parent
                        while (breadcrumb != null) {
                            breadcrumbs.add(breadcrumb.toFileDTO())
                            breadcrumb = breadcrumb.parent
                        }

                        // reverse list because the traversal is backwards
                        breadcrumbs.reverse()
                    }


                    logTimeSpent("getting the file list") {
                        fileDTOs = FileDAO
                            .find { FilesTable.owner eq currentUserDTO!!.id and FilesTable.parent.eq(parent?.id) }
                            .map { it.toFileDTO() }
                            .sortedWith { a, b ->
                                sortingDirection.compare(a, b)
                                //    CASEINSENSITIVE_NUMERICAL_ORDER.compare(a.name, b.name)
                                // NameFileComparator uses this for comparison, as I don't have files I cannot use it.
                                //  IOCase.INSENSITIVE.checkCompareTo(a.name, b.name)
                            }
                    }
                }
            }
        }

        ctx.header("HX-Push", "./?sort=${sortingDirection.name}")

        if (ctx.queryParam("table") != null) {
            ctx.render(
                "components/files/fileListComp.kte",
                mapOf("fileDTOs" to fileDTOs, "sortBy" to sortBy, "sortByName" to sortByName)
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