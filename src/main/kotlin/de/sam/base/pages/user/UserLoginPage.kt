package de.sam.base.pages.user

import de.sam.base.Page
import de.sam.base.authentication.AuthenticationResult
import de.sam.base.authentication.AuthenticationService
import de.sam.base.authentication.log.LoginLogRepository
import de.sam.base.user.UserRepository
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

    private val loginLogRepository: LoginLogRepository by inject()
    private val authenticationService: AuthenticationService by inject()
    private val userRepository: UserRepository by inject()

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

            if (captcha.isActiveOnPage(this)) {
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
                    ctx.req().changeSessionId() // prevent session fixation attacks

                    ctx.currentUserDTO = result.userDTO

                    loginLogRepository.logLoginForUserId(ctx, result.userDTO.id, DateTime.now())

                    if (result.requireTOTPValidation) {
                        ctx.needsToVerifyTOTP = true
                        ctx.hxRedirect(UserTOTPValidatePage.ROUTE)
                    } else {
                        ctx.needsToVerifyTOTP = false
                        ctx.hxRedirect(returnToUrl)
                    }
                }

                is AuthenticationResult.Failure -> {
                    lastTryUsername = username
                    errors.addAll(result.errors)

                    // check if the user exists
                    val user = userRepository.getUserByUsername(username)
                    if(user != null) {
                        loginLogRepository.logLoginForUserId(ctx, user.id, DateTime.now(), true)
                    }
                }
            }

            return@prolongAtLeast
        }
    }
}