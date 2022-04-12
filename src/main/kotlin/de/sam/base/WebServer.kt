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
import de.sam.base.pages.ErrorPage
import de.sam.base.pages.user.UserLoginPage
import de.sam.base.pages.user.UserSettingsPage
import de.sam.base.users.UserRoles
import de.sam.base.utils.currentUser
import de.sam.base.utils.session.Session
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.http.HttpResponseException
import io.javalin.http.UnauthorizedResponse

class WebServer {
    fun start() {
        val app = Javalin.create { javalinConfig ->
            // javalinConfig.enableWebjars()
            javalinConfig.sessionHandler { Session.fileSessionHandler() }
            javalinConfig.registerPlugin(RouteOverviewPlugin("/routes"));
            JavalinJte.configure(createTemplateEngine())

            javalinConfig.accessManager { handler, ctx, routeRoles ->
                if (routeRoles.isNotEmpty()) {
                    if (ctx.currentUser != null) {
                        val maxUserRole = ctx.currentUser!!.roles.maxOf { it.powerLevel }
                        val minReqiredRole = routeRoles.minOf { (it as UserRoles).powerLevel }
                        if (maxUserRole < minReqiredRole) {
                            val minRole = routeRoles.map { it as UserRoles }.minByOrNull { it.powerLevel }
                            // val minRoleName = (routeRoles.map { it as UserRoles }).minByOrNull { it.powerLevel }!!.name
                            throw UnauthorizedResponse(
                                "You are not authorized to access this resource.",
                                hashMapOf("minimumRole" to minRole!!.name)
                            ) //You need to be at least $minRole")
                        }
/*                    // check if ctx.currentUser.roles has any role in routeRoles
                    if (!routeRoles.any { ctx.currentUser!!.roles.contains(it) }) {
                        throw UnauthorizedResponse("You are not authorized to access this resource")
                    }*/
                    } else {
                        throw UnauthorizedResponse("You need to be logged in to access this resource.")
                    }
                }
                handler.handle(ctx)
            }
        }.start(config.port)


        app.events {
            it.handlerAdded { metaInfo ->
                if (metaInfo.handler is Page) {
                    // set route variable dynamically (cursed)
                    val routeField = metaInfo.handler.javaClass.getField("ROUTE")
                    if (routeField != null) {
                        routeField.set(String, metaInfo.path)
                    }
                }
            }
        }

        app.exception(HttpResponseException::class.java) { e, ctx ->
            ErrorPage(e).handle(ctx)
        }

        app.routes {
            get("/", IndexPage())
            get("/login", UserLoginPage())
            path("/user") {
                get("/settings", UserSettingsPage(), UserRoles.USER)
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
                }
            }
        }
    }

    // https://github.com/casid/jte-javalin-tutorial/blob/d75d550cd6cd1dd33fcf461047851409e18a0525/src/main/java/app/App.java#L51
    private fun createTemplateEngine(): TemplateEngine {
        return if (false) {
            val codeResolver = DirectoryCodeResolver(Path.of("src", "de.sam.base.main", "jte"))
            TemplateEngine.create(codeResolver, ContentType.Html)
        } else {
            return TemplateEngine.createPrecompiled(ContentType.Html)
        }
    }
}