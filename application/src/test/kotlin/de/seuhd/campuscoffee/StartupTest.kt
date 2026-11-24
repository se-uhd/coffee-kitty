package de.seuhd.campuscoffee

import de.seuhd.campuscoffee.configuration.FixturesProperties
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.internal.DataResetService
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

/**
 * Test of the startup without the dev profile. A data reset at startup would delete the data of a running installation.
 * The Spring context contains the startup tasks and Mockito mocks of the ports that they call.
 */
class StartupTest {
    private val userService = mock<UserService>()
    private val userInternalService = mock<UserInternalService>()
    private val dataResetService = mock<DataResetService>()

    @Test
    fun `the app deletes no data and creates no user on startup without the dev profile`() {
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withBean(UserService::class.java, { userService })
            .withBean(UserInternalService::class.java, { userInternalService })
            .withBean(DataResetService::class.java, { dataResetService })
            .withUserConfiguration(StartupTasks::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                verify(dataResetService, never()).clearAll()
                verify(userInternalService, never()).create(any())
                verify(userInternalService, never()).createInitialAdmin(any())
            }
    }

    /**
     * Registers the startup tasks in the root package and the `*Properties` classes. The scan leaves out the
     * subpackages and [Application], which would start the whole app.
     */
    @ComponentScan(
        basePackageClasses = [Application::class],
        resourcePattern = "*.class",
        excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [Application::class])]
    )
    @ConfigurationPropertiesScan(basePackageClasses = [FixturesProperties::class])
    private class StartupTasks
}
