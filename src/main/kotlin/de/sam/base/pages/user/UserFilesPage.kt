package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.database.FileDTO
import de.sam.base.services.FileService
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.file.sorting.FileSortingDirection
import de.sam.base.utils.fileDTOFromId
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.logging.logTimeSpent
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.transactions.transaction
import org.tinylog.kotlin.Logger

class UserFilesPage(private val fileService: FileService) : Page(
    name = "My Files",
    templateName = "user/files.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }

    lateinit var parent: FileDTO

    var fileDTOs = listOf<FileDTO>()

    var breadcrumbs = arrayListOf<FileDTO>()

    var sortByName: String = FileSortingDirection.sortDirections.first().prettyName
    var sortBy: String = FileSortingDirection.sortDirections.first().name

//    lateinit var rootFile: FileDTO

    override fun before() {
        breadcrumbs.clear()
    }

    override fun get() {
        val sortingDirection =
            FileSortingDirection.sortDirections.first {
                it.name == (ctx.queryParam("sort") ?: "name")
            }

        parent = ctx.fileDTOFromId!!
        
        logTimeSpent("the breadcrumb traversal") {
            breadcrumbs = fileService.getFileBreadcrumb(parent.id)
            title = breadcrumbs.last().name + " - My Files"
        }
        logTimeSpent("getting the files list") {
            transaction {
                if (parent.isFolder!!) {
                    if (!ctx.isLoggedIn) {
                        Logger.debug("File not found: user not logged in due to parent = null and folder requiring a user")
                        throw NotFoundResponse("File not found")
                    }
                    logTimeSpent("getting the file list") {
                        fileDTOs = fileService.getFolderContentForUser(ctx.currentUserDTO!!.id, parent.id)
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

//        if (ctx.preferencesString!!.split(",").contains("show-usage-quota")) {
//            rootFile = fileService.getRootFileForUser(ctx.currentUserDTO!!)!!
//        }

        if (ctx.queryParam("table") != null) {
            renderTemplate = false
            ctx.render(
                "components/files/fileListComp.kte",
                mapOf(
                    "fileDTOs" to fileDTOs,
                    "sortBy" to sortBy,
                    "sortByName" to sortByName,
                    "ctx" to ctx,
                    "parent" to parent
                )
            )
            return
        }
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