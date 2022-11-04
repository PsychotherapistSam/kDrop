package de.sam.base.pages.user

import com.password4j.Password
import de.sam.base.Page
import de.sam.base.captcha.Captcha
import de.sam.base.config.Configuration
import de.sam.base.controllers.AuthenticationController.Companion.argon2Instance
import de.sam.base.controllers.validateRegistrationAttempt
import de.sam.base.database.FileDAO
import de.sam.base.database.UserDAO
import de.sam.base.database.toDTO
import de.sam.base.users.UserRoles
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.hxRedirect
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.prolongAtLeast
import io.javalin.http.ForbiddenResponse
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class UserRegistrationPage : Page(
    name = "Registration",
    templateName = "user/registration.kte",
) {
    companion object {
        lateinit var ROUTE: String
    }

    var lastTryUsername: String = ""
    var errors: MutableList<String> = mutableListOf()

    override fun before() {
        errors.clear()
        lastTryUsername = ""

        if (ctx.isLoggedIn && ctx.currentUserDTO!!.getHighestRolePowerLevel() < UserRoles.ADMIN.powerLevel) {
            throw ForbiddenResponse("You are already registered.")
        } else if (!ctx.isLoggedIn && !Configuration.config.allowUserRegistration) {
            throw ForbiddenResponse("User registration is currently disabled.")
        }
    }

    override fun post() {
        prolongAtLeast(2000) {
            if (ctx.isLoggedIn) {
                ctx.hxRedirect("/")
                return@prolongAtLeast
            }

            val username = ctx.formParam("username")
            val password = ctx.formParam("password")

            if (Configuration.config.captcha.enabled && Configuration.config.captcha.locations.contains("registration")) {
                when (Configuration.config.captcha.service.lowercase()) {
                    "recaptcha" -> {
                        val captchaErrors = Captcha.validate(ctx)
                        if (captchaErrors.isNotEmpty()) {
                            //TODO: reset username field if captcha is not valid
                            lastTryUsername = username ?: ""
                            errors.addAll(captchaErrors)
                            return@prolongAtLeast
                        }
                    }
                }
            }

            val attempt = validateRegistrationAttempt(username, password)
            // first = user, second = errors
            if (attempt.second.isNotEmpty()) {
                lastTryUsername = username ?: ""
                errors.add(attempt.second.first())
                return@prolongAtLeast
            }

            val userDAO = transaction {
                val user = UserDAO.new {
                    this.name = username!!
                    this.password = Password.hash(password)
                        .addSalt("${this.id}") // argon2id salts the passwords on itself, but better safe than sorry
                        .addPepper(Configuration.config.passwordPepper)
                        .with(argon2Instance)
                        .result
                    this.roles = UserRoles.USER.name
                    this.hidden = false
                    this.preferences = ""
                    this.registrationDate = DateTime.now()
                }

                val rootFolder = FileDAO.new {
                    name = "My Files"
                    path = "/"
                    mimeType = ""
                    parent = null
                    owner = user
                    size = 0
                    sizeHR = "0 B"
                    private = true
                    created = DateTime.now()
                    isFolder = true
                    hash = null
                    isRoot = true
                }

                user.rootFolderId = rootFolder.id.value

                return@transaction user
            }
            ctx.currentUserDTO = userDAO.toDTO()
            ctx.hxRedirect("/")
            //ctx.redirect("/")
            return@prolongAtLeast
        }
    }
}