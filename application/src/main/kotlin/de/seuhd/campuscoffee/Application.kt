package de.seuhd.campuscoffee

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * The Spring Boot application class. `@ConfigurationPropertiesScan` registers every `@ConfigurationProperties` class
 * in this package and its subpackages, such as `FixturesProperties`.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class Application

/**
 * Starts the Spring Boot application.
 *
 * @param args the command-line arguments, such as `--spring.profiles.active=dev`
 */
fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
