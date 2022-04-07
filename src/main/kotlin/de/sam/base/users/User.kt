package de.sam.base.users

import java.util.*

class User(
    val id: UUID,
    val name: String,
    val password: String,
    val avatar: String?,
    val roles: List<UserRoles>,
    val preferences: HashMap<String, Any>
) {

    fun hasRole(role: UserRoles): Boolean {
        return roles.contains(role)
    }

    val sortedRoles = roles.sortedBy { it.powerLevel }.reversed()

}