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

    var userHasTOTP: Boolean = false
    var creationSuccess: Boolean = false

    override fun handle(ctx: Context) {
        qrCodeDaraUri = ""
        error = ""
        creationSuccess = false
        transaction {
            val user = ctx.currentUserDTO!!.fetchDAO()!!

            userHasTOTP = user.totpSecret != null

            if (ctx.method() == "POST" && !userHasTOTP) {
                val totp = ctx.formParamAsClass<String>("totp")
                    .check({ it.length < 20 }, "Your TOTP is incorrect.")
                    .check({ ctx.validateTOTP(it) }, "Your TOTP is incorrect.")
                    .getFirstError()

                if (totp.second != null) {
                    error = totp.second!!
                } else {
                    user.totpSecret = ctx.totpSecret

                    ctx.currentUserDTO = user.toDTO()
                    ctx.totpSecret = null
                    creationSuccess = true
                    userHasTOTP = true
                }
            } else if (ctx.method() == "DELETE") {
                user.totpSecret = null

                ctx.currentUserDTO = user.toDTO()
                ctx.totpSecret = null
                userHasTOTP = false
            }

            // user doesn't yet have saved a secret
            if (!userHasTOTP) {
                val secretGenerator: SecretGenerator = DefaultSecretGenerator()
                ctx.totpSecret = secretGenerator.generate()

                // generate a QR code
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
