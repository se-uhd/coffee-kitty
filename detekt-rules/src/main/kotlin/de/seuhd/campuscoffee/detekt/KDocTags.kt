package de.seuhd.campuscoffee.detekt

import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Collects the subject names of the given tags in this KDoc, such as the parameter names of the `@param` tags.
 *
 * @receiver the KDoc block to read
 * @param tags the tags to read the subject names from, such as `KDocKnownTag.PARAM`
 * @return the names that follow those tags
 */
internal fun KDoc.tagSubjects(vararg tags: KDocKnownTag): Set<String> =
    collectDescendantsOfType<KDocTag>()
        .filter { it.knownTag in tags }
        .mapNotNull { it.getSubjectName() }
        .toSet()
