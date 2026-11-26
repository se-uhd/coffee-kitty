package de.seuhd.campuscoffee.models.alloy

import edu.mit.csail.sdg.alloy4.A4Reporter
import edu.mit.csail.sdg.ast.Command
import edu.mit.csail.sdg.ast.ExprBinary
import edu.mit.csail.sdg.ast.ExprUnary
import edu.mit.csail.sdg.ast.Sig
import edu.mit.csail.sdg.parser.CompModule
import edu.mit.csail.sdg.parser.CompUtil
import edu.mit.csail.sdg.translator.A4Options
import edu.mit.csail.sdg.translator.A4Solution
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod
import java.nio.file.Path

/**
 * An Alloy model of classes, read with the API of the Alloy Analyzer. A sig that is neither abstract nor a one sig nor
 * one of [VALUE_SIGS] stands for a class, and an abstract sig with one sigs for an enum. Each field of a class sig
 * stands for a property.
 *
 * @property world the parsed model with its facts, assertions, and commands
 */
class AlloyModel(
    val world: CompModule
) {
    /** The sigs that stand for classes, in the order of the model. */
    val classSigs: List<Sig>
        get() =
            world.allReachableUserDefinedSigs.filter {
                it.isAbstract == null && it.isOne == null && it.name !in VALUE_SIGS
            }

    /** The abstract sigs with one sigs that stand for enums, without the value sigs. */
    val enumSigs: List<Sig>
        get() =
            world.allReachableUserDefinedSigs.filter {
                it.isAbstract != null && it.name !in VALUE_SIGS && children(it.name).isNotEmpty()
            }

    /**
     * Returns the names of the one sigs that extend an abstract sig.
     *
     * @param parent the name of the abstract sig
     */
    fun children(parent: String): List<String> =
        world.allReachableUserDefinedSigs
            .filterIsInstance<Sig.PrimSig>()
            .filter { it.isOne != null && it.parent?.label == "this/$parent" }
            .map { it.name }

    /**
     * Runs a command of the model and returns its solution.
     *
     * @param label the label of the command, such as the name of the assertion of a check
     */
    fun execute(label: String): A4Solution = execute(world.allCommands.single { it.label == label })

    /**
     * Runs a command with the SAT solver of the Alloy Analyzer and returns its solution.
     *
     * @param command a command of the model
     */
    fun execute(command: Command): A4Solution =
        TranslateAlloyToKodkod.execute_command(A4Reporter.NOP, world.allReachableSigs, command, A4Options())

    companion object {
        /** The sigs that stand for values rather than classes. */
        val VALUE_SIGS = setOf("Text", "Number", "Bool", "Instant")

        /**
         * Reads an Alloy model from a file.
         *
         * @param path the path of the `.als` file
         */
        fun read(path: Path): AlloyModel =
            AlloyModel(CompUtil.parseEverything_fromFile(A4Reporter.NOP, null, path.toString()))

        /**
         * Reads an Alloy model from its source.
         *
         * @param text the source of the model
         */
        fun parse(text: String): AlloyModel = AlloyModel(CompUtil.parseEverything_fromString(A4Reporter.NOP, text))
    }
}

/** The name of the sig without the prefix `this/` of the module. */
val Sig.name: String get() = label.removePrefix("this/")

/** The keyword of the multiplicity of the field: one, lone, some, set, seq, or other. */
val Sig.Field.multiplicity: String
    get() {
        val expr = decl().expr
        return when {
            expr is ExprBinary && expr.op == ExprBinary.Op.ISSEQ_ARROW_LONE -> "seq"
            expr is ExprUnary && expr.op == ExprUnary.Op.ONEOF -> "one"
            expr is ExprUnary && expr.op == ExprUnary.Op.LONEOF -> "lone"
            expr is ExprUnary && expr.op == ExprUnary.Op.SOMEOF -> "some"
            expr is ExprUnary && expr.op == ExprUnary.Op.SETOF -> "set"
            else -> "other"
        }
    }

/** The name of the sig that the field refers to. */
val Sig.Field.target: String
    get() {
        val expr = decl().expr
        val target = if (expr is ExprBinary) expr.right else (expr as ExprUnary).sub
        return target.toString().removePrefix("this/")
    }
