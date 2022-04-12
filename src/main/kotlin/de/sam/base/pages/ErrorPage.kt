package de.sam.base.pages

import de.sam.base.Page
import io.javalin.http.Context
import io.javalin.http.HttpResponseException

class ErrorPage(val e: HttpResponseException) : Page() {

    override var name: String = "Error ${e.status}"
    override var title: String = "Error ${e.status}"
    override var pageDescription: String = "Error ${e.status}"
    override var templateName: String = "error.kte"


    override fun handle(ctx: Context) {
        super.handle(ctx)
    }
}