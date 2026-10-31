package de.seuhd.campuscoffee.data

import de.seuhd.campuscoffee.domain.implementation.DataResetServiceImpl
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

/**
 * Spring Boot configuration for the integration tests of the data module. It is in the root package of the data
 * module, so the component scan wires the real mappers and data adapters, and the JPA auto-configuration finds the
 * repositories and entities, as in production, without the api or application modules. It also imports
 * [DataResetServiceImpl] from the domain, so that the tests clear the tables in the order that production uses.
 */
@SpringBootApplication
@Import(DataResetServiceImpl::class)
class DataTestApplication
