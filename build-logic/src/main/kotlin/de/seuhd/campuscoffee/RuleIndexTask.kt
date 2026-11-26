package de.seuhd.campuscoffee

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.yaml.snakeyaml.Yaml

/**
 * Writes the lists of [RuleIndex] into `CLAUDE.md` and `AGENTS.md`, or with [verify] fails when a list differs from the
 * front matter of the rule files.
 */
abstract class RuleIndexTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ruleFiles: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val config: RegularFileProperty

    @get:Internal
    abstract val root: DirectoryProperty

    @get:Input
    abstract val verify: Property<Boolean>

    @TaskAction
    fun run() {
        val lineLength = Yaml().load<Map<String, Any?>>(config.get().asFile.readText())["lineLength"] as Int
        val index = RuleIndex(ruleFiles.files.associate { it.name to it.readLines() }, lineLength)
        val stale =
            listOf("CLAUDE.md", "AGENTS.md").filter { target ->
                val file = root.get().file(target).asFile
                val text = file.readText()
                val generated = index.render(target, text)
                if (generated != text && !verify.get()) file.writeText(generated)
                generated != text
            }
        if (verify.get() && stale.isNotEmpty()) {
            throw GradleException(
                "The lists of the rule files in ${stale.joinToString()} differ from the front matter of " +
                    ".claude/rules/*.md. Run gradle ruleIndex instead of editing the lists."
            )
        }
    }
}
