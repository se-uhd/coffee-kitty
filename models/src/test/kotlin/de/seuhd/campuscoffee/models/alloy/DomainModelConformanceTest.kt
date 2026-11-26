package de.seuhd.campuscoffee.models.alloy

import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.implementation.DomainValidator
import de.seuhd.campuscoffee.domain.model.CoffeeBean
import de.seuhd.campuscoffee.domain.model.CoffeePrice
import de.seuhd.campuscoffee.domain.model.CoffeeRating
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.model.DomainModel
import de.seuhd.campuscoffee.domain.model.Payment
import de.seuhd.campuscoffee.domain.model.User
import edu.mit.csail.sdg.ast.Expr
import edu.mit.csail.sdg.ast.ExprList
import edu.mit.csail.sdg.ast.Sig
import edu.mit.csail.sdg.translator.A4Solution
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import java.io.File
import java.net.JarURLConnection
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Checks the Alloy model `src/main/alloy/DomainModel.als` against the domain model of the domain module. For a
 * counterexample, the test names the parts of the assertion that it breaks. A new text property fails the test until
 * [NO_NUL_ASSERTION] names its predicate. For each property of the value sig Text or Number, the test turns each value
 * class into a concrete value at the limits of the rules, such as a note of [CountCorrection.MAX_NOTE_LENGTH]
 * characters and one with a character more, and expects that `valid<Sig><Field>` and [DomainValidator] agree.
 */
class DomainModelConformanceTest {
    private val validator = DomainValidator(Validation.buildDefaultValidatorFactory().validator)

    @TestFactory
    fun `each check of the Alloy model finds no counterexample, and each run finds an instance`(): List<DynamicTest> =
        MODEL.world.allCommands.map { command ->
            dynamicTest("${if (command.check) "check" else "run"} ${command.label}") {
                val solution = MODEL.execute(command.label)
                if (command.check) {
                    assertThat(brokenParts(command.label, solution))
                        .describedAs("the parts of %s that a counterexample breaks", command.label)
                        .isEmpty()
                    assertThat(solution.satisfiable()).describedAs("a counterexample").isFalse()
                } else {
                    assertThat(solution.satisfiable()).describedAs("an instance").isTrue()
                }
            }
        }

    @Test
    fun `the assertion NoTextPropertyAcceptsNUL names each text property`() {
        val named = Regex("valid[A-Z]\\w*").findAll(assertion(NO_NUL_ASSERTION).toString()).map { it.value }
        val textProperties =
            MODEL.classSigs.flatMap { sig -> sig.fields.filter { it.target == "Text" }.map { predicate(sig, it) } }
        assertThat(named.toList()).containsExactlyInAnyOrderElementsOf(textProperties)
    }

    @TestFactory
    fun `each class sig of the Alloy model is a domain model with the same properties`(): List<DynamicTest> =
        MODEL.classSigs.map { sig -> dynamicTest(sig.name) { assertMatches(sig) } }

    @Test
    fun `each class of the domain model is a sig of the Alloy model`() {
        assertThat(domainClasses().filter { it.isData }.map { it.simpleName }).containsExactlyInAnyOrderElementsOf(
            MODEL.classSigs.map { it.name }
        )
    }

    @Test
    fun `each enum of the domain model has the one sigs of its abstract sig`() {
        for (enum in domainClasses().filter { it.java.isEnum }) {
            val constants = enum.java.enumConstants.map { (it as Enum<*>).name }
            assertThat(
                MODEL.children(enum.simpleName!!)
            ).describedAs(enum.simpleName).containsExactlyInAnyOrderElementsOf(constants)
        }
    }

    @TestFactory
    fun `the Alloy model and DomainValidator accept the same values of each property`(): List<DynamicTest> =
        MODEL.classSigs
            .flatMap { sig -> sig.fields.map { sig to it } }
            .filter { (_, field) -> field.target in RULED_VALUE_SIGS }
            .map { (sig, field) ->
                dynamicTest("${sig.name}.${field.label}") {
                    val predicate = predicate(sig, field)
                    for (valueClass in MODEL.children(field.target)) {
                        val value = concreteValue(sig.name, field.label, valueClass) ?: continue
                        val alloy =
                            evaluationSolution.eval(
                                MODEL.world.parseOneExpressionFromString("$predicate[$valueClass]")
                            )
                        val kotlin = accepts(typical(sig.name, mapOf(field.label to value)))
                        assertThat(kotlin)
                            .describedAs("%s.%s as %s (%s)", sig.name, field.label, valueClass, value)
                            .isEqualTo(alloy)
                    }
                    if (field.multiplicity == "lone") {
                        assertThat(
                            accepts(typical(sig.name, mapOf(field.label to null)))
                        ).describedAs("no value").isTrue()
                    }
                }
            }

    private fun assertMatches(sig: Sig) {
        val kClass = Class.forName("$MODEL_PACKAGE.${sig.name}").kotlin
        val properties = kClass.memberProperties.filter { it.name !in METADATA }.associateBy { it.name }
        val matched = mutableSetOf<String>()
        for (field in sig.fields) {
            val property =
                properties[field.label] ?: properties["${field.label}Id"] ?: fail("${sig.name} has no ${field.label}.")
            matched += property.name
            val type = property.returnType
            when (field.multiplicity) {
                "one" -> {
                    assertThat(type.isMarkedNullable).describedAs("%s is nullable", property.name).isFalse()
                }

                "lone" -> {
                    assertThat(type.isMarkedNullable).describedAs("%s is nullable", property.name).isTrue()
                }

                "seq" -> {
                    assertThat(type.classifier).describedAs("%s is a list", property.name).isEqualTo(List::class)
                }

                else -> {
                    fail(
                        "The field ${field.label} has the multiplicity ${field.multiplicity}, which no property has."
                    )
                }
            }
            val classifier =
                if (field.multiplicity ==
                    "seq"
                ) {
                    type.arguments
                        .single()
                        .type
                        ?.classifier
                } else {
                    type.classifier
                }
            assertThat(
                classifier
            ).describedAs("the type of %s", property.name).isEqualTo(kotlinType(field.target, property))
        }
        assertThat(properties.keys).describedAs("the properties of %s", sig.name).isEqualTo(matched)
    }

    private fun kotlinType(
        target: String,
        property: KProperty1<*, *>
    ): KClass<*> =
        when {
            property.name.endsWith("Id") -> UUID::class
            target == "Text" -> String::class
            target == "Number" -> Int::class
            target == "Bool" -> Boolean::class
            target == "Instant" -> LocalDateTime::class
            else -> Class.forName("$MODEL_PACKAGE.$target").kotlin
        }

    private fun accepts(model: Any): Boolean =
        runCatching { validator.requireValid(model) }.exceptionOrNull().let { failure ->
            if (failure != null && failure !is ValidationException) throw failure
            failure == null
        }

    private fun typical(
        sig: String,
        overrides: Map<String, Any?>
    ): Any {
        val constructor = Class.forName("$MODEL_PACKAGE.$sig").kotlin.primaryConstructor!!
        val fields =
            MODEL.classSigs
                .single { it.name == sig }
                .fields
                .associateBy { it.label }
        val arguments =
            constructor.parameters.filter { it.name in overrides || !it.isOptional }.associateWith { parameter ->
                if (parameter.name in overrides) {
                    overrides[parameter.name]
                } else {
                    val field = fields[parameter.name] ?: fail("$sig has no field ${parameter.name}.")
                    when (field.target) {
                        "Text" -> concreteValue(sig, field.label, "ShortText")
                        "Number" -> concreteValue(sig, field.label, "Middle")
                        else -> typical(field.target, emptyMap())
                    }
                }
            }
        return constructor.callBy(arguments)!!
    }

    private fun concreteValue(
        sig: String,
        field: String,
        valueClass: String
    ): Any? {
        val key = "$sig.$field"
        TEXTS[key]?.let { return it.value(valueClass) }
        return NUMBERS[key]?.value(valueClass) ?: if (key in NUMBERS) null else fail("No concrete values for $key.")
    }

    /** The concrete values of a String property: a typical value and the largest length. */
    private data class TextValues(
        val typical: String,
        val maxLength: Int
    ) {
        fun value(valueClass: String): String =
            when (valueClass) {
                "ShortText" -> typical
                "LongestText" -> "x".repeat(maxLength - typical.length) + typical
                "TooLongText" -> "x".repeat(maxLength + 1 - typical.length) + typical
                "BlankText" -> " "
                "TextWithNul" -> typical.take(1) + "\u0000" + typical.drop(1)
                "TextWithControl" -> typical.take(1) + "\u0001" + typical.drop(1)
                "TextWithHyphen" -> typical.take(1) + "-" + typical.drop(1)
                "TextWithoutAt" -> typical.replace("@", "")
                else -> fail("The value class $valueClass of Text has no concrete value.")
            }
    }

    /** The concrete values of an Int property: the smallest and the largest value, or null without a limit. */
    private data class NumberValues(
        val min: Int?,
        val max: Int?
    ) {
        fun value(valueClass: String): Int? =
            when (valueClass) {
                "BelowMin" -> (min ?: 0) - 1
                "Min" -> min ?: 0
                "Middle" -> if (max == null) (min ?: 0) + 1 else ((min ?: 0) + max) / 2
                "Max" -> max ?: Int.MAX_VALUE
                "AboveMax" -> max?.plus(1)
                else -> fail("The value class $valueClass of Number has no concrete value.")
            }
    }

    private fun predicate(
        sig: Sig,
        field: Sig.Field
    ) = "valid${sig.name}${field.label.replaceFirstChar(Char::uppercase)}"

    private fun assertion(label: String): Expr =
        MODEL.world.allAssertions
            .single { it.label.removePrefix("this/") == label }
            .expr

    private fun brokenParts(
        label: String,
        solution: A4Solution
    ): List<String> =
        if (solution.satisfiable()) {
            conjuncts(assertion(label)).filter { solution.eval(it) == false }.map { it.toString() }
        } else {
            emptyList()
        }

    private fun conjuncts(formula: Expr): List<Expr> {
        val body = formula.deNOP()
        return if (body is ExprList && body.op == ExprList.Op.AND) body.args.flatMap(::conjuncts) else listOf(body)
    }

    private fun domainClasses(): List<KClass<*>> {
        val path = MODEL_PACKAGE.replace('.', '/')
        val url = DomainModel::class.java.classLoader.getResource(path) ?: fail("The classpath has no $path.")
        val files =
            when (url.protocol) {
                "file" -> {
                    File(url.toURI()).list().orEmpty().toList()
                }

                "jar" -> {
                    (url.openConnection() as JarURLConnection)
                        .jarFile
                        .entries()
                        .toList()
                        .map { it.name }
                        .filter { it.startsWith("$path/") }
                        .map { it.removePrefix("$path/") }
                }

                else -> {
                    fail("The classes of $path are in a ${url.protocol} location.")
                }
            }
        return files
            .filter { it.endsWith(".class") && '/' !in it && '$' !in it }
            .map { Class.forName("$MODEL_PACKAGE.${it.removeSuffix(".class")}").kotlin }
            .filter { it.java.isEnum || (it.isData && it.isSubclassOf(DomainModel::class)) }
    }

    private companion object {
        const val MODEL_PACKAGE = "de.seuhd.campuscoffee.domain.model"

        /** The properties that every class has for storing it, which the Alloy model leaves out. */
        val METADATA = setOf("id", "createdAt", "updatedAt")

        /** The value sigs whose classes stand for values at the limits of rules. */
        val RULED_VALUE_SIGS = setOf("Text", "Number")

        val TEXTS =
            mapOf(
                "User.loginName" to TextValues("jane_doe", User.MAX_LOGIN_NAME_LENGTH),
                "User.emailAddress" to TextValues("jane@se.de", User.MAX_EMAIL_LENGTH),
                "User.firstName" to TextValues("Jane", User.MAX_NAME_LENGTH),
                "User.lastName" to TextValues("Doe", User.MAX_NAME_LENGTH),
                "CountCorrection.note" to TextValues("Double tap", CountCorrection.MAX_NOTE_LENGTH),
                "CoffeeBean.name" to TextValues("Arabica", CoffeeBean.MAX_NAME_LENGTH),
                "Payment.note" to TextValues("Deposit", Payment.MAX_NOTE_LENGTH)
            )

        val NUMBERS =
            mapOf(
                "CoffeeConsumption.count" to NumberValues(null, null),
                "CountCorrection.countBefore" to NumberValues(null, null),
                "CountCorrection.countAfter" to NumberValues(0, null),
                "CoffeeRating.value" to NumberValues(CoffeeRating.MIN_VALUE, CoffeeRating.MAX_VALUE),
                "CoffeePrice.amountCents" to NumberValues(0, CoffeePrice.MAX_AMOUNT_CENTS),
                "Payment.amountCents" to NumberValues(null, null)
            )

        /** The assertion that no text property accepts the NUL character. */
        const val NO_NUL_ASSERTION = "NoTextPropertyAcceptsNUL"

        val MODEL = AlloyModel.read(Path.of("src/main/alloy/DomainModel.als"))

        /** A solution of the first run command, in which the test evaluates the predicates on the value classes. */
        val evaluationSolution: A4Solution by lazy {
            MODEL.execute(
                MODEL.world.allCommands
                    .first { !it.check }
                    .label
            )
        }
    }
}
