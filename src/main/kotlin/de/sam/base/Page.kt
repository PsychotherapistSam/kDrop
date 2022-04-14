package de.sam.base

import de.sam.base.database.User
import de.sam.base.utils.currentUser
import de.sam.base.utils.isLoggedIn
import io.javalin.http.Context
import io.javalin.http.Handler
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

abstract class Page: Handler {
    abstract var name: String
    abstract var title: String
    abstract var pageDescription: String
    abstract var templateName: String

    var pageDiff: Long = 0
    private var templateStartTime: Long? = null // = System.nanoTime()
    var currentUser: User? = null // = ctx.currentUser

    //TODO: nonces
    //  val nonce = ctx.attribute<String>("nonce")

    fun getRenderTime(): String {
        val templateDiff = System.nanoTime() - (templateStartTime ?: System.nanoTime())
        return nanoToMilli(pageDiff + templateDiff)
    }

    private fun nanoToMilli(time: Long): String {
        return "${
            BigDecimal(time)
                .divide(BigDecimal(1000000), 4, RoundingMode.HALF_UP)
        }ms"
    }

    /*
    open fun render() {
        //  ctx.render(templateName, Collections.singletonMap("page", this))
    }*/

    override fun handle(ctx: Context) {
        currentUser = ctx.currentUser
        templateStartTime = System.nanoTime()
        ctx.render(templateName, Collections.singletonMap("page", this))
    }
}