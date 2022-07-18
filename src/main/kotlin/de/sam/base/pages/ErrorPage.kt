package de.sam.base.pages

import de.sam.base.Page
import io.javalin.http.Context
import io.javalin.http.HttpResponseException

class ErrorPage(val e: HttpResponseException) : Page(
    name = "Error " + e.status,
    templateName = "error.kte"
) {
    override fun handle(ctx: Context) {
        ctx.status(e.status)
        super.handle(ctx)
    }
}