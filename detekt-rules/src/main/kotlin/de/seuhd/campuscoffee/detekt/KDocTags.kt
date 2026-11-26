package de.seuhd.campuscoffee.detekt

import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc

private val TAG = Regex("""^@(\w+)\s+\[?`?([^\s\]`]+)""")

/**
 * Returns the subject names of the [tags] in this KDoc, such as the parameter names of the `@param` tags. It reads the
 * lines of the KDoc, as [KDocLength] does, because the Kotlin PSI merges a tag into the previous tag when the text of
 * the previous tag starts with a code span.
 *
 * @receiver the KDoc to read
 * @param tags the kinds of tag to read, such as `KDocKnownTag.PARAM`
 * @return the names that follow these tags, without duplicates
 */
internal fun KDoc.tagSubjects(vararg tags: KDocKnownTag): Set<String> {
    val names = tags.map { it.name.lowercase() }.toSet()
    return text
        .removePrefix("/**")
        .removeSuffix("*/")
        .lines()
        .mapNotNull { TAG.find(it.trim().removePrefix("*").trim()) }
        .filter { it.groupValues[1] in names }
        .map { it.groupValues[2] }
        .toSet()
}
