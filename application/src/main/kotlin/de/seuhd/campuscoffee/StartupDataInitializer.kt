package de.seuhd.campuscoffee

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component

/**
 * Runs the startup tasks in a fixed order before the embedded web server accepts requests: first the fixture loader,
 * which Spring creates only when `campus-coffee.fixtures.load-on-startup` is true, and then the bootstrap admin loader.
 * The bootstrap admin loader therefore sees the fixture admin `jane_doe` and creates no second admin. The coffee app of
 * SE@UHD has a class with the same name.
 *
 * Spring calls [afterSingletonsInstantiated] after it has created every singleton bean. At that point, Flyway has
 * migrated the schema, and the embedded web server does not accept requests yet. The app therefore serves no request
 * before the startup tasks have finished.
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
