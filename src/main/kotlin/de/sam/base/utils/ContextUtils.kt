package de.sam.base.utils

import de.sam.base.database.ApiKeyDTO
import de.sam.base.database.FileDTO
import de.sam.base.database.ShareDTO
import de.sam.base.database.UserDTO
import io.javalin.http.Context
import io.javalin.http.Header
import java.io.File
import java.io.FileInputStream

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

val Context.requestStartTime: Long
    get() = this.attribute("javalin-request-log-start-time") as Long? ?: 0L

var Context.share: ShareDTO?
    get() = this.attribute<ShareDTO>("requestShareParameter")
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

var Context.tokenTime: Long?
    get() = this.sessionAttribute<Long>("tokenTime")
    set(value) = this.sessionAttribute("tokenTime", value)

var Context.realIp: String
    get() = this.header("X-Forwarded-For")?.split(",")?.get(0) ?: this.ip()
    set(value) = throw UnsupportedOperationException("Cannot set ip")


var Context.apiKeyUsed: ApiKeyDTO?
    get() = this.sessionAttribute("apiKeyUsed")
    set(apiKeyUsed) = this.sessionAttribute("apiKeyUsed", apiKeyUsed)

fun Context.resultFile(file: File, name: String, mimeType: String, dispositionType: String = "attachment", onlyHeader: Boolean = false) {
    // https://www.w3.org/Protocols/HTTP/Issues/content-disposition.txt 1.3, last paragraph
    this.header(Header.CONTENT_DISPOSITION, "$dispositionType; filename=$name")
    this.header(Header.CACHE_CONTROL, "max-age=31536000, immutable")

    if (onlyHeader) {
        this.header(Header.CONTENT_TYPE, mimeType)
    } else {
        CustomSeekableWriter.write(this, FileInputStream(file), mimeType, file.length())
    }
}