package de.seuhd.campuscoffee.data

import de.seuhd.campuscoffee.domain.implementation.DataResetServiceImpl
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

/**
 * Spring Boot configuration for the integration tests of the data module, without the modules `api` and
 * `application`. Because the class is in the root package of the data module, the component scan finds the mappers
 * and data adapters, and the JPA auto-configuration finds the repositories and JPA entities. The class imports
 * [DataResetServiceImpl] explicitly, because the component scan does not cover the domain module. With this import,
 * the tests run the same data reset as production.
 */
@SpringBootApplication
@Import(DataResetServiceImpl::class)
class DataTestApplication
