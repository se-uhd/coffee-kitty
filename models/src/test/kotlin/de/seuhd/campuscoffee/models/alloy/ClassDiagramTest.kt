package de.seuhd.campuscoffee.models.alloy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClassDiagramTest {
    private val model =
        """
        abstract sig Text {}
        one sig ShortText extends Text {}
        abstract sig Kind {}
        one sig RED, GREEN extends Kind {}
        sig Owner { name: one Text, nickname: lone Text, kind: one Kind }
        sig Thing { owner: one Owner, friend: lone Thing }
        """.trimIndent()

    private fun lines(text: String) = toClassDiagram("Toy", text).lines()

    @Test
    fun `toClassDiagram writes a class with an attribute for each field of a value sig`() {
        assertThat(lines(model)).containsSubsequence("class Owner {", "  name : Text", "  nickname : Text [0..1]")
    }

    @Test
    fun `toClassDiagram writes an enum with a constant for each one sig of an abstract sig`() {
        assertThat(lines(model)).containsSubsequence("enum Kind {", "  RED", "  GREEN", "}")
    }

    @Test
    fun `the tail of an arrow has the number of objects that the facts let refer to one object`() {
        val oneThingPerOwner = "$model\nfact OneThingPerOwner { all o: Owner | one owner.o }"

        assertThat(lines(model)).contains("Thing \"*\" --> \"1\" Owner : owner")
        assertThat(lines(oneThingPerOwner)).contains("Thing \"1\" --> \"1\" Owner : owner")
    }

    @Test
    fun `the legend shows whether each assertion holds`() {
        val check = "\nassert NoSelfFriend { no t: Thing | t.friend = t }\ncheck NoSelfFriend"
        val fact = "\nfact NoSelf { no t: Thing | t.friend = t }"

        assertThat(lines(model + check)).contains("| NoSelfFriend | assertion | breaks |")
        assertThat(
            lines(model + fact + check)
        ).contains("| NoSelf | fact | assumed |", "| NoSelfFriend | assertion | holds |")
    }
}
