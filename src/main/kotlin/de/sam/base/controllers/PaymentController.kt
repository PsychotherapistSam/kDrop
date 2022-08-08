package de.sam.base.controllers

import com.stripe.model.PaymentIntent
import com.stripe.param.PaymentIntentCreateParams
import io.javalin.http.ContentType
import io.javalin.http.Context
import java.util.*

class PaymentController {

    fun createIntent(ctx: Context) {
        val params: PaymentIntentCreateParams = PaymentIntentCreateParams.builder()
            .setAmount(1500)
            .setCurrency("eur")
            .addPaymentMethodType("klarna")
            .addPaymentMethodType("giropay")
//            .setAutomaticPaymentMethods(
//                PaymentIntentCreateParams.AutomaticPaymentMethods
//                    .builder()
//                    .setEnabled(true)
//                    .build()
//            )
            .build()

        // Create a PaymentIntent with the order amount and currency

        // Create a PaymentIntent with the order amount and currency
        val paymentIntent: PaymentIntent = PaymentIntent.create(params)

        ctx.contentType(ContentType.APPLICATION_JSON)
        ctx.json(mapOf("client_secret" to paymentIntent.clientSecret))
    }

    fun finishPayment(ctx: Context) {
        val intentId = ctx.queryParam("payment_intent")
        val intentClientSecret = ctx.queryParam("payment_intent_client_secret")
        val redirectStatus = ctx.queryParam("redirect_status")

        val paymentIntent = PaymentIntent.retrieve(intentId)
        when(paymentIntent.status){
            "succeeded" -> {
                ctx.status(200)
                ctx.html("<h1>Payment succeeded!</h1>")
            }
            "processing" -> {
                ctx.status(200)
                ctx.html("<h1>Your payment is still processing. Check your invoices list.</h1>")
            }
            "requires_payment_method" -> {
                ctx.status(200)
                ctx.html("<h1>Your payment was not successful, please try again.</h1>")
            }
            else -> {
                ctx.status(200)
                ctx.html("<h1>Something went wrong.</h1>")
            }
        }


        println(ctx.method())
    }

}