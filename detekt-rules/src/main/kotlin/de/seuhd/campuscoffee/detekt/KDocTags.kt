package de.seuhd.campuscoffee.detekt

import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Returns the subject names of the [tags] in this KDoc, such as the parameter names of the `@param` tags.
 *
 * @receiver the KDoc to read
 * @param tags the kinds of tag to read, such as `KDocKnownTag.PARAM`
 * @return the names that follow these tags, without duplicates
 */
internal fun KDoc.tagSubjects(vararg tags: KDocKnownTag): Set<String> =
    collectDescendantsOfType<KDocTag>()
        .filter { it.knownTag in tags }
        .mapNotNull { it.getSubjectName() }
        .toSet()
