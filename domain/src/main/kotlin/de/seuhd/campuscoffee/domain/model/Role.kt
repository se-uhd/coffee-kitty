package de.seuhd.campuscoffee.domain.model

/**
 * Authorization role of a user. An [ADMIN] may create users, and a [USER] may not. A user holds exactly one role. The
 * coffee app of SE@UHD has the same two roles. The consumption endpoints do not check the role or the acting user, so
 * any client can add or undo a coffee of any user.
 */
enum class Role {
    USER,
    ADMIN
}
