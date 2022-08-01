package de.sam.base.utils

import de.sam.base.database.*
import io.javalin.http.Context

// https://github.com/tipsy/javalinstagram/blob/7d03477b89a21addc8cf734b52b292828d48eefe/src/main/kotlin/javalinstagram/Extensions.kt#L7
var Context.currentUserDTO: UserDTO?
    get() = this.cachedSessionAttribute("user")
    set(user) = this.sessionAttribute("user", user)

var Context.isLoggedIn: Boolean
    get() = this.currentUserDTO != null
    set(value) = throw UnsupportedOperationException("Cannot set isLoggedIn")

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