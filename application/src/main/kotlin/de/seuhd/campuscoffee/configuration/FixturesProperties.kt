package de.seuhd.campuscoffee.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The configuration of the fixture loader, bound from `campus-coffee.fixtures.*`. It declares the typed key on which
 * `FixtureStartupLoader` is conditional through `@ConditionalOnProperty`.
 *
 * @property loadOnStartup when true and the database has no users yet, the fixture users are loaded on startup
 *   (enabled in the dev profile only).
 * @property resetOnStartup when true (in the dev profile only), every startup first clears all data and then reseeds
 *   the fixtures. Without it, a persisted dev database keeps the users and coffee counts of the previous run, and the
 *   fixtures are not loaded again.
 */
@ConfigurationProperties("campus-coffee.fixtures")
data class FixturesProperties(
    val loadOnStartup: Boolean = false,
    val resetOnStartup: Boolean = false
)
