package de.sam.base.requirements

import de.sam.base.database.ShareDAO
import de.sam.base.database.SharesTable
import de.sam.base.database.toDTO
import de.sam.base.services.FileService
import de.sam.base.utils.FileCache
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.fileDTOFromId
import de.sam.base.utils.logging.logTimeSpent
import de.sam.base.utils.share
import de.sam.base.utils.string.isUUID
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.HttpStatus
import io.javalin.http.pathParamAsClass
import io.javalin.security.RouteRole
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.util.*


enum class Requirement(var errorMessage: String, var httpStatus: HttpStatus) : RouteRole, KoinComponent {
    IS_LOGGED_IN("You need to be logged in to access this resource.", HttpStatus.FORBIDDEN) {
        override fun isMet(ctx: Context): Boolean {
            return ctx.currentUserDTO != null
        }
    },
    HAS_ACCESS_TO_FILE("This file does not exist or it has been deleted.", HttpStatus.NOT_FOUND) {

        override fun isMet(ctx: Context): Boolean {
            val fileCache by inject<FileCache>()

            if (!IS_LOGGED_IN.isMet(ctx)) {
                this.errorMessage = IS_LOGGED_IN.errorMessage
                return false
            }

            if (ctx.currentUserDTO!!.rootFolderId == null) {
                return false
            }

            val fileId = try {
                ctx.pathParamAsClass<UUID>("fileId").getOrDefault(ctx.currentUserDTO!!.rootFolderId!!)
            } catch (e: IllegalArgumentException) {
                ctx.currentUserDTO!!.rootFolderId!!
            }

            if (fileCache.containsKey(fileId) && System.currentTimeMillis() < fileCache[fileId]!!.first + 1000 * 10) {
                val fileCacheEntry = fileCache[fileId]!!
                ctx.fileDTOFromId = fileCacheEntry.second
            } else {
                if (fileCache.containsKey(fileId)) {
                    fileCache.remove(fileId)
                }

                transaction {
                    logTimeSpent("Getting file by id") {
                        val fileService: FileService by inject()
                        val fileDTO = fileService.getFileById(fileId)

                        if (fileDTO != null) {
                            Logger.trace("Setting fileDTO and DAO to request attribute")
                            ctx.fileDTOFromId = fileDTO
                            fileCache[fileId] = Pair(System.currentTimeMillis(), fileDTO)
                        }
                    }
                }
            }

            if (ctx.fileDTOFromId == null) {
                return false
            }

            if (ctx.fileDTOFromId != null && !ctx.fileDTOFromId!!.isOwnedByUserId(ctx.currentUserDTO?.id)) {
                Logger.error("File not found: access manager")
                return false
            }

            return true
        }
    },
    HAS_ACCESS_TO_SHARE("This share does not exist or it has been deleted.", HttpStatus.NOT_FOUND) {
        override fun isMet(ctx: Context): Boolean {
            val shareId = ctx.pathParamAsClass<String>("shareId").get()

            return transaction {
                logTimeSpent("Getting share by id") {
                    val shareDAO = if (shareId.isUUID) {
                        ShareDAO.find { SharesTable.id eq UUID.fromString(shareId) }
                            .limit(1)
                            .firstOrNull()
                    } else {
                        ShareDAO.find { SharesTable.vanityName eq shareId }
                            .limit(1)
                            .firstOrNull()
                    }

                    if (shareDAO == null) {
                        Logger.info("Share not found: access manager (actually not found)")
                        return@transaction false
                    }

                    if (ctx.method() == HandlerType.DELETE && ctx.currentUserDTO?.id != shareDAO.user.id.value) {
                        Logger.info("Share not found: access manager (user not owner)")
                        return@transaction false
                    }

                    val shareDTO = shareDAO.toDTO()
                    Logger.trace("Setting shareDTO and DAO to request attribute")
                    ctx.share = shareDAO to shareDTO

                    return@transaction true
                }
            }
        }
    },
    IS_IN_SETUP_STAGE("This page can not be found", HttpStatus.NOT_FOUND) {
        override fun isMet(ctx: Context): Boolean {
            return true
        }
    };

    open fun isMet(ctx: Context): Boolean {
        return false
    }
}