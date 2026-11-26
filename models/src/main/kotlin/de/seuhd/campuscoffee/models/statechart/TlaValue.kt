package de.seuhd.campuscoffee.models.statechart

/**
 * The value of a TLA+ variable, as TLC prints it in a state graph or an error trace. A view reads a value with the
 * properties below, such as [int] and [items].
 */
sealed interface TlaValue {
    /** An integer, such as `2`. */
    data class IntValue(
        val value: Long
    ) : TlaValue

    /** A string, such as `"recent"`. */
    data class StringValue(
        val value: String
    ) : TlaValue

    /** `TRUE` or `FALSE`. */
    data class BoolValue(
        val value: Boolean
    ) : TlaValue

    /** A sequence or a tuple, such as `<<1, 2>>`. */
    data class SeqValue(
        val items: List<TlaValue>
    ) : TlaValue

    /** A set, such as `{1, 2}`. */
    data class SetValue(
        val items: List<TlaValue>
    ) : TlaValue

    /** A record, such as `[paid |-> TRUE, amount |-> 3]`. */
    data class RecordValue(
        val fields: Map<String, TlaValue>
    ) : TlaValue

    /** A function with a domain other than `1..n`, such as `(alice :> 2 @@ bob :> 0)`. */
    data class FunctionValue(
        val pairs: List<Pair<TlaValue, TlaValue>>
    ) : TlaValue

    /** A model value, or text that the parser cannot read. */
    data class NameValue(
        val text: String
    ) : TlaValue
}

val TlaValue.int: Long
    get() = (this as? TlaValue.IntValue)?.value ?: error("TLC printed $this where the view expects an integer.")

val TlaValue.string: String
    get() = (this as? TlaValue.StringValue)?.value ?: error("TLC printed $this where the view expects a string.")

val TlaValue.items: List<TlaValue>
    get() =
        when (this) {
            is TlaValue.SeqValue -> items
            is TlaValue.SetValue -> items
            else -> error("TLC printed $this where the view expects a sequence or a set.")
        }

/**
 * Parses one TLA+ value as TLC prints it. Text that the parser cannot read becomes a [TlaValue.NameValue].
 *
 * @param text the value, such as `<<"recent", "old">>`
 * @return the parsed value
 */
fun parseTlaValue(text: String): TlaValue =
    runCatching { TlaValueParser(text).parseComplete() }.getOrElse { TlaValue.NameValue(text.trim()) }

/** Reads one TLA+ value from [text]. */
private class TlaValueParser(
    private val text: String
) {
    private var pos = 0

    /** Reads a value and fails when text other than whitespace follows it. */
    fun parseComplete(): TlaValue {
        val value = value()
        skipSpace()
        require(pos == text.length) { "Unexpected text at position $pos of $text" }
        return value
    }

    /** Reads the value that starts at the current position. */
    private fun value(): TlaValue {
        skipSpace()
        return when {
            text.startsWith("<<", pos) -> TlaValue.SeqValue(list("<<", ">>"))
            text.startsWith("{", pos) -> TlaValue.SetValue(list("{", "}"))
            text.startsWith("[", pos) -> record()
            text.startsWith("(", pos) -> function()
            text.startsWith("\"", pos) -> TlaValue.StringValue(string())
            else -> word()
        }
    }

    /** Reads the values between [open] and [close], which commas separate. */
    private fun list(
        open: String,
        close: String
    ): List<TlaValue> {
        expect(open)
        val items = mutableListOf<TlaValue>()
        skipSpace()
        if (!text.startsWith(close, pos)) {
            do {
                items += value()
                skipSpace()
            } while (accept(","))
        }
        expect(close)
        return items
    }

    /** Reads a record `[name |-> value, ...]`. */
    private fun record(): TlaValue {
        expect("[")
        val fields = linkedMapOf<String, TlaValue>()
        do {
            skipSpace()
            val name = name()
            skipSpace()
            expect("|->")
            fields[name] = value()
            skipSpace()
        } while (accept(","))
        expect("]")
        return TlaValue.RecordValue(fields)
    }

    /** Reads a function `(key :> value @@ ...)`. */
    private fun function(): TlaValue {
        expect("(")
        val pairs = mutableListOf<Pair<TlaValue, TlaValue>>()
        do {
            val key = value()
            skipSpace()
            expect(":>")
            pairs += key to value()
            skipSpace()
        } while (accept("@@"))
        expect(")")
        return TlaValue.FunctionValue(pairs)
    }

    /** Reads a string in double quotes, in which a backslash escapes the next character. */
    private fun string(): String {
        expect("\"")
        val out = StringBuilder()
        while (text[pos] != '"') {
            if (text[pos] == '\\') {
                pos++
                out.append(if (text[pos] == 'n') '\n' else text[pos])
            } else {
                out.append(text[pos])
            }
            pos++
        }
        pos++
        return out.toString()
    }

    /** Reads an integer, `TRUE`, `FALSE`, or a model value. */
    private fun word(): TlaValue {
        val start = pos
        accept("-")
        val word = text.substring(start, pos) + name()
        return when {
            word == "TRUE" -> TlaValue.BoolValue(true)
            word == "FALSE" -> TlaValue.BoolValue(false)
            word.toLongOrNull() != null -> TlaValue.IntValue(word.toLong())
            else -> TlaValue.NameValue(word)
        }
    }

    /** Reads a name of letters, digits, and underscores. */
    private fun name(): String {
        val start = pos
        while (pos < text.length && (text[pos].isLetterOrDigit() || text[pos] == '_')) pos++
        require(pos > start) { "No value at position $start of $text" }
        return text.substring(start, pos)
    }

    /** Skips whitespace. */
    private fun skipSpace() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }

    /** Skips [token] and returns true when the text continues with it. */
    private fun accept(token: String): Boolean = text.startsWith(token, pos).also { if (it) pos += token.length }

    /** Skips [token], or fails when the text does not continue with it. */
    private fun expect(token: String) {
        require(accept(token)) { "Expected $token at position $pos of $text" }
    }
}
