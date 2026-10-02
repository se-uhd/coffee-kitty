package de.seuhd.campuscoffee.data

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Boot configuration used only by the data module's integration tests. Its package is the data layer's root, so the
 * component scan wires the real mappers and data services, and the JPA auto-configuration finds the repositories and
 * entities, as in production, without the api or application layers.
 */
@SpringBootApplication
class DataTestApplication
