package de.sam.base.pages

import de.sam.base.Page
import io.javalin.http.Context
import io.javalin.http.HttpResponseException

class ErrorPage(val e: HttpResponseException) : Page() {

    override var name: String = "Error ${e.status}"
    override var title: String
        get() = name
        set(value) {}
    override var pageDescription: String
        get() = name
        set(value) {}
    override var templateName: String = "error.kte"

    override fun handle(ctx: Context) {
        ctx.status(e.status)
        super.handle(ctx)
    }
}