package de.sam.base

import com.fasterxml.jackson.datatype.joda.JodaModule
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.controllers.AuthenticationController
import de.sam.base.controllers.FileController
import de.sam.base.controllers.ShareController
import de.sam.base.controllers.UserController
import de.sam.base.pages.ChangelogPage
import de.sam.base.pages.ErrorPage
import de.sam.base.pages.SetupPage
import de.sam.base.pages.admin.AdminIndexPage
import de.sam.base.pages.admin.AdminUserEditPage
import de.sam.base.pages.admin.AdminUserViewPage
import de.sam.base.pages.admin.AdminUsersPage
import de.sam.base.pages.user.*
import de.sam.base.pages.user.settings.UserEditPage
import de.sam.base.pages.user.settings.UserLoginLogSettingsPage
import de.sam.base.pages.user.settings.UserTOTPSettingsPage
import de.sam.base.requirements.Requirement
import de.sam.base.services.FileService
import de.sam.base.services.LoginLogService
import de.sam.base.services.ShareService
import de.sam.base.users.UserRoles
import de.sam.base.utils.CustomAccessManager
import de.sam.base.utils.currentUserDTO
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
            JavalinJte.init(createTemplateEngine())
            JavalinValidation.register(UUID::class.java) { UUID.fromString(it) }

            javalinConfig.jetty.sessionHandler { Session.sqlSessionHandler(config.devEnvironment) }
            javalinConfig.plugins.register(RouteOverviewPlugin("/admin/routes", UserRoles.ADMIN))

            val customAccessManager = CustomAccessManager()

            javalinConfig.accessManager(customAccessManager::manage)
            javalinConfig.requestLogger.http { ctx, timeInMs ->
                Logger.info("${ctx.method()} ${ctx.path()} ${ctx.status()} ${timeInMs}ms")
            }

            javalinConfig.staticFiles.add {
                it.hostedPath = "/"
                it.directory = "/public"
                it.location = Location.CLASSPATH
                it.precompress = true
                it.aliasCheck = null
            }

            val jackson = JavalinJackson.defaultMapper().apply { registerModule(JodaModule()) }
            javalinConfig.jsonMapper(JavalinJackson(jackson))
        }.start(config.port)

        val loginLogService = LoginLogService()
        val fileService = FileService()
        val shareService = ShareService()

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
            get("/changelog", ChangelogPage())
            get("/login", UserLoginPage(loginLogService))
            post("/login", UserLoginPage(loginLogService))
            get("/registration", UserRegistrationPage())
            post("/registration", UserRegistrationPage())
            path("/user") {
                get("/quota", { ctx ->
                    val file = fileService.getRootFolderForUser(ctx.currentUserDTO!!.id)
                    ctx.render("components/usageQuotaComponent.kte", Collections.singletonMap("file", file))
                }, Requirement.IS_LOGGED_IN)
                path("/settings") {
                    get("/", UserEditPage(), UserRoles.USER)
                    get("/totp", UserTOTPSettingsPage(), UserRoles.USER)
                    post("/totp", UserTOTPSettingsPage(), UserRoles.USER)
                    delete("/totp", UserTOTPSettingsPage(), UserRoles.USER)
                    get("/loginHistory", UserLoginLogSettingsPage(loginLogService), UserRoles.USER)
                }
                get("/shares", UserSharesPage(shareService), UserRoles.USER)
                get("/search", FileController(fileService)::performFileSearch, UserRoles.USER)
                path("/files") {
                    get(
                        "/",
                        UserFilesPage(fileService),
                        UserRoles.USER,
                        Requirement.HAS_ACCESS_TO_FILE,
                        Requirement.IS_LOGGED_IN
                    )
                    path("/{fileId}") {
                        get("/", UserFilesPage(fileService), Requirement.HAS_ACCESS_TO_FILE)
                        get("/shares", UserSharePage()::shareList, Requirement.HAS_ACCESS_TO_FILE)
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
                    before("/{userId}*", UserController(fileService)::getUserParameter)
                    path("/{userId}") {
                        get("/", AdminUserViewPage(), UserRoles.ADMIN)
                        get("/edit", AdminUserEditPage(), UserRoles.ADMIN)
                    }
                }
            }
            path("/setup") {
                get("/", SetupPage(), Requirement.IS_IN_SETUP_STAGE)
                post("/", SetupPage(), Requirement.IS_IN_SETUP_STAGE)
            }
            get("/s/{shareId}", UserSharePage(), Requirement.HAS_ACCESS_TO_SHARE)
        }

        // https://stackoverflow.com/a/7260540
        app.routes {
            path("/api/v1") {
                path("/session") {
                    delete(AuthenticationController()::logoutRequest)
                }
                path("/users") {
                    before("/{userId}*", UserController(fileService)::getUserParameter)
                    path("/{userId}") {
                        delete("/", UserController(fileService)::deleteUserFromContext, UserRoles.SELF, UserRoles.ADMIN)
                        put("/", UserController(fileService)::updateUser, UserRoles.SELF, UserRoles.ADMIN)
                    }
                }
                path("/files") {
                    post("/", FileController(fileService)::uploadFile, UserRoles.USER)
                    put("/", FileController(fileService)::getFiles, UserRoles.USER)
                    delete("/", FileController(fileService)::deleteFiles, UserRoles.USER)
                    path("/{fileId}") {
                        get("/", FileController(fileService)::getSingleFile, Requirement.HAS_ACCESS_TO_FILE)
                        put(
                            "/",
                            FileController(fileService)::updateFile,
                            UserRoles.USER,
                            Requirement.HAS_ACCESS_TO_FILE
                        )
                        delete(
                            "/",
                            FileController(fileService)::deleteSingleFile,
                            UserRoles.USER,
                            Requirement.HAS_ACCESS_TO_FILE
                        )

                        get("/metadata", FileController(fileService)::getFileMetadata, Requirement.HAS_ACCESS_TO_FILE)

                        post(
                            "/setAsChildren",
                            FileController(fileService)::moveFiles,
                            UserRoles.USER,
                            Requirement.HAS_ACCESS_TO_FILE,
                            UserRoles.FILE_ACCESS_CHECK_ALLOW_HOME
                        )
                    }
                }

                path("/directories") {
                    //TODO: move this to the files post (uploadFile) but accept no file if it has to be a directory
                    post("/", FileController(fileService)::createDirectory, Requirement.IS_LOGGED_IN)
                    get("/root", FileController(fileService)::getRootDirectory, Requirement.IS_LOGGED_IN)
                }

                path("/shares") {
                    post("/", ShareController()::create, UserRoles.USER)
                    path("/{shareId}") {
                        get("/", ShareController()::getOne, UserRoles.USER, Requirement.HAS_ACCESS_TO_SHARE)
                        get("/download", FileController(fileService)::getSingleFile, Requirement.HAS_ACCESS_TO_SHARE)
                        delete("/", ShareController()::delete, UserRoles.USER, Requirement.HAS_ACCESS_TO_SHARE)
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