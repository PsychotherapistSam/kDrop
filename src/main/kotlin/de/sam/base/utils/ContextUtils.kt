package de.sam.base.utils

import de.sam.base.database.UserDTO
import io.javalin.http.Context

// https://github.com/tipsy/javalinstagram/blob/7d03477b89a21addc8cf734b52b292828d48eefe/src/main/kotlin/javalinstagram/Extensions.kt#L7
var Context.currentUserDTO: UserDTO?
    get() = this.sessionAttribute("user")
    set(user) = this.sessionAttribute("user", user)

var Context.isLoggedIn: Boolean
    get() = this.currentUserDTO != null
    set(value) = throw UnsupportedOperationException("Cannot set isLoggedIn")