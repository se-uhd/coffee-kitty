package de.seuhd.campuscoffee.data

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * The Spring Boot configuration for the integration tests of the data module, without the modules `api` and
 * `application`. The class is in the root package of the data module. The component scan therefore finds the mappers
 * and data adapters, and the JPA auto-configuration finds the repositories and JPA entities.
 */
@SpringBootApplication
class DataTestApplication
