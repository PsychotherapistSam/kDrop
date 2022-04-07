package de.sam.base

import de.sam.base.users.User
import de.sam.base.users.UserRoles
import io.javalin.http.Context
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

abstract class Page(private val ctx: Context) {
    abstract var name: String
    abstract var pageTitle: String
    abstract var pageDescription: String
    abstract var templateName: String
    private val createdNanoTime = System.nanoTime()
    var user: User = User(
        UUID.randomUUID(),
        "Sam",
        "123456",
        null,
        listOf(UserRoles.ADMIN, UserRoles.PREMIUM),
        hashMapOf("nsfw" to false, "language" to "en")
    )

    //  val nonce = ctx.attribute<String>("nonce")

    val renderTime =
        //"${BigDecimal(System.nanoTime() - createdNanoTime / 1000000.0).setScale(2, RoundingMode.HALF_EVEN)}ms"
        "${BigDecimal(System.nanoTime() - createdNanoTime).divide(BigDecimal(1000000), 4, RoundingMode.HALF_UP)}ms"

    open fun render() {
        ctx.render(templateName, Collections.singletonMap("page", this))
    }
}