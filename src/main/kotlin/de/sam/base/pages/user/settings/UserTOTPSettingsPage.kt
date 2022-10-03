package de.sam.base.pages.user.settings

import de.sam.base.Page
import de.sam.base.config.Configuration.Companion.config
import de.sam.base.database.UserDAO
import de.sam.base.database.fetchDAO
import de.sam.base.database.toDTO
import de.sam.base.utils.currentUserDTO
import de.sam.base.utils.getFirstError
import de.sam.base.utils.totpSecret
import de.sam.base.utils.validateTOTP
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.secret.SecretGenerator
import dev.samstevens.totp.util.Utils.getDataUriForImage
import io.javalin.http.formParamAsClass
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

    lateinit var user: UserDAO
    override fun before() {
        qrCodeDaraUri = ""
        error = ""
        creationSuccess = false
        transaction {
            user = ctx.currentUserDTO!!.fetchDAO()!!
        }
        userHasTOTP = user.totpSecret != null
    }

    override fun get() {

    }

    override fun post() {
        if (!userHasTOTP) {
            val totp = ctx.formParamAsClass<String>("totp")
                .check({ it.length < 20 }, "Your TOTP is incorrect.")
                .check({ ctx.validateTOTP(it) }, "Your TOTP is incorrect.")
                .getFirstError()

            if (totp.second != null) {
                error = totp.second!!
            } else {
                transaction {
                    user.totpSecret = ctx.totpSecret
                }
                ctx.currentUserDTO = user.toDTO()
                ctx.totpSecret = null
                creationSuccess = true
                userHasTOTP = true
            }
        }
    }

    override fun delete() {
        if (userHasTOTP) {
            transaction {
                user.totpSecret = null
            }

            ctx.currentUserDTO = user.toDTO()
            ctx.totpSecret = null
            userHasTOTP = false
        }
    }

    override fun after() {
        transaction {
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
    }
}
