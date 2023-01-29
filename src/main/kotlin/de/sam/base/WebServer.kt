package de.sam.base

import com.fasterxml.jackson.datatype.joda.JodaModule
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.controllers.*
import de.sam.base.pages.ErrorPage
import de.sam.base.pages.admin.AdminIndexPage
import de.sam.base.pages.admin.AdminUserEditPage
import de.sam.base.pages.admin.AdminUserViewPage
import de.sam.base.pages.admin.AdminUsersPage
import de.sam.base.pages.user.*
import de.sam.base.pages.user.settings.UserEditPage
import de.sam.base.pages.user.settings.UserTOTPSettingsPage
import de.sam.base.users.UserRoles
import de.sam.base.utils.CustomAccessManager
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.session.Session
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Header
import io.javalin.http.HttpResponseException
import io.javalin.http.staticfiles.Location
import io.javalin.json.JavalinJackson
import io.javalin.plugin.bundled.RouteOverviewPlugin
import io.javalin.rendering.template.JavalinJte
import io.javalin.validation.JavalinValidation
import org.tinylog.kotlin.Logger
import java.nio.file.Path
import java.util.*


class WebServer {
    fun start() {
        Logger.debug("Creating javalin app")
        val app = Javalin.create { javalinConfig ->
            // javalinConfig.enableWebjars()

            // register jte.gg template renderer
            JavalinJte.init(createTemplateEngine())
            // for uuid validation
            JavalinValidation.register(UUID::class.java) { UUID.fromString(it) }

            javalinConfig.jetty.sessionHandler { Session.sqlSessionHandler() }
            javalinConfig.plugins.register(RouteOverviewPlugin("/admin/routes", UserRoles.ADMIN))

            // limit to one instance per webserver
            val customAccessManager = CustomAccessManager()

            javalinConfig.accessManager(customAccessManager::manage)
            javalinConfig.requestLogger.http { ctx, timeInMs ->
                Logger.info("${ctx.method()} ${ctx.path()} ${ctx.status()} ${timeInMs}ms")
            }

            javalinConfig.staticFiles.add {
                it.hostedPath = "/"
                it.directory = "/public"
                it.location = Location.CLASSPATH // Location.CLASSPATH (jar) or Location.EXTERNAL (file system)
                it.precompress = false // if the files should be pre-compressed and cached in memory (optimization)
                it.aliasCheck = null // you can configure this to enable symlinks (= ContextHandler.ApproveAliases())
            }


            // dos with large files
//            javalinConfig.autogenerateEtags = false

            val jackson = JavalinJackson.defaultMapper().apply { registerModule(JodaModule()) }
            javalinConfig.jsonMapper(JavalinJackson(jackson))
//            javalinConfig.enableCorsForAllOrigins()
        }.start(config.port)

        Logger.debug("Registering Javalin route handlers")
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
                            Logger.debug("Route already set: ${metaInfo.path}, not overwriting")
                        }
                    }
                }
            }
        }

        Logger.debug("Registering Javalin exception handlers")
        app.exception(HttpResponseException::class.java) { e, ctx ->
            if (ctx.header(Header.ACCEPT)?.contains("application/json") == true
                || ctx.header("x-client")?.equals("web/api") == true
            ) {
                ctx.status(e.status)
                ctx.json(arrayOf(e.message))
            } else {
                ErrorPage(e).handle(ctx)
            }
        }

        Logger.debug("Registering Javalin routes")
        app.routes {
            before("*") { ctx ->
                ctx.header(
                    "Content-Security-Policy",
                    "default-src 'self'  https://www.google.com; font-src data: https://cdn.jsdelivr.net; img-src 'self'; object-src 'none'; script-src 'self' https://cdn.jsdelivr.net https://releases.transloadit.com https://www.google.com https://www.gstatic.com; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://releases.transloadit.com; frame-ancestors 'self'"
                )
                ctx.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
                ctx.header("X-Frame-Options", "SAMEORIGIN")
                ctx.header("X-Content-Type-Options", "nosniff")
                ctx.header("Referrer-Policy", "no-referrer")
            }
            get("/") { ctx ->
                if (ctx.isLoggedIn) {
                    ctx.redirect(UserFilesPage.ROUTE)
                } else {
                    ctx.redirect(UserLoginPage.ROUTE)
                }
            }
            get("/login", UserLoginPage())
            post("/login", UserLoginPage())
            get("/registration", UserRegistrationPage())
            post("/registration", UserRegistrationPage())
            path("/user") {
                path("/settings") {
                    get("/", UserEditPage(), UserRoles.USER)
                    get("/totp", UserTOTPSettingsPage(), UserRoles.USER)
                    post("/totp", UserTOTPSettingsPage(), UserRoles.USER)
                    delete("/totp", UserTOTPSettingsPage(), UserRoles.USER)
                }
                get("/payment", UserPaymentPage(), UserRoles.USER)
                path("/files") {
                    get("/", UserFilesPage(), UserRoles.USER, UserRoles.FILE_ACCESS_CHECK)
                    path("/{fileId}") {
                        get("/", UserFilesPage(), UserRoles.FILE_ACCESS_CHECK)
                        get("/shares", UserSharePage()::shareList, UserRoles.FILE_ACCESS_CHECK)
                    }
                }
                path("/totp") {
                    get("/validate", UserTOTPValidatePage())
                    post("/validate", UserTOTPValidatePage())
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
            get("/s/{shareId}", UserSharePage(), UserRoles.SHARE_ACCESS_CHECK)
        }

        // https://stackoverflow.com/a/7260540
        app.routes {
            path("/api/v1") {
                path("/session") {
//                    post(AuthenticationController()::loginRequest)
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
                path("/files") {
                    // get("/", UserController()::getFiles, UserRoles.USER)
                    // before("/", FileController()::checkFile)
                    post("/", FileController()::uploadFile, UserRoles.USER)
                    put("/", FileController()::getFiles, UserRoles.USER)
                    delete("/", FileController()::deleteFiles, UserRoles.USER)
                    // before("/{fileId}*", FileController()::getFileParameter)
                    path("/{fileId}") {
                        get("/", FileController()::getSingleFile, UserRoles.FILE_ACCESS_CHECK)
                        put("/", FileController()::updateFile, UserRoles.USER, UserRoles.FILE_ACCESS_CHECK)
                        delete("/", FileController()::deleteSingleFile, UserRoles.USER, UserRoles.FILE_ACCESS_CHECK)

                        post(
                            "/setAsChildren",
                            FileController()::moveFiles,
                            UserRoles.USER,
                            UserRoles.FILE_ACCESS_CHECK,
                            UserRoles.FILE_ACCESS_CHECK_ALLOW_HOME
                        )
                        //put("/", FileController()::updateFile, UserRoles.USER)
                    }
                }

                path("/directories") {
                    //TODO: move this to the files post (uploadFile) but accept no file if it has to be a directory
                    post("/", FileController()::createDirectory, UserRoles.USER)
                }

                path("/shares") {
                    post("/", ShareController()::create, UserRoles.USER)
                    path("/{shareId}") {
                        get("/", ShareController()::getOne, UserRoles.USER, UserRoles.SHARE_ACCESS_CHECK)
                        get("/download", FileController()::getSingleFile, UserRoles.SHARE_ACCESS_CHECK)
                        delete("/", ShareController()::delete, UserRoles.USER, UserRoles.SHARE_ACCESS_CHECK)
                    }
//                    crud("/{shareId}", ShareController(), UserRoles.SHARE_ACCESS_CHECK)
                }
                path("/payments") {
                    post("/create-intent", PaymentController()::createIntent, UserRoles.USER)
                    get("/finish", PaymentController()::finishPayment, UserRoles.USER)
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