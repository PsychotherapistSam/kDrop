package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.authentication.AuthenticationResult
import de.sam.base.authentication.AuthenticationService
import de.sam.base.authentication.UserService
import de.sam.base.captcha.Captcha
import de.sam.base.services.LoginLogService
import de.sam.base.utils.*
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.koin.core.component.inject

class UserLoginPage : Page(
    name = "Login",
    templateName = "user/login.kte",
) {
    companion object {
        const val ROUTE: String = "/login"
    }

    private val captcha: Captcha by inject()

    private val loginLogService: LoginLogService by inject()
    private val authenticationService: AuthenticationService by inject()
    private val userService: UserService by inject()

    private val rateLimiter: RateLimiter by inject()

    var lastTryUsername: String = ""
    var errors: MutableList<String> = mutableListOf()

    override fun post() {
        val returnToUrl = ctx.loginReturnUrl ?: UserFilesPage.ROUTE
        prolongAtLeast(2000) {
            if (ctx.isLoggedIn) {
                ctx.hxRedirect(returnToUrl)
                return@prolongAtLeast
            }
            val username = ctx.formParam("username")
            val password = ctx.formParam("password")

            val taken = runBlocking {
                rateLimiter.authentication.tryTake(ctx.realIp)
            }

            if (!taken) {
                errors.add("Too many login attempts. Please try again later.")
                return@prolongAtLeast
            }

            if (config.captcha != null && config.captcha!!.locations.contains("login")) {
                val captchaErrors = captcha.validate(ctx)
                if (captchaErrors.isNotEmpty()) {
                    lastTryUsername = username ?: ""
                    errors.addAll(captchaErrors)
                    return@prolongAtLeast
                }
            }

            val result = authenticationService.login(username = username!!, password = password!!)

            when (result) {
                is AuthenticationResult.Success -> {
                    ctx.currentUserDTO = result.userDTO

                    val date = DateTime.now()

                    loginLogService.logLoginForUserId(ctx, result.userDTO.id, date)
                    userService.updateLastLoginTime(result.userDTO.id, date)

                    if (result.requireTOTPValidation) {
                        ctx.needsToVerifyTOTP = true
                        ctx.hxRedirect(UserTOTPValidatePage.ROUTE)
                    } else {
                        ctx.hxRedirect(returnToUrl)
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