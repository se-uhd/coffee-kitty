package de.seuhd.campuscoffee.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The configuration of the fixture loader, which Spring binds from the keys under `campus-coffee.fixtures`.
 * [loadOnStartup] declares the type and the default value of the key `campus-coffee.fixtures.load-on-startup`, and
 * IntelliJ can then resolve the key in `application.yaml`. No code reads [loadOnStartup], because the
 * `@ConditionalOnProperty` on `FixtureStartupLoader` names the key directly.
 *
 * @property loadOnStartup when true, Spring creates the fixture loader, which creates the fixture users on startup in a
 *   database without users. Only the dev profile sets `load-on-startup` to true.
 * @property resetOnStartup when true, the fixture loader runs a data reset and then creates the fixture users on every
 *   startup. The property applies only when [loadOnStartup] is true. Only the dev profile sets `reset-on-startup` to
 *   true. When it is false, the users and coffee counts of the previous run stay in a persisted database. The fixture
 *   loader then does not create the fixture users again.
 */
@ConfigurationProperties("campus-coffee.fixtures")
data class FixturesProperties(
    val loadOnStartup: Boolean = false,
    val resetOnStartup: Boolean = false
)
