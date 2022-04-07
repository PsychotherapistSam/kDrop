package de.sam.base.pages

import de.sam.base.Page
import io.javalin.http.Context

class IndexPage(ctx: Context) : Page(ctx) {
    companion object {
        const val ROUTE: String = "/"
    }

    override var name: String = "Index"
    override var pageTitle: String = "Index"
    override var pageDescription: String = "Index of the homepage"
    override var templateName: String = "index.kte"

    override fun render() {
        println("rendering $name page, overriding default render()")
        super.render()
    }
}