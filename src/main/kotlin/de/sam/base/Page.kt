package de.sam.base

import de.sam.base.captcha.Captcha
import de.sam.base.config.Configuration
import de.sam.base.database.UserDTO
import de.sam.base.utils.currentUserDTO
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.measureTime

abstract class Page(
    var name: String,
    var title: String = name,
    var pageDescription: String = name,
    var templateName: String
) : Handler, KoinComponent {

    val config: Configuration by inject()
    val captcha: Captcha by inject()

    var pageDiff: Long = 0
    private var templateStartTime: Long? = null

    var currentUserDTO: UserDTO? = null

    lateinit var ctx: Context
    var renderTemplate = true

    private var pagePreTime = 0L
    private var pageRenderStart = 0L

    fun getRenderTime(): String {
        val pageRenderTime = System.nanoTime() - pageRenderStart
        return nanoToMilli(pagePreTime + pageRenderTime)
    }

    private fun nanoToMilli(time: Long): String {
        return "${
            BigDecimal(time)
                .divide(BigDecimal(1000000), 4, RoundingMode.HALF_UP)
        }ms"
    }

    override fun handle(context: Context) {
        this.ctx = context
        currentUserDTO = this.ctx.currentUserDTO
        templateStartTime = System.nanoTime()
        renderTemplate = true

        pagePreTime = measureTime {
            before()
            when (this.ctx.method()) {
                GET -> get()
                POST -> post()
                DELETE -> delete()
                else -> throw UnsupportedOperationException("Method ${this.ctx.method()} not supported")
            }
            after()
        }.inWholeNanoseconds

        if (renderTemplate) {
            pageRenderStart = System.nanoTime()
            render()
        }
    }

    fun render() {
        ctx.render(templateName, mapOf("page" to this))
    }

    open fun before() {}
    open fun get() {}
    open fun post() {}
    open fun delete() {}
    open fun after() {}

}