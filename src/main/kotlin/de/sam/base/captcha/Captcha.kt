package de.sam.base.captcha

import com.fasterxml.jackson.databind.ObjectMapper
import de.sam.base.Page
import de.sam.base.config.Configuration
import io.javalin.http.Context
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.formParamAsClass
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Captcha : KoinComponent {
    private val config: Configuration by inject()

    private val captchaServiceUrlMap = mapOf(
        "recaptcha" to Pair("https://www.google.com/recaptcha/api/siteverify", "g-recaptcha-response"),
        "turnstile" to Pair("https://challenges.cloudflare.com/turnstile/v0/siteverify", "cf-turnstile-response")
    )
    val activeService: Configuration.Captcha? = config.captcha

    fun isActiveOnPage(page: Page): Boolean {
        return config.captcha != null && config.captcha!!.locations.any { it.lowercase() == page.javaClass.simpleName.lowercase() }
    }

    fun validate(ctx: Context): List<String> {
        val errors = arrayListOf<String>()

        val (verifyUrl, formKey) = getServicePair(ctx)
            ?: throw InternalServerErrorResponse("Unknown captcha service")

        val captchaSolution =
            ctx.formParamAsClass<String>(formKey)
                .allowNullable()
                .check({ !it.isNullOrBlank() }, "Solving the captcha is required")

        if (captchaSolution.errors().isNotEmpty()) {
            errors.add(captchaSolution.errors().values.first()[0].message)
            return errors
        }

        val client = OkHttpClient()

        val request = Request.Builder()
            .url(verifyUrl)
            .post(
                FormBody.Builder()
                    .add("secret", config.captcha!!.secretKey)
                    .add("response", captchaSolution.get() ?: "")
                    .build()
            )
            .build()


        val response = client.newCall(request).execute()
        val json = response.body.string()

        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(json)
        val success = jsonNode.get("success").asBoolean()
        if (!success) {
            errors.add("The provided captcha solution is invalid")
            // throw BadRequestResponse("captcha solution is invalid")
        }
        return errors

    }

    private fun getServicePair(ctx: Context): Pair<String, String>? {
        return captchaServiceUrlMap.values.firstOrNull { ctx.formParam(it.second) != null }
    }
}