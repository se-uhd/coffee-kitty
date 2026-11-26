package de.seuhd.campuscoffee.models.alloy

import de.seuhd.campuscoffee.models.statechart.isCurrent
import de.seuhd.campuscoffee.models.statechart.renderSvg
import edu.mit.csail.sdg.ast.Sig
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess

/** The scope of the Alloy runs that derive the multiplicity at the source end of an arrow. */
const val REFERRER_SCOPE = 4

private const val ARGUMENTS = 3

/**
 * Writes or checks the class diagram of each Alloy model. The mode `write` writes the diagram as PlantUML source and as
 * SVG. The mode `check` fails when a diagram differs from its model.
 *
 * @param args the mode (`write` or `check`), the directory of the Alloy models, and the directory of the diagrams
 */
fun main(args: Array<String>) {
    require(args.size == ARGUMENTS && args[0] in setOf("write", "check")) {
        "Usage: class-diagram <write|check> <model directory> <diagram directory>"
    }
    val (models, diagrams) = args.drop(1).map { Path.of(it) }
    val files =
        models.listDirectoryEntries("*.als").sorted().flatMap { model ->
            val source = toClassDiagram(model.nameWithoutExtension, model.readText())
            val name = model.nameWithoutExtension
            listOf(diagrams.resolve("$name.puml") to source, diagrams.resolve("$name.svg") to renderSvg(source))
        }
    if (args[0] == "write") {
        diagrams.createDirectories()
        files.forEach { (file, content) -> file.writeText(content) }
        return
    }
    val stale = files.filterNot { (file, content) -> isCurrent(file, content) }.map { it.first }
    if (stale.isNotEmpty()) {
        System.err.println(
            stale.joinToString("\n") { "$it differs from its model. Run `gradle :models:classDiagrams`." }
        )
        exitProcess(1)
    }
}

/**
 * Returns the PlantUML source of the class diagram of the Alloy model [name], as the section Class diagram of
 * `models/README.md` describes it. The title is the start of the header comment of the model and the file name.
 *
 * @param name the name of the model, such as `DomainModel`
 * @param text the source of the model
 */
fun toClassDiagram(
    name: String,
    text: String
): String {
    val classes = AlloyModel.parse(text).classSigs
    val classNames = classes.map { it.name }.toSet()
    val references =
        classes.flatMap { sig ->
            sig.fields.filter { it.target in classNames }.map { Reference(sig.name, it.label, it.target) }
        }
    val model = AlloyModel.parse(text + references.joinToString("") { it.commands() })
    return buildString {
        appendLine("@startuml")
        appendLine("!pragma layout smetana")
        appendLine("hide methods")
        appendLine("skinparam classAttributeIconSize 0")
        appendLine("title ${title(name, text)}")
        model.classSigs.forEach { appendClass(it, classNames) }
        model.enumSigs.forEach { sig ->
            appendLine("enum ${sig.name} {")
            model.children(sig.name).forEach { appendLine("  $it") }
            appendLine("}")
        }
        references.forEach { appendLine(it.arrow(model)) }
        appendRules(model)
        appendLine(
            "caption Multiplicities at the source ends of the arrows were derived with the Alloy Analyzer " +
                "(scope $REFERRER_SCOPE)."
        )
        appendLine("@enduml")
    }
}

private fun title(
    name: String,
    text: String
): String {
    val comment = Regex("""/\*(.*?)\*/""", RegexOption.DOT_MATCHES_ALL).find(text)?.groupValues?.get(1)
    val subject =
        comment
            ?.lines()
            ?.joinToString(" ") { it.trim().removePrefix("*").trim() }
            ?.trim()
            ?.substringBefore(" (")
            ?.substringBefore(".")
            ?.removePrefix("The ")
            ?.replaceFirstChar { it.uppercase() }
    return if (subject.isNullOrBlank()) "Classes of $name.als" else "$subject ($name.als)"
}

private fun StringBuilder.appendClass(
    sig: Sig,
    classNames: Set<String>
) {
    appendLine("class ${sig.name} {")
    sig.fields.filter { it.target !in classNames }.forEach { field ->
        val suffix =
            when (field.multiplicity) {
                "one" -> ""
                "seq" -> " [*] {seq}"
                else -> " [${uml(field.multiplicity)}]"
            }
        appendLine("  ${field.label} : ${field.target}$suffix")
    }
    appendLine("}")
}

private fun StringBuilder.appendRules(model: AlloyModel) {
    val facts = model.world.allFacts.map { it.a.removePrefix("this/") }
    val checks = model.world.allCommands.filter { it.check }
    if (facts.isEmpty() && checks.isEmpty()) return
    appendLine("legend right")
    appendLine("|= Rule |= Kind |= Result |")
    facts.forEach { appendLine("| $it | fact | assumed |") }
    checks.forEach {
        val result = if (model.execute(it).satisfiable()) "breaks" else "holds"
        appendLine("| ${it.label} | assertion | $result |")
    }
    appendLine("endlegend")
}

private fun uml(keyword: String): String =
    when (keyword) {
        "one" -> "1"
        "lone" -> "0..1"
        "some" -> "1..*"
        else -> "*"
    }

/**
 * The field [field] of the class sig [source], which refers to the class sig [target].
 *
 * @property source the name of the sig with the field
 * @property field the name of the field
 * @property target the name of the sig that the field refers to
 */
private class Reference(
    val source: String,
    val field: String,
    val target: String
) {
    private val label = "Referrers_${source}_$field"

    /**
     * Returns two run commands for the model. The first finds an object of [target] that no object of [source] refers
     * to, and the second an object that two objects of [source] refer to.
     */
    fun commands(): String {
        val none = "some t: $target | no s: $source | t in s.$field"
        val several = "some t: $target | some disj s1, s2: $source | t in s1.$field and t in s2.$field"
        return "\nrun ${label}_none { $none } for $REFERRER_SCOPE\n" +
            "run ${label}_several { $several } for $REFERRER_SCOPE\n"
    }

    /**
     * Returns the arrow of the reference in PlantUML.
     *
     * @param model the model with the commands of [commands], which give the multiplicity at the tail
     */
    fun arrow(model: AlloyModel): String {
        val field =
            model.classSigs
                .single { it.name == source }
                .fields
                .single { it.label == this.field }
        val none = model.execute("${label}_none").satisfiable()
        val several = model.execute("${label}_several").satisfiable()
        val referrers =
            when {
                none && several -> "*"
                none -> "0..1"
                several -> "1..*"
                else -> "1"
            }
        return "$source \"$referrers\" --> \"${uml(field.multiplicity)}\" $target : ${this.field}"
    }
}
