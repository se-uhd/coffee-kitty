package de.seuhd.campuscoffee.domain.model

/**
 * Authorization role of a user. Only an [ADMIN] may create users, correct coffee counts, and list the corrections of a
 * user. A user has one role. The coffee app of SE@UHD has the same two roles. Any client can add or undo a coffee for
 * any user, without a role or an acting user.
 */
enum class Role {
    USER,
    ADMIN
}
