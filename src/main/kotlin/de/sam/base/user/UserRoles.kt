package de.sam.base.user

import io.javalin.security.RouteRole

enum class UserRoles(
    var title: String,
    var powerLevel: Int,
    var color: String,
    var hidden: Boolean = false,
    var special: Boolean = false
) : RouteRole {
    // self is handled differently in the accessManager, checks if the user requesting is also the user being accessed
    SELF("self", 1, "", true, false),
    FILE_ACCESS_CHECK_ALLOW_HOME("allow home file on file access check", 1, "", true, true),
    USER("User", 1, "white"),
    PREMIUM("Premium", 10, "purple"),
    DEVELOPER("Developer", 1337, "teal"),
    ADMIN("Administrator", 9999, "red");
}