package de.sam.base.utils

import de.sam.base.config.Configuration.Companion.config
import de.sam.base.database.FileDAO
import de.sam.base.database.FileDTO
import de.sam.base.database.toDTO
import de.sam.base.users.UserRoles
import de.sam.base.utils.logging.logTimeSpent
import io.javalin.core.security.AccessManager
import io.javalin.core.security.RouteRole
import io.javalin.core.util.Header
import io.javalin.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.tinylog.kotlin.Logger
import java.net.URI
import java.util.*
import kotlin.system.measureNanoTime
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class CustomAccessManager : AccessManager {
    val fileCache = mutableMapOf<UUID, Triple<Long, FileDAO, FileDTO>>()

    override fun manage(handler: Handler, ctx: Context, routeRoles: MutableSet<RouteRole>) {
        val routeRolesMap = routeRoles.map { it as UserRoles }
        val userAgentHeader = ctx.header(Header.USER_AGENT) ?: throw BadRequestResponse("User-Agent is missing")
        // Redirect safari users to a firefox download
        /*if (userAgentHeader.contains("Safari")) {
            ctx.redirect("https://firefox.com/download")
            return
        }*/

        // Register bots
        val listOfBotUserAgents = listOf("Googlebot")
        if (listOfBotUserAgents.any { it in userAgentHeader }) {
            ctx.attribute("isBot", true)
        }

        val time = measureTime {
            val info = UAgentInfo(userAgentHeader, ctx.header("Accepts"))
            info.initDeviceScan()
            ctx.attribute("isMobile", info.isTierIphone)
        }
        Logger.debug("detected useragent in in ${time.toDouble(DurationUnit.MILLISECONDS)}ms")

        if (config.enforceHost) {
            if (URI(ctx.url()).host != URI(config.host).host) {
                throw BadRequestResponse("Invalid host, expected ${URI(config.host).host} but got ${URI(ctx.url()).host}")
            }
        }

        if (routeRolesMap.any { !it.special }) {
            if (!ctx.isLoggedIn) {
                throw UnauthorizedResponse("You need to be logged in to access this resource.")
            }

            val maxUserRole = ctx.currentUserDTO!!.roles.maxOf { it.powerLevel }
            val minReqiredRole = routeRolesMap
//                .filter { !it.hidden }
                .minOf { it.powerLevel }

            val reachesRoleRequirement = maxUserRole >= minReqiredRole

            if (routeRolesMap.contains(UserRoles.SELF) && ctx.pathParam("userId") != null) {
                if (ctx.currentUserDTO!!.id != UUID.fromString(ctx.pathParam("userId")) && !reachesRoleRequirement) {
                    // you can't access other users' resources if "self" is set
                    throw UnauthorizedResponse(
                        "You are not authorized to access this resource."
                    )
                }
            }

            if (!reachesRoleRequirement) {
                val minRole = routeRolesMap.minByOrNull { it.powerLevel }
                // val minRoleName = (routeRolesMap.map { it as UserRoles }).minByOrNull { it.powerLevel }!!.name

                throw UnauthorizedResponse(
                    "You are not authorized to access this resource.",
                    hashMapOf("minimumRole" to minRole!!.name)
                ) //You need to be at least $minRole")
            }
/*                    // check if ctx.currentUser.roles has any role in routeRolesMap
                    if (!routeRolesMap.any { ctx.currentUser!!.roles.contains(it) }) {
                        throw UnauthorizedResponse("You are not authorized to access this resource")
                    }*/
        }

        //TODO: check access to parent file/folder

        if (routeRolesMap.contains(UserRoles.FILE_ACCESS_CHECK)) {
            val userQueryTime = measureNanoTime {
                val fileId = ctx.pathParamAsClass<UUID>("fileId")
                    .get()

                if (fileCache.containsKey(fileId) && System.currentTimeMillis() < fileCache[fileId]!!.first + 1000 * 10) {
                    ctx.fileDAOFromId = fileCache[fileId]!!.second
                    ctx.fileDTOFromId = fileCache[fileId]!!.third
                } else {
                    if (fileCache.containsKey(fileId)) {
                        fileCache.remove(fileId)
                    }
                    transaction {
                        logTimeSpent("Getting file by id") {
                            val fileDAO = FileDAO.findById(fileId)
                            if (fileDAO != null) {
                                val fileDTO = fileDAO.toDTO()
                                Logger.trace("Setting fileDTO and DAO to request attribute")
                                ctx.fileDAOFromId = fileDAO
                                ctx.fileDTOFromId = fileDTO
                                fileCache[fileId] = Triple(System.currentTimeMillis(), fileDAO, fileDTO!!)
                            }
                        }
                    }
                }

                if (ctx.fileDTOFromId != null && !ctx.fileDTOFromId!!.canBeViewedByUserId(ctx.currentUserDTO?.id)) {
                    Logger.error("File not found: access manager")
                    throw NotFoundResponse("File not found")
                }
            }
            ctx.attribute("fileQueryTime", userQueryTime)
        }
        handler.handle(ctx)
    }
}