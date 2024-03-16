package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.authentication.AuthenticationResult
import de.sam.base.authentication.AuthenticationService
import de.sam.base.user.UserRoles
import de.sam.base.utils.*
import io.javalin.http.ForbiddenResponse
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject

class UserRegistrationPage : Page(
    name = "Registration",
    templateName = "user/registration.kte",
) {
    companion object {
        const val ROUTE: String = "/registration"
    }

    private val authenticationService: AuthenticationService by inject()

    private val rateLimiter: RateLimiter by inject()

    private var lastTryUsername: String = ""
    var errors: MutableList<String> = mutableListOf()

    override fun before() {
        if (ctx.isLoggedIn && ctx.currentUserDTO!!.getHighestRolePowerLevel() < UserRoles.ADMIN.powerLevel) {
            throw ForbiddenResponse("You are already registered.")
        } else if (!ctx.isLoggedIn && !config.allowUserRegistration) {
            throw ForbiddenResponse("User registration is currently disabled.")
        }
    }

    override fun post() {
        prolongAtLeast(2000) {
            val canBypassCaptcha = ctx.isLoggedIn && ctx.currentUserDTO!!.roles.contains(UserRoles.ADMIN)
            val dontSetSession = canBypassCaptcha && ctx.header("No-Session") == "true"

            if (ctx.isLoggedIn && !canBypassCaptcha) {
                ctx.hxRedirect("/")
                return@prolongAtLeast
            }

            val username = ctx.formParam("username")
            val password = ctx.formParam("password")

            val taken = runBlocking {
                rateLimiter.authentication.tryTake(ctx.realIp)
            }

            if (!taken) {
                errors.add("Too many registration attempts. Please try again later.")
                return@prolongAtLeast
            }

            if (!canBypassCaptcha && captcha.isActiveOnPage(this)) {
                val captchaErrors = captcha.validate(ctx)
                if (captchaErrors.isNotEmpty()) {
                    lastTryUsername = username ?: ""
                    errors.addAll(captchaErrors)
                    return@prolongAtLeast
                }
            }

            val result = authenticationService.register(username = username!!, password = password!!)

            when (result) {
                is AuthenticationResult.Success -> {
                    if (!dontSetSession) {
                        ctx.currentUserDTO = result.userDTO
                        ctx.hxRedirect("/")
                    }
                }

                is AuthenticationResult.Failure -> {
                    lastTryUsername = username
                    errors.addAll(result.errors)
                }
            }

            return@prolongAtLeast
        }
    }
}