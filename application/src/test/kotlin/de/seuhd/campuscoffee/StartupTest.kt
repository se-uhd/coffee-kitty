package de.seuhd.campuscoffee

import de.seuhd.campuscoffee.configuration.BootstrapAdminProperties
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
 * A test of the startup with the configuration of `application.yaml` and without a bootstrap admin. The Spring context
 * contains the startup task and a Mockito mock of the port that it calls.
 */
class StartupTest {
    private val userInternalService = mock<UserInternalService>()

    @Test
    fun `the app creates no user on startup without a configured bootstrap admin`() {
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withBean(UserInternalService::class.java, { userInternalService })
            .withUserConfiguration(StartupTasks::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                verify(userInternalService, never()).create(any())
                verify(userInternalService, never()).createInitialAdmin(any())
            }
    }

    /**
     * Registers the classes of the startup task in the root package and the `*Properties` classes. The scan leaves out
     * the subpackages and [Application], which would start the whole app.
     */
    @ComponentScan(
        basePackageClasses = [Application::class],
        resourcePattern = "*.class",
        excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [Application::class])]
    )
    @ConfigurationPropertiesScan(basePackageClasses = [BootstrapAdminProperties::class])
    private class StartupTasks
}
