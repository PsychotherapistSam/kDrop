package de.sam.base.pages

import de.sam.base.Page
import io.javalin.http.Context
import io.javalin.http.HttpResponseException

class ErrorPage(val e: HttpResponseException) : Page() {

    override var name: String = "Error ${e.status}"
    override var title: String = name
    override var pageDescription: String = name
    override var templateName: String = "error.kte"
}