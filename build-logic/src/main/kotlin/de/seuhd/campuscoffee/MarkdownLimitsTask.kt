package de.seuhd.campuscoffee

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Checks each Markdown file against its entry in `config/markdown/limits.yml` (doc/adr/013-limits-for-text.md) and
 * fails for a file that no entry lists. The message of each violation names the source of the limit.
 */
abstract class MarkdownLimitsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val config: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val markdownFiles: ConfigurableFileCollection

    @get:Internal
    abstract val root: DirectoryProperty

    @TaskAction
    fun check() {
        val rootDir = root.get().asFile
        val limits = MarkdownLimits.parse(config.get().asFile.readText())
        val files = markdownFiles.files.associateBy { it.relativeTo(rootDir).invariantSeparatorsPath }
        val violations = limits.check(files)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Markdown files break the limits of config/markdown/limits.yml:\n" + violations.joinToString("\n")
            )
        }
    }
}
