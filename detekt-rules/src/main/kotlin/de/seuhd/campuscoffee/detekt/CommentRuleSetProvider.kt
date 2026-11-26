package de.seuhd.campuscoffee.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Provides the rule set `campus-coffee-comments`, which detekt finds through `META-INF/services`. The text rules of
 * doc/adr/013-limits-for-text.md: KDoc only on contracts, short KDoc, none on a private declaration, no other
 * comments, and short OpenAPI texts.
 */
class CommentRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("campus-coffee-comments")

    override fun instance(): RuleSet =
        RuleSet(
            ruleSetId,
            listOf(
                ::UndocumentedContract,
                ::UndocumentedFunctionParameter,
                ::KDocLength,
                ::LineComment,
                ::PrivateKDoc,
                ::OpenApiTextLength
            )
        )
}
