package de.sam.base

import de.sam.base.controllers.AuthenticationController
import de.sam.base.pages.IndexPage
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import io.javalin.Javalin
import io.javalin.plugin.rendering.template.JavalinJte
import java.nio.file.Path
import io.javalin.apibuilder.ApiBuilder.*
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.controllers.UserController
import de.sam.base.pages.ErrorPage
import de.sam.base.pages.admin.AdminIndexPage
import de.sam.base.pages.admin.AdminUserEditPage
import de.sam.base.pages.user.UserEditPage
import de.sam.base.pages.admin.AdminUserViewPage
import de.sam.base.pages.admin.AdminUsersPage
import de.sam.base.pages.user.UserFilesPage
import de.sam.base.pages.user.UserLoginPage
import de.sam.base.pages.user.UserRegistrationPage
import de.sam.base.users.UserRoles
import de.sam.base.utils.CustomAccessManager
import de.sam.base.utils.session.Session
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.core.validation.JavalinValidation
import io.javalin.http.HttpResponseException
import io.javalin.http.UnauthorizedResponse
import java.util.*

class WebServer {
    fun start() {
        val app = Javalin.create { javalinConfig ->
            // javalinConfig.enableWebjars()

            // register jte.gg template renderer
            JavalinJte.configure(createTemplateEngine())
            // for userId validation
            JavalinValidation.register(UUID::class.java) { UUID.fromString(it) }

            javalinConfig.sessionHandler { Session.fileSessionHandler() }
            javalinConfig.registerPlugin(RouteOverviewPlugin("/admin/routes", UserRoles.ADMIN));
            javalinConfig.accessManager { handler, ctx, routeRoles ->
                CustomAccessManager().manage(handler, ctx, routeRoles)
            }
        }.start(config.port)

        app.events {
            it.handlerAdded { metaInfo ->
                if (metaInfo.handler is Page) {
                    // set route variable dynamically (cursed)
                    val routeField = metaInfo.handler.javaClass.getField("ROUTE")
                    if (routeField != null) {
                        val value = routeField.get(String)
                        if (value == null || value.toString().isEmpty()) {
                            routeField.set(String, metaInfo.path)
                        } else {
                            println("Route already set: ${metaInfo.path}, not overwriting")
                        }
                    }
                }
            }
        }

        app.exception(HttpResponseException::class.java) { e, ctx ->
            if (ctx.header("Accept")?.contains("application/json") == true) {
                ctx.status(e.status)
                ctx.json(arrayOf(e.message))
            } else {
                ErrorPage(e).handle(ctx)
            }
        }

        app.routes {
            get("/", IndexPage(), UserRoles.USER)
            get("/login", UserLoginPage())
            post("/login", UserLoginPage())
            get("/registration", UserRegistrationPage())
            path("/user") {
                get("/settings", UserEditPage(), UserRoles.USER)
                path("/files") {
                    get("/", UserFilesPage(), UserRoles.USER)
                    path("/{fileId}") {
                        get("/", UserFilesPage(), UserRoles.USER)
                    }
                }
            }
            path("/admin") {
                get("/", AdminIndexPage(), UserRoles.ADMIN)
                path("/users") {
                    get("/", AdminUsersPage(), UserRoles.ADMIN)

                    before("/{userId}*", UserController()::getUserParameter)
                    path("/{userId}") {
                        get("/", AdminUserViewPage(), UserRoles.ADMIN)
                        get("/edit", AdminUserEditPage(), UserRoles.ADMIN)
                    }
                }
            }
        }

        // https://stackoverflow.com/a/7260540
        app.routes {
            path("/api/v1") {
                path("/session") {
                    post(AuthenticationController()::loginRequest)
                    delete(AuthenticationController()::logoutRequest)
                    // crud
                }
                path("/users") {
                    // crud stuff
                    post(AuthenticationController()::registrationRequest)
                    before("/{userId}*", UserController()::getUserParameter)
                    path("/{userId}") {
                        delete("/", UserController()::deleteUser, UserRoles.SELF, UserRoles.ADMIN)
                        // get(UserController()::getUser)
                        put("/", UserController()::updateUser, UserRoles.SELF, UserRoles.ADMIN)
                    }
                }
            }
        }
    }

    // https://github.com/casid/jte-javalin-tutorial/blob/d75d550cd6cd1dd33fcf461047851409e18a0525/src/main/java/app/App.java#L51
    private fun createTemplateEngine(): TemplateEngine {
        if (false) {
            val codeResolver = DirectoryCodeResolver(Path.of("src", "main", "jte"))
            return TemplateEngine
                .create(codeResolver, ContentType.Html)

        } else {
            val templateEngine = TemplateEngine.createPrecompiled(ContentType.Html)
            templateEngine.setTrimControlStructures(true)
            return templateEngine
        }
    }
}