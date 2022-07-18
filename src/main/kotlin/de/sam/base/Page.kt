package de.sam.base

import de.sam.base.database.UserDTO
import de.sam.base.utils.currentUserDTO
import io.javalin.http.Context
import io.javalin.http.Handler
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

abstract class Page(
    var name: String,
    var title: String = name,
    var pageDescription: String = name,
    var templateName: String
) : Handler {

    var pageDiff: Long = 0
    private var templateStartTime: Long? = null // = System.nanoTime()
    var currentUserDTO: UserDTO? = null // = ctx.currentUser

    var context: Context? = null

    //TODO: nonces
    //  val nonce = ctx.attribute<String>("nonce")

    fun getRenderTime(): String {
//        val templateDiff = System.nanoTime() - (templateStartTime ?: System.nanoTime())
//        println("pageDiff: ${nanoToMilli(pageDiff)}; templateDiff: ${nanoToMilli(templateDiff)}; total: ${nanoToMilli(pageDiff + templateDiff)}")
//        return nanoToMilli(pageDiff + templateDiff)
        val startTime = context?.attribute("javalin-request-log-start-time") as Long? ?: System.nanoTime()
        return nanoToMilli(System.nanoTime() - startTime)
    }

    fun getPageDiff(): String {
        return nanoToMilli(pageDiff)
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
        context = ctx
        currentUserDTO = ctx.currentUserDTO
        templateStartTime = System.nanoTime()
        ctx.render(templateName, Collections.singletonMap("page", this))
    }
}