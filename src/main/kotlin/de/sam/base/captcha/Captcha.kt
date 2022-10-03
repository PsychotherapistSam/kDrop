package de.sam.base.captcha

import com.fasterxml.jackson.databind.ObjectMapper
import de.sam.base.config.Configuration
import io.javalin.http.Context
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.formParamAsClass
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class Captcha {
    companion object {
        private val captchaServiceUrlMap = mapOf(
            "recaptcha" to "https://www.google.com/recaptcha/api/siteverify"
        )

        fun validate(ctx: Context): List<String> {
            val errors = arrayListOf<String>()
            val captchaSolution =
                ctx.formParamAsClass<String>("g-recaptcha-response")
                    .allowNullable()
                    .check({ it != null && it.isNotBlank() }, "Solving the captcha is required")

            if (captchaSolution.errors().isNotEmpty()) {
                errors.add(captchaSolution.errors().values.first()[0].message)
                return errors
            }

            val client = OkHttpClient()

            val request = Request.Builder()
                .url(
                    captchaServiceUrlMap[Configuration.config.captcha.service.lowercase()]
                        ?: throw InternalServerErrorResponse("Unknown captcha service")
                )
                .post(
                    FormBody.Builder()
                        .add("secret", Configuration.config.captcha.secretKey)
                        .add("response", captchaSolution.get() ?: "")
                        //                            .add("sitekey", "10000000-ffff-ffff-ffff-000000000001")
                        .build()
                )
                .build()


            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: throw InternalServerErrorResponse("no response body")

            val mapper = ObjectMapper()
            val jsonNode = mapper.readTree(json)
            val success = jsonNode.get("success").asBoolean()
            if (!success) {
                errors.add("The provided captcha solution is invalid")
                // throw BadRequestResponse("captcha solution is invalid")
            }
            return errors
        }

    }
}