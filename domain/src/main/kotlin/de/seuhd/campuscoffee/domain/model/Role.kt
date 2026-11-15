package de.seuhd.campuscoffee.domain.model

/**
 * Authorization role of a user. Only an [ADMIN] may create users. A user has one role. The coffee app of SE@UHD has the
 * same two roles. The consumption endpoints check neither the role nor the acting user. Any client can add or undo a
 * coffee of any user.
 */
enum class Role {
    USER,
    ADMIN
}
