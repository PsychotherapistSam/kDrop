package de.sam.base.pages.user.settings

import de.sam.base.Page
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.database.fetchDAO
import de.sam.base.database.toDTO
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.hxRedirect
import de.sam.base.utils.totpSecret
import de.sam.base.utils.validateTOTP
import dev.samstevens.totp.code.*
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.secret.SecretGenerator
import dev.samstevens.totp.util.Utils.getDataUriForImage
import io.javalin.core.validation.Validator
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction


class UserTOTPSettingsPage : Page(
    name = "Settings",
    templateName = "user/settings/totp.kte"
) {
    companion object {
        lateinit var ROUTE: String
    }

    var qrCodeDaraUri: String = ""
    var error: String = ""


    override fun handle(ctx: Context) {
        qrCodeDaraUri = ""
        error = ""

        if (ctx.currentUserDTO!!.totpSecret.isNullOrBlank()) {
            if (ctx.method() == "POST") {
                val totp = ctx.formParamAsClass<String>("totp")
                    .check({ it.length < 20 }, "Your TOTP is incorrect.")
                    .check({ ctx.validateTOTP(it) }, "Your TOTP is incorrect.")
                    .getFirstError()

                if (totp.second != null) {
                    error = totp.second!!
                } else {
                    transaction {
                        val user = ctx.currentUserDTO!!.fetchDAO()!!
                        user.totpSecret = ctx.totpSecret
                        ctx.currentUserDTO = user.toDTO()
                        ctx.totpSecret = ""
                        ctx.hxRedirect("?success=true")
                    }

                }
            } else if (ctx.totpSecret == null) {
                val secretGenerator: SecretGenerator = DefaultSecretGenerator()
                ctx.totpSecret = secretGenerator.generate()
            }
            val data = QrData.Builder()
                .label(ctx.currentUserDTO!!.name)
                .secret(ctx.totpSecret)
                .issuer(config.name)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build()

            val generator = ZxingPngQrGenerator();
            val imageData = generator.generate(data)

            val mimeType: String = generator.imageMimeType

            qrCodeDaraUri = getDataUriForImage(imageData, mimeType)
        } else {
            if (ctx.method() == "DELETE") {
                transaction {
                    val user = ctx.currentUserDTO!!.fetchDAO()!!
                    user.totpSecret = null
                    ctx.currentUserDTO = user.toDTO()
                }
            }
        }
        super.handle(ctx)
    }

    private fun <T> Validator<T>.getFirstError(): Pair<Validator<T>, String?> {
        if (this.errors().isNotEmpty()) {
            val errors = this.errors()
            var errorMessage = errors.values.first()[0].message

            errorMessage = when (errorMessage) {
                "TYPE_CONVERSION_FAILED" -> "The value you entered is not valid."
                else -> errorMessage
            }

            return Pair(this, errorMessage)
        }
        return Pair(this, null)
    }
}
