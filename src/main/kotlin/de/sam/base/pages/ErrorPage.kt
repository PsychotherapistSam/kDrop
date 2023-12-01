package de.sam.base.pages

import de.sam.base.Page
import de.sam.base.utils.loginReturnUrl
import io.javalin.http.HttpResponseException
import org.tinylog.kotlin.Logger

class ErrorPage(val e: HttpResponseException) : Page(
    name = "Error " + e.status, templateName = "error.kte"
) {
    override fun get() {
        ctx.status(e.status)
        if (e.status == 401) {
            ctx.loginReturnUrl = ctx.path()
            Logger.warn(ctx.loginReturnUrl)
        }
    }
}

