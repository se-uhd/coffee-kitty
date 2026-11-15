package de.seuhd.campuscoffee

import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.beans.factory.support.DefaultListableBeanFactory

/**
 * Unit tests for [StartupDataInitializer]. The bootstrap admin loader runs after the fixture loader, and it also runs
 * when Spring did not create a fixture loader.
 */
class StartupDataInitializerTest {
    private val fixtureStartupLoader = mock<FixtureStartupLoader>()
    private val bootstrapAdminLoader = mock<BootstrapAdminLoader>()

    @Test
    fun `afterSingletonsInstantiated runs the fixture loader and then the bootstrap admin loader`() {
        val beans = DefaultListableBeanFactory().apply { registerSingleton("fixtures", fixtureStartupLoader) }

        StartupDataInitializer(beans.getBeanProvider(FixtureStartupLoader::class.java), bootstrapAdminLoader)
            .afterSingletonsInstantiated()

        val ordered = inOrder(fixtureStartupLoader, bootstrapAdminLoader)
        ordered.verify(fixtureStartupLoader).loadOnStartup()
        ordered.verify(bootstrapAdminLoader).createBootstrapAdmin()
    }

    @Test
    fun `afterSingletonsInstantiated without a fixture loader runs only the bootstrap admin loader`() {
        StartupDataInitializer(
            DefaultListableBeanFactory().getBeanProvider(FixtureStartupLoader::class.java),
            bootstrapAdminLoader
        ).afterSingletonsInstantiated()

        verify(bootstrapAdminLoader).createBootstrapAdmin()
    }
}
