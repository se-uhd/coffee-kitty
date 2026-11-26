package de.seuhd.campuscoffee.models.alloy

import java.awt.Font
import java.awt.font.FontRenderContext
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Moves each multiplicity in the SVG of a class diagram to the nearest place where it overlaps no edge, arrowhead,
 * class, or other label, because PlantUML places it on the arrowhead and has no setting for its distance
 * (`models/README.md`, section Class diagram).
 */
object EndLabelPlacement {
    private val link = Regex("""<g class="link"[^>]*>.*?</g>""", RegexOption.DOT_MATCHES_ALL)
    private val text = Regex("""<text x="([-\d.]+)" y="([-\d.]+)"([^>]*)>([^<]*)</text>""")
    private val multiplicity = Regex("""\*|\d+|\d+\.\.(\d+|\*)""")
    private val fontSize = Regex("""font-size="([\d.]+)"""")
    private val textLength = Regex("""textLength="([\d.]+)"""")
    private val polygon = Regex("""<polygon points="([^"]+)"""")
    private val path = Regex("""<path d="(M[^"]+)"""")
    private val rect = Regex("""<rect\b[^>]*>""")
    private val attribute = Regex("""([\w-]+)="([^"]*)"""")
    private val renderContext = FontRenderContext(null, true, true)
    private const val MARGIN = 2.0
    private const val MAX_DISTANCE = 40
    private const val DISTANCE_STEP = 2
    private const val ANGLE_STEP = 15
    private const val CURVE_SAMPLES = 24
    private const val ASCENT = 0.8
    private const val DESCENT = 0.25

    /** @param svg the SVG that PlantUML renders for a class diagram */
    fun place(svg: String): String {
        val labels = labels(svg)
        val obstacles = segments(svg)
        val boxes = rects(svg) + labels.filterNot { it.isMultiplicity }.map { it.box }
        val placed = mutableListOf<Box>()
        var result = svg
        labels.filter { it.isMultiplicity }.forEach { label ->
            val free = position(label.box, obstacles, boxes + placed)
            placed += free
            if (free != label.box) {
                val moved =
                    label.element.replaceFirst(
                        "x=\"${label.x}\" y=\"${label.y}\"",
                        "x=\"${format(label.xValue + free.left - label.box.left)}\" " +
                            "y=\"${format(label.yValue + free.top - label.box.top)}\""
                    )
                result = result.replaceFirst(label.element, moved)
            }
        }
        return result
    }

    /**
     * Returns whether a multiplicity in [svg] overlaps an edge, an arrowhead, a class, or another label.
     *
     * @param svg the SVG of a class diagram
     */
    fun hasOverlap(svg: String): Boolean {
        val labels = labels(svg)
        val obstacles = segments(svg)
        val multiplicities = labels.filter { it.isMultiplicity }.map { it.box }
        val boxes = rects(svg) + labels.filterNot { it.isMultiplicity }.map { it.box }
        return multiplicities.withIndex().any { (i, box) ->
            collides(box, obstacles, boxes + multiplicities.filterIndexed { j, _ -> j != i })
        }
    }

    private fun position(
        start: Box,
        obstacles: List<Segment>,
        boxes: List<Box>
    ): Box {
        if (!collides(start, obstacles, boxes)) return start
        val candidates =
            (DISTANCE_STEP..MAX_DISTANCE step DISTANCE_STEP).asSequence().flatMap { distance ->
                (0 until 360 step ANGLE_STEP).asSequence().map { angle ->
                    val radians = Math.toRadians(angle.toDouble())
                    start.shifted(distance * cos(radians), distance * sin(radians))
                }
            }
        return candidates.firstOrNull { !collides(it, obstacles, boxes) } ?: start
    }

    private fun collides(
        box: Box,
        obstacles: List<Segment>,
        boxes: List<Box>
    ): Boolean {
        val padded = box.padded(MARGIN)
        return obstacles.any { padded.intersects(it) } || boxes.any { padded.overlaps(it) }
    }

    private fun labels(svg: String): List<Label> =
        link.findAll(svg).flatMap { group -> text.findAll(group.value).map { Label(it) } }.toList()

    private fun segments(svg: String): List<Segment> =
        link
            .findAll(svg)
            .flatMap { group ->
                polygon.findAll(group.value).flatMap { closed(points(it.groupValues[1])) } +
                    path.findAll(group.value).flatMap { curve(it.groupValues[1]) }
            }.toList()

    private fun rects(svg: String): List<Box> =
        rect
            .findAll(svg)
            .mapNotNull { element ->
                val values = attribute.findAll(element.value).associate { it.groupValues[1] to it.groupValues[2] }
                val numbers = listOf("x", "y", "width", "height").mapNotNull { values[it]?.toDoubleOrNull() }
                numbers.takeIf { it.size == RECT_VALUES }?.let {
                    val (x, y, width) = it
                    Box(x, y, x + width, y + it.last())
                }
            }.toList()

    private fun points(values: String): List<Pair<Double, Double>> =
        values
            .split(',', ' ')
            .filter { it.isNotBlank() }
            .map { it.toDouble() }
            .chunked(2)
            .map { it[0] to it[1] }

    private fun closed(points: List<Pair<Double, Double>>): Sequence<Segment> =
        (points + points.first()).zipWithNext { a, b -> Segment(a, b) }.asSequence()

    private fun curve(data: String): Sequence<Segment> {
        val points =
            Regex("""-?[\d.]+""")
                .findAll(data)
                .map { it.value.toDouble() }
                .chunked(2)
                .map { it[0] to it[1] }
                .toList()
        var start = points.first()
        val sampled = mutableListOf(start)
        points.drop(1).chunked(BEZIER_POINTS).filter { it.size == BEZIER_POINTS }.forEach { (c1, c2, end) ->
            for (sample in 1..CURVE_SAMPLES) sampled += bezier(start, c1, c2, end, sample.toDouble() / CURVE_SAMPLES)
            start = end
        }
        return sampled.zipWithNext { a, b -> Segment(a, b) }.asSequence()
    }

    private class Label(
        match: MatchResult
    ) {
        val element = match.value
        val x = match.groupValues[1]
        val y = match.groupValues[2]
        val xValue = x.toDouble()
        val yValue = y.toDouble()
        val content = match.groupValues[4]
        val isMultiplicity = multiplicity.matches(content)
        val box: Box

        init {
            val size =
                fontSize
                    .find(match.groupValues[3])
                    ?.groupValues
                    ?.get(1)
                    ?.toDouble() ?: DEFAULT_FONT_SIZE
            val width =
                textLength
                    .find(match.groupValues[3])
                    ?.groupValues
                    ?.get(1)
                    ?.toDouble()
                    ?: Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        1
                    ).deriveFont(size.toFloat()).getStringBounds(content, renderContext).width
            box = Box(xValue, yValue - ASCENT * size, xValue + width, yValue + DESCENT * size)
        }
    }

    private data class Segment(
        val a: Pair<Double, Double>,
        val b: Pair<Double, Double>
    )

    private data class Box(
        val left: Double,
        val top: Double,
        val right: Double,
        val bottom: Double
    ) {
        fun shifted(
            dx: Double,
            dy: Double
        ) = Box(left + dx, top + dy, right + dx, bottom + dy)

        fun padded(margin: Double) = Box(left - margin, top - margin, right + margin, bottom + margin)

        fun overlaps(other: Box) = left < other.right && other.left < right && top < other.bottom && other.top < bottom

        fun contains(point: Pair<Double, Double>) = point.first in left..right && point.second in top..bottom

        fun intersects(segment: Segment): Boolean {
            if (contains(segment.a) || contains(segment.b)) return true
            val corners = listOf(left to top, right to top, right to bottom, left to bottom)
            return (corners + corners.first()).zipWithNext().any { (c, d) -> crosses(segment.a, segment.b, c, d) }
        }

        private fun crosses(
            p: Pair<Double, Double>,
            q: Pair<Double, Double>,
            r: Pair<Double, Double>,
            s: Pair<Double, Double>
        ): Boolean {
            fun side(
                a: Pair<Double, Double>,
                b: Pair<Double, Double>,
                c: Pair<Double, Double>
            ) = (b.first - a.first) * (c.second - a.second) - (b.second - a.second) * (c.first - a.first)
            return side(p, q, r) * side(p, q, s) < 0 && side(r, s, p) * side(r, s, q) < 0
        }
    }

    private const val DEFAULT_FONT_SIZE = 13.0
    private const val BEZIER_POINTS = 3
    private const val RECT_VALUES = 4
}

private const val BEZIER_WEIGHT = 3

private fun bezier(
    p0: Pair<Double, Double>,
    p1: Pair<Double, Double>,
    p2: Pair<Double, Double>,
    p3: Pair<Double, Double>,
    t: Double
): Pair<Double, Double> {
    val u = 1 - t

    fun at(
        a: Double,
        b: Double,
        c: Double,
        d: Double
    ) = u * u * u * a + BEZIER_WEIGHT * u * u * t * b + BEZIER_WEIGHT * u * t * t * c + t * t * t * d
    return at(p0.first, p1.first, p2.first, p3.first) to at(p0.second, p1.second, p2.second, p3.second)
}

private fun format(value: Double): String = "%.3f".format(Locale.ROOT, value).trimEnd('0').trimEnd('.')
