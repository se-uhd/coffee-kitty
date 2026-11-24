package de.seuhd.campuscoffee.dev

import de.seuhd.campuscoffee.Application
import org.springframework.boot.fromApplication

/**
 * Starts the dev application: the app of [Application] in the `dev` profile, with the [DevDataConfiguration] in
 * addition. `gradle :application:bootTestRun` runs this function. The function is in the test sources, so the
 * production jar contains neither the user fixtures nor the code that deletes the data.
 *
 * @param args the command-line arguments, such as `--server.port=8082`
 */
fun main(args: Array<String>) {
    fromApplication<Application>()
        .with(DevDataConfiguration::class.java)
        .withAdditionalProfiles("dev")
        .run(*args)
}
