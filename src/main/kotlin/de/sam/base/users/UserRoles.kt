package de.sam.base.users

import io.javalin.core.security.RouteRole

enum class UserRoles(var title: String, var powerLevel: Int, var color: String) : RouteRole {
    USER("User", 1, "white"),
    PREMIUM("Premium", 10, "purple"),
    ADMIN("Administrator", 9999, "red");
}