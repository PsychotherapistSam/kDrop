package de.sam.base

import de.sam.base.database.User
import de.sam.base.utils.currentUser
import io.javalin.http.Context
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

abstract class Page(private val ctx: Context) {
    abstract var name: String
    abstract var title: String
    abstract var pageDescription: String
    abstract var templateName: String
    private val createdNanoTime = System.nanoTime()

    var user: User? = ctx.currentUser

    //TODO: nonces
    //  val nonce = ctx.attribute<String>("nonce")

    fun getRenderTime(): String {
        return "${BigDecimal(System.nanoTime() - createdNanoTime).divide(BigDecimal(1000000), 4, RoundingMode.HALF_UP)}ms"
    }

    open fun render() {
        ctx.render(templateName, Collections.singletonMap("page", this))
    }
}