package de.seuhd.campuscoffee.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The configuration of the fixture loader, bound from the keys under `campus-coffee.fixtures`. The property
 * [loadOnStartup] declares the type and the default value of the key `campus-coffee.fixtures.load-on-startup`, which
 * lets IntelliJ resolve the key in `application.yaml`. No code reads [loadOnStartup], because the
 * `@ConditionalOnProperty` on `FixtureStartupLoader` names the key directly.
 *
 * @property loadOnStartup when true, Spring creates the fixture loader, which creates the fixture users on startup in a
 *   database without users. Only the dev profile sets `load-on-startup` to true.
 * @property resetOnStartup when true, the fixture loader runs a data reset and then creates the fixture users on every
 *   startup. The property has an effect only together with [loadOnStartup]. Only the dev profile sets
 *   `reset-on-startup` to true. When `reset-on-startup` is false, a persisted database keeps the users and coffee
 *   counts of the previous run, and the fixture loader does not create the fixture users again.
 */
@ConfigurationProperties("campus-coffee.fixtures")
data class FixturesProperties(
    val loadOnStartup: Boolean = false,
    val resetOnStartup: Boolean = false
)
