package de.sam.base

import com.fasterxml.jackson.datatype.joda.JodaModule
import de.sam.base.config.Configuration
import de.sam.base.controllers.AuthenticationController
import de.sam.base.controllers.ShareController
import de.sam.base.file.FileController
import de.sam.base.file.repository.FileRepository
import de.sam.base.pages.ChangelogPage
import de.sam.base.pages.ErrorPage
import de.sam.base.pages.SetupPage
import de.sam.base.pages.admin.AdminIndexPage
import de.sam.base.pages.admin.AdminUserEditPage
import de.sam.base.pages.admin.AdminUserViewPage
import de.sam.base.pages.admin.AdminUsersPage
import de.sam.base.pages.user.*
import de.sam.base.pages.user.settings.*
import de.sam.base.requirements.Requirement
import de.sam.base.tasks.TaskController
import de.sam.base.tasks.queue.TaskQueue
import de.sam.base.user.UserController
import de.sam.base.user.UserRoles
import de.sam.base.utils.CustomAccessManager
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.isLoggedIn
import de.sam.base.utils.session.Session
import gg.jte.TemplateEngine
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Header
import io.javalin.http.HttpResponseException
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.staticfiles.Location
import io.javalin.json.JavalinJackson
import io.javalin.rendering.template.JavalinJte
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.util.*


class WebServer : KoinComponent {
    private val fileRepository: FileRepository by inject()
    private val config: Configuration by inject()
    private val session: Session by inject()
    private val templateEngine: TemplateEngine by inject()
    private val taskQueue: TaskQueue by inject()
    private val taskController: TaskController by inject()

    fun start() {
        Logger.tag("Web").debug("Creating javalin app")
        val app = Javalin.create { javalinConfig ->

            javalinConfig.fileRenderer(JavalinJte(templateEngine))

            javalinConfig.validation.register(UUID::class.java) { UUID.fromString(it) }

            javalinConfig.jetty.modifyServletContextHandler { handler ->
                handler.sessionHandler = session.sessionHandler
            }

            // TODO: javalinConfig.registerPlugin(RouteOverviewPlugin("/admin/routes", UserRoles.ADMIN))

            javalinConfig.requestLogger.http { ctx, timeInMs ->
                Logger.tag("Web").info("${ctx.method()} ${ctx.path()} ${ctx.status()} ${timeInMs}ms")
            }

            javalinConfig.staticFiles.add {
                it.hostedPath = "/"
                it.directory = "/public"
                it.location = Location.CLASSPATH
                it.precompress = false
                it.aliasCheck = null
            }

            val jackson = JavalinJackson.defaultMapper().apply { registerModule(JodaModule()) }
            javalinConfig.jsonMapper(JavalinJackson(jackson))

            Logger.tag("Web").debug("Registering Page routes")
            javalinConfig.router.apiBuilder {
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
                get("/changelog") { ChangelogPage().handle(it) }
                get("/login") { UserLoginPage().handle(it) }
                post("/login") { UserLoginPage().handle(it) }
                get("/registration") { UserRegistrationPage().handle(it) }
                post("/registration") { UserRegistrationPage().handle(it) }
                path("/user") {
                    get("/quota", { ctx ->
                        val file = fileRepository.getRootFolderForUser(ctx.currentUserDTO!!.id)
                        ctx.render("components/usageQuotaComponent.kte", Collections.singletonMap("file", file))
                    }, Requirement.IS_LOGGED_IN)
                    path("/settings") {
                        get("/", { UserEditPage().handle(it) }, UserRoles.USER)
                        path("/totp") {
                            get("/", { UserTOTPSettingsPage().handle(it) }, UserRoles.USER)
                            post("/", { UserTOTPSettingsPage().handle(it) }, UserRoles.USER)
                            delete("/", { UserTOTPSettingsPage().handle(it) }, UserRoles.USER)
                        }
                        get("/loginHistory", { UserLoginLogSettingsPage().handle(it) }, UserRoles.USER)
                        path("/apiKeys") {
                            get("/", { UserApiKeysSettingsPage().handle(it) }, UserRoles.USER)
                            post("/", { UserApiKeysSettingsPage().handle(it) }, UserRoles.USER)
                            delete("/", { UserApiKeysSettingsPage().handle(it) }, UserRoles.USER)
                        }
                        get("/integrations", { UserIntegrationsSettingsPage().handle(it) }, UserRoles.USER)
                        post("/integrations", { UserIntegrationsSettingsPage().handle(it) }, UserRoles.USER)
                    }
                    path("/sessions") {
                        post("/revoke", { UserLoginLogSettingsPage().handle(it) }, UserRoles.USER)
                    }
                    get("/shares", { UserSharesPage().handle(it) }, UserRoles.USER)
                    get("/search", FileController()::performFileSearch, UserRoles.USER)
                    path("/files") {
                        get(
                            "/",
                            { UserFilesPage().handle(it) },
                            UserRoles.USER,
                            Requirement.HAS_ACCESS_TO_FILE,
                            Requirement.IS_LOGGED_IN
                        )
                        path("/{fileId}") {
                            get("/", { UserFilesPage().handle(it) }, Requirement.HAS_ACCESS_TO_FILE)
                            get("/shares", UserSharePage()::shareList, Requirement.HAS_ACCESS_TO_FILE)
                        }
                    }
                    path("/totp") {
                        get("/validate", { UserTOTPValidatePage().handle(it) }, Requirement.IS_LOGGED_IN)
                        post("/validate", { UserTOTPValidatePage().handle(it) }, Requirement.IS_LOGGED_IN)
                    }
                }
                path("/admin") {
                    get("/", { AdminIndexPage().handle(it) }, UserRoles.ADMIN)
                    path("/task") {
                        get("/", taskController::list, UserRoles.ADMIN)
                        sse("/active", taskController::handleSSE, UserRoles.ADMIN)
                        path("/{action}") {
                            post("/run", taskController::runSingle, UserRoles.ADMIN)
                        }
                    }
                    path("/users") {
                        get("/", { AdminUsersPage().handle(it) }, UserRoles.ADMIN)
                        //TODO: change this
                        before("/{userId}*", UserController()::getUserParameter)
                        path("/{userId}") {
                            get("/", { AdminUserViewPage().handle(it) }, UserRoles.ADMIN)
                            get("/edit", { AdminUserEditPage().handle(it) }, UserRoles.ADMIN)
                        }
                    }
                }
                path("/setup") {
                    get("/", { SetupPage().handle(it) }, Requirement.IS_IN_SETUP_STAGE)
                    post("/", { SetupPage().handle(it) }, Requirement.IS_IN_SETUP_STAGE)
                }
                get("/s/{shareId}", { UserSharePage().handle(it) }, Requirement.HAS_ACCESS_TO_SHARE)
            }

            // https://stackoverflow.com/a/7260540
            Logger.tag("Web").debug("Registering API routes")
            javalinConfig.router.apiBuilder {
                path("/api/v1") {
                    path("/session") {
                        delete(AuthenticationController()::logoutRequest, UserRoles.USER, Requirement.IS_LOGGED_IN)
                    }
                    path("/users") {
                        before("/{userId}*", UserController()::getUserParameter)
                        path("/{userId}") {
                            delete("/", UserController()::deleteUser, UserRoles.SELF, UserRoles.ADMIN)
                            put("/", UserController()::updateUser, UserRoles.SELF, UserRoles.ADMIN)
                        }
                    }
                    path("/integration") {
                        path("/sharex") {
                            post("/upload", FileController()::handleShareXUpload, Requirement.IS_VALID_API_KEY)
                        }
                    }
                    path("/files") {
                        path("/upload") {
                            get("/", FileController()::handleTUSUpload, UserRoles.USER)
                            post("/", FileController()::handleTUSUpload, UserRoles.USER)
                            head("/", FileController()::handleTUSUpload, UserRoles.USER)
                            path("/{fileId}") {
                                head("/", FileController()::handleTUSUpload, UserRoles.USER)
                                patch("/", FileController()::handleTUSUpload, UserRoles.USER)
                                delete("/", FileController()::handleTUSUpload, UserRoles.USER)
                            }
                        }
                        put("/", FileController()::getFiles, UserRoles.USER)
                        delete("/", FileController()::deleteFiles, UserRoles.USER)
                        path("/{fileId}") {
                            get("/", FileController()::getSingleFile, Requirement.HAS_ACCESS_TO_FILE)
                            put("/", FileController()::updateFile, UserRoles.USER, Requirement.HAS_ACCESS_TO_FILE)
                            delete(
                                "/",
                                FileController()::deleteSingleFile,
                                UserRoles.USER,
                                Requirement.HAS_ACCESS_TO_FILE
                            )

                            get("/metadata", FileController()::getFileMetadata, Requirement.HAS_ACCESS_TO_FILE)

                            post(
                                "/setAsChildren",
                                FileController()::moveFiles,
                                UserRoles.USER,
                                Requirement.HAS_ACCESS_TO_FILE,
                                UserRoles.FILE_ACCESS_CHECK_ALLOW_HOME
                            )

                            post(
                                "/hash",
                                FileController()::hashFile,
                                UserRoles.USER,
                                Requirement.HAS_ACCESS_TO_FILE
                            )
                        }
                    }

                    path("/directories") {
                        //TODO: move this to the files post (uploadFile) but accept no file if it has to be a directory
                        post("/", FileController()::createDirectory, Requirement.IS_LOGGED_IN)
                        get("/root", FileController()::getRootDirectory, Requirement.IS_LOGGED_IN)
                    }

                    path("/shares") {
                        post("/", ShareController()::create, UserRoles.USER)
                        path("/{shareId}") {
                            get("/", ShareController()::getOne, UserRoles.USER, Requirement.HAS_ACCESS_TO_SHARE)
                            get("/download", FileController()::getSingleFile, Requirement.HAS_ACCESS_TO_SHARE)
                            delete("/", ShareController()::delete, UserRoles.USER, Requirement.HAS_ACCESS_TO_SHARE)
                        }
                    }
                }
            }
        }.start(config.port)

        app.beforeMatched(CustomAccessManager()::manage)

        Logger.tag("Web").debug("Registering Task Queue Handler")
        taskQueue.onTaskStatusChange = taskController.onTaskStatusChange

        Logger.tag("Web").debug("Registering Javalin exception handlers")
        app.exception(HttpResponseException::class.java) { e, ctx ->
            if (e is InternalServerErrorResponse) {
                Logger.error(e.message)
                Logger.error(e)
            }

            if (ctx.header(Header.ACCEPT)?.contains("application/json") == true
                || ctx.header("x-client")?.equals("web/api") == true
            ) {
                ctx.status(e.status)
                ctx.json(arrayOf(e.message))
            } else {
                ErrorPage(e).handle(ctx)
            }
        }
        // register general error handler
        app.exception(Exception::class.java) { e, ctx ->
            Logger.tag("Web").error(e)
            Logger.tag("Web").error(e.message)
            ctx.status(500)
            ctx.json(arrayOf(e.message))
        }
    }
}
