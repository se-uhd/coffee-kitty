package de.seuhd.coffeekitty.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The configuration of the fixture loader, bound from `coffee-kitty.fixtures.*`. Its [loadOnStartup] property gives
 * the key `coffee-kitty.fixtures.load-on-startup` a type and a default, so that the IDE resolves the key in
 * `application.yaml`. No code reads the property, because the `@ConditionalOnProperty` on `FixtureStartupLoader` reads
 * the key itself.
 *
 * @property loadOnStartup when true, registers the fixture loader, which creates the fixture users on startup in a
 *   database without users (on in the dev profile only).
 * @property resetOnStartup when true together with [loadOnStartup], the fixture loader first clears all data and then
 *   creates the fixture users on every startup (on in the dev profile only). Without it, a persisted dev database keeps
 *   the users and coffee counts of the previous run, and the fixture users are not loaded again.
 */
@ConfigurationProperties("coffee-kitty.fixtures")
data class FixturesProperties(
    val loadOnStartup: Boolean = false,
    val resetOnStartup: Boolean = false
)
