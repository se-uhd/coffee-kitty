package de.seuhd.campuscoffee.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Provides the rule set `campus-coffee-comments`, which detekt finds through `META-INF/services`. The comment rules of
 * doc/adr/013-limits-for-text.md: KDoc only on contracts, short KDoc, and no other comments.
 */
class CommentRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("campus-coffee-comments")

    override fun instance(): RuleSet =
        RuleSet(ruleSetId, listOf(::UndocumentedContract, ::UndocumentedFunctionParameter, ::KDocLength, ::LineComment))
}
