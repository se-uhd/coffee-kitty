package de.seuhd.campuscoffee

import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component

/**
 * Runs the startup task of the app, the bootstrap admin loader. Spring calls [afterSingletonsInstantiated] after Flyway
 * has migrated the schema and before the embedded web server accepts requests. The coffee app of SE@UHD has a class
 * with the same name.
 *
 * The production code creates no fixture users and deletes no data. The dev application in the test sources of this
 * module does both before it serves requests (see `DevDataSeeder`).
 *
 * @param bootstrapAdminLoader the bootstrap admin loader
 */
@Component
class StartupDataInitializer(
    private val bootstrapAdminLoader: BootstrapAdminLoader
) : SmartInitializingSingleton {
    override fun afterSingletonsInstantiated() {
        bootstrapAdminLoader.createBootstrapAdmin()
    }
}
