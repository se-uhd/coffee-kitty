package de.seuhd.campuscoffee.models.alloy

import de.seuhd.campuscoffee.models.statechart.renderSvg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class EndLabelPlacementTest {
    private val crossed =
        """
        <svg><g class="link"><path d="M10,0 C10,20 10,40 10,60" fill="none"/>
        <text x="6" y="34" fill="#000" font-size="13">1</text></g></svg>
        """.trimIndent()

    @Test
    fun `place moves a multiplicity off the edge that crosses it`() {
        assertThat(EndLabelPlacement.hasOverlap(crossed)).isTrue()

        assertThat(EndLabelPlacement.hasOverlap(EndLabelPlacement.place(crossed))).isFalse()
    }

    @Test
    fun `place moves a multiplicity out of a class box`() {
        val inBox =
            """
            <svg><rect x="0" y="20" width="30" height="30" fill="#F1F1F1"/>
            <g class="link"><text x="5" y="35" fill="#000" font-size="13">1</text></g></svg>
            """.trimIndent()

        assertThat(EndLabelPlacement.hasOverlap(inBox)).isTrue()
        assertThat(EndLabelPlacement.hasOverlap(EndLabelPlacement.place(inBox))).isFalse()
    }

    @Test
    fun `place keeps a multiplicity that overlaps nothing`() {
        val free = crossed.replace("x=\"6\"", "x=\"40\"")

        assertThat(EndLabelPlacement.place(free)).isEqualTo(free)
    }

    @Test
    fun `the class diagram of DomainModel has no multiplicity on an edge, an arrowhead, or a class`() {
        val source = toClassDiagram("DomainModel", Path.of("src/main/alloy/DomainModel.als").readText())

        assertThat(EndLabelPlacement.hasOverlap(EndLabelPlacement.place(renderSvg(source)))).isFalse()
    }
}
