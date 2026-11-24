package de.seuhd.campuscoffee

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component

/**
 * Runs the startup tasks in a fixed order, first the fixture loader and then the bootstrap admin loader. Spring creates
 * the fixture loader only when `campus-coffee.fixtures.load-on-startup` is true. When the fixture loader has created
 * the fixture users, `jane_doe` is an admin, and the bootstrap admin loader creates no second admin. The coffee app of
 * SE@UHD has a class with the same name.
 *
 * Spring calls [afterSingletonsInstantiated] after Flyway has migrated the schema and before the embedded web server
 * accepts requests.
 *
 * @param fixtureStartupLoader the fixture loader, if Spring created it
 * @param bootstrapAdminLoader the bootstrap admin loader
 */
@Component
class StartupDataInitializer(
    private val fixtureStartupLoader: ObjectProvider<FixtureStartupLoader>,
    private val bootstrapAdminLoader: BootstrapAdminLoader
) : SmartInitializingSingleton {
    override fun afterSingletonsInstantiated() {
        fixtureStartupLoader.ifAvailable { it.loadOnStartup() }
        bootstrapAdminLoader.createBootstrapAdmin()
    }
}
