package de.sam.base.utils

import de.sam.base.database.*
import io.javalin.http.Context

// https://github.com/tipsy/javalinstagram/blob/7d03477b89a21addc8cf734b52b292828d48eefe/src/main/kotlin/javalinstagram/Extensions.kt#L7
var Context.currentUserDTO: UserDTO?
    get() = this.cachedSessionAttribute("user")
    set(user) = this.sessionAttribute("user", user)

var Context.isLoggedIn: Boolean
    get() = this.currentUserDTO != null && !needsToVerifyTOTP
    set(value) = throw UnsupportedOperationException("Cannot set isLoggedIn")

var Context.needsToVerifyTOTP: Boolean
    get() = this.sessionAttribute<Boolean>("needsToVerifyTOTP") ?: false
    set(value) = this.sessionAttribute("needsToVerifyTOTP", value)

var Context.preferencesString: String?
    get() = this.currentUserDTO?.preferences ?: ""
    set(_) = throw UnsupportedOperationException("Cannot set preferencesString")

var Context.isMobileUser: Boolean
    get() = this.attribute<Boolean>("isMobile") == true
    set(value) = this.attribute("isMobile", value)

var Context.fileDTOFromId: FileDTO?
    get() = this.attribute<FileDTO>("requestFileDTOParameter")
    set(value) = this.attribute("requestFileDTOParameter", value)

var Context.fileDAOFromId: FileDAO?
    get() = this.attribute<FileDAO>("requestFileDAOParameter")
    set(value) = this.attribute("requestFileDAOParameter", value)

val Context.requestStartTime: Long
    get() = this.attribute("javalin-request-log-start-time") as Long? ?: 0L

var Context.share: Pair<ShareDAO, ShareDTO>?
    get() = this.attribute<Pair<ShareDAO, ShareDTO>>("requestShareParameter")
    set(value) = this.attribute("requestShareParameter", value)

//TODO: fix actual redirecting, this doesnt seem to work.
fun Context.hxRedirect(route: String) {
    // would love to use redirect & push url but that's not how it works. redirect aborts everything else and forces a browser redirect
//    this.redirect(route)
    this.header("HX-Redirect", route)
}

var Context.isBot: Boolean?
    get() = this.attribute<Boolean>("isBot")
    set(value) = this.attribute("isBot", value)

var Context.totpSecret: String?
    get() = this.sessionAttribute<String>("totpSecret")
    set(value) = this.sessionAttribute("totpSecret", value)

fun Context.validateTOTP(code: String): Boolean {
    return validateTOTP(code, this.totpSecret ?: return false)
}

fun Context.validateTOTP(code: String, secret: String): Boolean {
    return verifier.isValidCode(secret, code)
}

var Context.loginReturnUrl: String?
    get() = this.sessionAttribute<String>("loginReturnUrl")
    set(value) = this.sessionAttribute("loginReturnUrl", value)