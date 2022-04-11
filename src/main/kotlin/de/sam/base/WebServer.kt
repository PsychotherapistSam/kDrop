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
import de.sam.base.pages.user.LoginPage
import de.sam.base.utils.session.Session

class WebServer {
    fun start() {
        val app = Javalin.create {
            // it.enableWebjars()
            it.sessionHandler { Session.fileSessionHandler() }
            JavalinJte.configure(createTemplateEngine())
        }.start(config.port)

        app.routes {
            get("/") { IndexPage(it, it.path()).render() }
            get("/login") { LoginPage(it, it.path()).render() }
        }

        // https://stackoverflow.com/a/7260540
        app.routes {
            path("api/v1") {
                path("session") {
                    post(AuthenticationController()::loginRequest)
                    delete(AuthenticationController()::logoutRequest)
                    // crud
                }
                path("users") {
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