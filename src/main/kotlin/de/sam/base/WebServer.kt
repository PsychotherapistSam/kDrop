package de.sam.base

import de.sam.base.config.Configuration.Companion.config
import de.sam.base.controllers.AuthenticationController
import de.sam.base.controllers.FileController
import de.sam.base.controllers.UserController
import de.sam.base.pages.ErrorPage
import de.sam.base.pages.IndexPage
import de.sam.base.pages.admin.AdminIndexPage
import de.sam.base.pages.admin.AdminUserEditPage
import de.sam.base.pages.admin.AdminUserViewPage
import de.sam.base.pages.admin.AdminUsersPage
import de.sam.base.pages.user.UserEditPage
import de.sam.base.pages.user.UserFilesPage
import de.sam.base.pages.user.UserLoginPage
import de.sam.base.pages.user.UserRegistrationPage
import de.sam.base.users.UserRoles
import de.sam.base.utils.CustomAccessManager
import de.sam.base.utils.session.Session
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.core.validation.JavalinValidation
import io.javalin.http.HttpResponseException
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.template.JavalinJte
import java.nio.file.Path
import java.util.*


class WebServer {
    fun start() {
        /*transaction {
            addLogger(StdOutSqlLogger)
            logTimeSpent("adding 5000 testfiles 5") {
                val owner = UserDAO.find { UsersTable.name eq "Sam" }.first()
                val fileOne = FileDAO.new {
                    this.name = "testfolder5 (5k files)"
                    this.path = "/testfolder5 (5k files)"
                    this.parent = null
                    this.owner = owner
                    this.size = 0
                    this.sizeHR = "0 B"
                    this.password = null
                    this.private = false
                    this.created = DateTime.now()
                    this.isFolder = true
                }

                for (i in 0..5000) {
                    FileDAO.new {
                        this.name = "testfile$i.txt"
                        this.path = "/${fileOne.name}/$name"
                        this.parent = fileOne
                        this.owner = owner
                        this.size = Random().nextLong(2000000) + 15000
                        this.sizeHR = humanReadableByteCountBin(this.size)
                        this.password = null
                        this.private = false
                        this.created = DateTime.now()
                        this.isFolder = false
                    }
                }
            }
        }*/


        val app = Javalin.create { javalinConfig ->
            // javalinConfig.enableWebjars()

            // register jte.gg template renderer
            JavalinJte.configure(createTemplateEngine())
            // for uuid validation
            JavalinValidation.register(UUID::class.java) { UUID.fromString(it) }

            javalinConfig.sessionHandler { Session.sqlSessionHandler() }
            javalinConfig.registerPlugin(RouteOverviewPlugin("/admin/routes", UserRoles.ADMIN))
            javalinConfig.accessManager { handler, ctx, routeRoles ->
                CustomAccessManager().manage(handler, ctx, routeRoles)
            }
            javalinConfig.requestLogger { ctx, timeInMs ->
                println("${ctx.method()} ${ctx.path()} ${ctx.status()} ${timeInMs}ms")
            }

            javalinConfig.addStaticFiles {
                it.hostedPath = "/"
                it.directory = "/public"
                it.location = Location.CLASSPATH       // Location.CLASSPATH (jar) or Location.EXTERNAL (file system)
                it.precompress = false                 // if the files should be pre-compressed and cached in memory (optimization)
                it.aliasCheck = null                   // you can configure this to enable symlinks (= ContextHandler.ApproveAliases())
            }

//            javalinConfig.enableCorsForAllOrigins()
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
                        get("/", UserFilesPage())
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
                path("/files") {
                    // get("/", UserController()::getFiles, UserRoles.USER)
                    // before("/", FileController()::checkFile)
                    post("/", FileController()::uploadFile, UserRoles.USER)
                    put("/", FileController()::getFiles, UserRoles.USER)
                    delete("/", FileController()::deleteFiles, UserRoles.USER)
                    before("/{fileId}*", FileController()::getFileParameter)
                    path("/{fileId}") {
                        get("/", FileController()::getSingleFile)
                        delete("/", FileController()::deleteSingleFile, UserRoles.USER)
                        //put("/", FileController()::updateFile, UserRoles.USER)
                    }
                }
                path("/directories") {
                    post("/", FileController()::createDirectory, UserRoles.USER)
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