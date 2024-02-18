package de.sam.base.utils

import de.sam.base.config.Configuration
import de.sam.base.requirements.Requirement
import de.sam.base.user.UserRepository
import de.sam.base.user.UserRoles
import io.javalin.http.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.net.URI
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class CustomAccessManager : KoinComponent {
    private val config: Configuration by inject()
    private val userRepository: UserRepository by inject()

    fun manage(ctx: Context) {
        val routeRoles = ctx.routeRoles()

        if (ctx.path().startsWith("/api/v1/payments")) {
            //TODO: change this, without this an error get's thrown from stripejs
            ctx.header(Header.CONTENT_SECURITY_POLICY, "default-src *")
        }

        val routeRolesMap = routeRoles.filterIsInstance<UserRoles>()
        val userAgentHeader = ctx.header(Header.USER_AGENT) ?: throw BadRequestResponse("User-Agent is missing")
        // Redirect safari users to a firefox download
        /*if (userAgentHeader.contains("Safari")) {
            ctx.redirect("https://firefox.com/download")
            return
        }*/

        // Register bots
        val listOfBotUserAgents = listOf("Googlebot")
        if (listOfBotUserAgents.any { it in userAgentHeader }) {
            ctx.isBot = true
        }

        val time = measureTime {
            val info = UAgentInfo(userAgentHeader, ctx.header("Accepts"))
            info.initDeviceScan()
            ctx.isMobileUser = info.isTierIphone
        }
        Logger.debug("detected useragent in in ${time.toDouble(DurationUnit.MILLISECONDS)}ms")

        if (config.enforceHost) {
            if (URI(ctx.url()).host != URI(config.host).host) {
                throw BadRequestResponse("Invalid host, expected ${URI(config.host).host} but got ${URI(ctx.url()).host}")
            }
        }

        // cache busting for sessions
        if (ctx.isLoggedIn) {
            val userId = ctx.currentUserDTO!!.id
            val sessionToken = ctx.tokenTime ?: -1
            val currentToken = CacheInvalidation.userTokens[userId] ?: -1

            if (sessionToken < currentToken) {
                val user = userRepository.getUserById(userId)
                if (user == null) {
                    ctx.currentUserDTO = null
                    ctx.req().session.invalidate()
                    return
                }

                ctx.currentUserDTO = user
                ctx.tokenTime = currentToken
            }
        }

        val requirements = routeRoles.filterIsInstance<Requirement>()
        requirements.forEach {
            val (isMet, duration) = measureTimedValue {
                it.isMet(ctx)
            }
            Logger.debug("Requirement ${it.name} took ${duration.toDouble(DurationUnit.MILLISECONDS)} ms")
            if (!isMet) {
                Logger.debug("Requirement ${it.name} failed")
                when (it.httpStatus) {
                    HttpStatus.NOT_FOUND -> throw NotFoundResponse(it.errorMessage)
                    HttpStatus.FORBIDDEN -> throw ForbiddenResponse(it.errorMessage)
                    HttpStatus.UNAUTHORIZED -> throw UnauthorizedResponse(it.errorMessage)
                    else -> throw InternalServerErrorResponse(it.errorMessage)
                }
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
                throw UnauthorizedResponse(
                    "You are not authorized to access this resource.",
                    hashMapOf("minimumRole" to minRole!!.name)
                )
            }
        }
    }
}