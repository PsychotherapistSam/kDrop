package de.sam.base.requirements

import de.sam.base.authentication.apikey.ApiKeyRepository
import de.sam.base.file.repository.FileRepository
import de.sam.base.file.share.ShareRepository
import de.sam.base.utils.*
import de.sam.base.utils.logging.logTimeSpent
import de.sam.base.utils.string.isUUID
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.HttpStatus
import io.javalin.http.pathParamAsClass
import io.javalin.security.RouteRole
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.util.*


enum class Requirement(var errorMessage: String, var httpStatus: HttpStatus) : RouteRole, KoinComponent {
    IS_LOGGED_IN("You need to be logged in to access this resource.", HttpStatus.FORBIDDEN) {
        override fun isMet(ctx: Context): Boolean {
            // not checking for TOTP here, because its used during TOTP validation
            return ctx.currentUserDTO != null
        }
    },
    HAS_ACCESS_TO_FILE("This file does not exist or it has been deleted.", HttpStatus.NOT_FOUND) {
        override fun isMet(ctx: Context): Boolean {
            val fileRepository: FileRepository by inject()

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

            val contains = fileRepository.fileCache.getIfPresent(fileId) != null

            if (!contains)
                Logger.tags("Requirements").info("File not cached")
            else
                Logger.tags("Requirements").info("File cached")

            val file = fileRepository.fileCache.get(fileId) ?: return false

            if (!file.isOwnedByUserId(ctx.currentUserDTO?.id)) {
                Logger.tags("Requirements").error("File not found: access manager")
                return false
            }

            ctx.fileId = file.id

            return true
        }
    },
    HAS_ACCESS_TO_SHARE("This share does not exist or it has been deleted.", HttpStatus.NOT_FOUND) {
        override fun isMet(ctx: Context): Boolean {
            val shareId = ctx.pathParamAsClass<String>("shareId").get()
            val shareRepository: ShareRepository by inject()

            logTimeSpent("Getting share by id", "Requirements") {
                val share =
                    if (shareId.isUUID)
                        shareRepository.getShareById(UUID.fromString(shareId))
                    else
                        shareRepository.getShareByName(shareId) ?: return false

                if (share == null) {
                    Logger.tags("Requirements").info("Share not found: access manager (actually not found)")
                    return false
                }

                if (ctx.method() == HandlerType.DELETE && ctx.currentUserDTO?.id != share.user) {
                    Logger.tags("Requirements").info("Share not found: access manager (user not owner)")
                    return false
                }

                Logger.tags("Requirements").trace("Setting shareDTO and DAO to request attribute")
                ctx.share = share
                return true
            }
        }
    },
    IS_VALID_API_KEY("Invalid API key.", HttpStatus.UNAUTHORIZED) {
        override fun isMet(ctx: Context): Boolean {
            val header = ctx.header("Authorization")

            if (header.isNullOrBlank() || !header.startsWith("Bearer "))
                return false

            val apiKey = header.split(" ")[1]

            val apiKeyRepository: ApiKeyRepository by inject()

            val apiKeyDto = apiKeyRepository.getApiKeyByApiKey(apiKey)
                ?: return false

            ctx.apiKeyUsed = apiKeyDto
            return true
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