package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.database.FileDAO
import de.sam.base.database.FileDTO
import de.sam.base.database.FilesTable
import de.sam.base.database.toFileDTO
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.file.sorting.FileSortingDirection
import de.sam.base.utils.fileDAOFromId
import de.sam.base.utils.fileDTOFromId
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.logging.logTimeSpent
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.tinylog.kotlin.Logger
import java.util.*
import kotlin.system.measureNanoTime

class UserFilesPage : Page(
    name = "My Files",
    templateName = "user/files.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }

    var parent: FileDTO? = null
    var fileIsOwnedByCurrentUser = false
    var fileDTOs = listOf<FileDTO>()
    var breadcrumbs = arrayListOf<FileDTO>()

    var sortByName: String = FileSortingDirection.sortDirections.first().prettyName
    var sortBy: String = FileSortingDirection.sortDirections.first().name

    override fun handle(ctx: Context) {
        pageDiff = measureNanoTime {
            breadcrumbs.clear()

            val sortingDirection =
                FileSortingDirection.sortDirections.first {
                    it.name == (ctx.queryParam("sort") ?: "name")
                }

            parent = ctx.fileDTOFromId

            transaction {
                if (parent != null && parent!!.isOwnedByUserId(ctx.currentUserDTO?.id)) {
                    logTimeSpent("the breadcrumb traversal") {
                        // recursive list parents for breadcrumb
                        var breadcrumb = parent
                        while (breadcrumb != null) {
                            breadcrumbs.add(breadcrumb)
                            breadcrumb = breadcrumb.parent
                        }
                        // reverse list because the traversal is backwards
                        breadcrumbs.reverse()
                    }
                }

                // parent == null defualts to root folder
                if (parent == null || parent!!.isFolder) {
                    if (!ctx.isLoggedIn) {
                        Logger.debug("File not found: user not logged in due to parent = null and folder requiring a user")
                        throw NotFoundResponse("File not found")
                    }

                    logTimeSpent("getting the file list") {
                        fileDTOs = FileDAO
                            .find { FilesTable.owner eq ctx.currentUserDTO!!.id and FilesTable.parent.eq(ctx.fileDAOFromId?.id) }
//                            .find { FilesTable.owner eq ctx.currentUserDTO!!.id and FilesTable.parent.eq(ctx.fileDAOFromId) }
//                            .filter { it.parent?.id?.value == parent?.id }
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
            ctx.header("HX-Push", "./?sort=${sortingDirection.name}")
        }

        if (ctx.queryParam("table") != null) {
            ctx.render(
                "components/files/fileListComp.kte",
                mapOf(
                    "fileDTOs" to fileDTOs,
                    "sortBy" to sortBy,
                    "sortByName" to sortByName,
                    "ctx" to ctx
                )
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