package de.sam.base.pages.user.settings

import de.sam.base.Page
import de.sam.base.user.repository.UserRepository
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
import org.koin.core.component.inject


class UserTOTPSettingsPage : Page(
    name = "Settings",
    templateName = "user/settings/totp.kte"
) {
    companion object {
        const val ROUTE: String = "/user/settings/totp"
    }

    val userRepository: UserRepository by inject()

    var qrCodeDaraUri: String = ""
    var error: String = ""

    var userHasTOTP: Boolean = false
    var creationSuccess: Boolean = false

    override fun before() {
        userHasTOTP = ctx.currentUserDTO!!.totpSecret != null
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
                ctx.currentUserDTO = userRepository.updateUser(
                    ctx.currentUserDTO!!.copy(
                        totpSecret = ctx.totpSecret
                    )
                )
                ctx.totpSecret = null
                creationSuccess = true
                userHasTOTP = true
            }
        }
    }

    override fun delete() {
        if (userHasTOTP) {
            ctx.currentUserDTO = userRepository.updateUser(
                ctx.currentUserDTO!!.copy(
                    totpSecret = null
                )
            )
            ctx.totpSecret = null
            userHasTOTP = false
        }
    }

    override fun after() {
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
