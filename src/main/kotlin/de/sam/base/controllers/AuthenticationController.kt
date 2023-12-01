package de.sam.base.controllers

import io.javalin.http.Context

class AuthenticationController {


    fun logoutRequest(ctx: Context) {
        ctx.req().session.invalidate()
    }
}
