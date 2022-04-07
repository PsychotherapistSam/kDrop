package de.sam.base.users

enum class UserRoles(var title: String, var powerLevel: Int, var color: String) {
    ADMIN("Administrator", 9999, "red"),
    PREMIUM("Premium", 10, "purple"),
    USER("User", 1, "white");
}