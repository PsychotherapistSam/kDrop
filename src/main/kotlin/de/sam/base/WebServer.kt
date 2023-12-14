package de.sam.base

import com.fasterxml.jackson.datatype.joda.JodaModule
import de.sam.base.config.Configuration
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.kotlin.Logger
import java.nio.file.Path
import java.util.*


class WebServer : KoinComponent {
    private val fileService: FileService by inject()
    private val config: Configuration by inject()
    private val session: Session by inject()

    fun start() {
        Logger.debug("Creating javalin app")
        val app = Javalin.create { javalinConfig ->
            JavalinJte.init(createTemplateEngine())
            JavalinValidation.register(UUID::class.java) { UUID.fromString(it) }

            javalinConfig.jetty.sessionHandler { session.sessionHandler }
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
                it.precompress = false
                it.aliasCheck = null
            }

            val jackson = JavalinJackson.defaultMapper().apply { registerModule(JodaModule()) }
            javalinConfig.jsonMapper(JavalinJackson(jackson))
        }.start(config.port)


        Logger.debug("Registering Javalin exception handlers")
        app.exception(HttpResponseException::class.java) { e, ctx ->
            if (ctx.header(Header.ACCEPT)?.contains("application/json") == true || ctx.header("x-client")
                    ?.equals("web/api") == true
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
            get("/changelog") { ChangelogPage().handle(it) }
            get("/login") { UserLoginPage().handle(it) }
            post("/login") { UserLoginPage().handle(it) }
            get("/registration") { UserRegistrationPage().handle(it) }
            post("/registration") { UserRegistrationPage().handle(it) }
            path("/user") {
                get("/quota", { ctx ->
                    val file = fileService.getRootFolderForUser(ctx.currentUserDTO!!.id)
                    ctx.render("components/usageQuotaComponent.kte", Collections.singletonMap("file", file))
                }, Requirement.IS_LOGGED_IN)
                path("/settings") {
                    get("/", { UserEditPage().handle(it) }, UserRoles.USER)
                    get("/totp", { UserTOTPSettingsPage().handle(it) }, UserRoles.USER)
                    post("/totp", { UserTOTPSettingsPage().handle(it) }, UserRoles.USER)
                    delete("/totp", { UserTOTPSettingsPage().handle(it) }, UserRoles.USER)
                    get("/loginHistory", { UserLoginLogSettingsPage().handle(it) }, UserRoles.USER)
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
        app.routes {
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
                        delete("/", FileController()::deleteSingleFile, UserRoles.USER, Requirement.HAS_ACCESS_TO_FILE)

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
    }


    // https://github.com/casid/jte-javalin-tutorial/blob/d75d550cd6cd1dd33fcf461047851409e18a0525/src/main/java/app/App.java#L51
    private fun createTemplateEngine(): TemplateEngine {
        if (false) {
            val codeResolver = DirectoryCodeResolver(Path.of("src", "main", "jte"))
            return TemplateEngine.create(codeResolver, ContentType.Html)

        } else {
            val templateEngine = TemplateEngine.createPrecompiled(ContentType.Html)
            templateEngine.setTrimControlStructures(true)
            return templateEngine
        }
    }
}
