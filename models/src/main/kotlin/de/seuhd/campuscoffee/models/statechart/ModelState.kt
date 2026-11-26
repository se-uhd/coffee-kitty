package de.seuhd.campuscoffee.models.statechart

/**
 * One state of a TLA+ model. Two states are equal when each variable has the same value.
 *
 * @property values the value of each variable, by its name
 */
data class ModelState(
    val values: Map<String, TlaValue>
) {
    /**
     * Returns the value of the variable [name].
     *
     * @param name the name of a variable of the model
     */
    operator fun get(name: String): TlaValue =
        values[name] ?: error("The model has no variable $name. Its variables are ${values.keys}.")
}

/**
 * Parses the lines `/\ name = value` that TLC prints for one state. A value can continue on the next lines. For a
 * model with one variable, TLC prints the line without `/\ `.
 *
 * @param text the lines of one state
 * @return the state with the value of each variable
 */
fun parseModelState(text: String): ModelState {
    val assignments = mutableListOf<StringBuilder>()
    for (line in text.lines().map { it.trim() }.filter { it.isNotEmpty() }) {
        if (line.startsWith("/\\ ") || assignments.isEmpty()) {
            assignments += StringBuilder(line.removePrefix("/\\ "))
        } else {
            assignments.last().append(' ').append(line)
        }
    }
    return ModelState(
        assignments.associate { assignment ->
            val parts = assignment.split(" = ", limit = 2)
            require(parts.size == 2) { "TLC printed the state line \"$assignment\" without \" = \"." }
            parts[0].trim() to parseTlaValue(parts[1])
        }
    )
}
