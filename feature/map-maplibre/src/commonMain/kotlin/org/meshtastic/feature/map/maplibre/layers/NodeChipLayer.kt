/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
@file:Suppress("LongParameterList")

package org.meshtastic.feature.map.maplibre.layers

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.expressions.dsl.asBoolean
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.condition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.value.BooleanValue
import org.maplibre.compose.expressions.value.FloatValue
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.FeaturesClickHandler
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapState
import org.maplibre.compose.sources.VectorSource
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.model.Node
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Person
import org.meshtastic.core.ui.icon.Temperature
import org.meshtastic.core.ui.theme.LocalMonochrome
import org.meshtastic.feature.map.MapNodePolicy
import org.meshtastic.feature.map.maplibre.geojson.ClusterMember
import org.meshtastic.feature.map.maplibre.geojson.MapChipGlyph
import org.meshtastic.feature.map.maplibre.geojson.MapChipKey
import org.meshtastic.feature.map.maplibre.geojson.NodeFeatureKeys
import org.meshtastic.feature.map.maplibre.geojson.featureValue
import org.meshtastic.feature.map.maplibre.geojson.lenientBoolean
import org.meshtastic.feature.map.maplibre.geojson.lenientInt
import org.meshtastic.feature.map.maplibre.geojson.stackedMembers
import org.meshtastic.feature.map.maplibre.geojson.toNodeChip
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Node markers drawn as chips — the rounded, node-coloured badge with the short name in it that
 * [org.meshtastic.core.ui.component.NodeChip] renders everywhere else in the app, and that the Google map also puts on
 * the map.
 *
 * A MapLibre text layer cannot draw a chip's label: a short name can be emoji, which the basemap glyphs lack, and it
 * has no strikethrough or minimum width. So a tinted SDF sprite under `iconTextFit` is no substitute.
 *
 * So the whole chip is rasterized instead, exactly as the Google flavor rasterizes a `NodeChip` per marker. One image
 * per *distinct* chip, and a single layer that picks between them with a `match` on the chip key the feature carries —
 * rather than a layer per chip, which is what made a shared short name a fatal duplicate layer id.
 *
 * @param chipFilter narrows which features get a chip. The main map passes the unclustered-only test; the smaller maps
 *   have no clusters and pass nothing.
 * @param nodes the nodes worth an image, already narrowed and ordered by the caller — [NodeLayers] passes only what the
 *   padded viewport can see, loneliest first, so the budget is spent on what is on screen rather than on whatever
 *   happens to come first in a mesh of thousands.
 */
@Composable
internal fun NodeChipLayer(
    id: String,
    source: VectorSource,
    nodes: List<Node>,
    onNodeClick: ((Int) -> Unit)? = null,
    chipFilter: Expression<BooleanValue>? = null,
    onStack: ((List<ClusterMember>) -> Unit)? = null,
) = MapChipLayer(
    id = id,
    source = source,
    // Already ordered by how likely each node is to be drawn on its own; MapChipLayer keeps as many as it can
    // afford.
    chips = remember(nodes, LocalMonochrome.current) { nodes.map { it.toNodeChip() } },
    filter = chipFilter,
    onClick =
    onNodeClick?.let { click ->
        { features -> nodeTap(features, click, onStack) }
    },
)

/**
 * A tap on a node: the chip drawn on top is the one the user meant. Query order is not that order, so pick by the rank
 * [chipSortKey] draws by — on the Google map the highest zIndex marker takes the tap. Several nodes on one spot are
 * listed for a choice.
 */
internal fun nodeTap(
    features: List<Feature<*, JsonObject?>>,
    click: (Int) -> Unit,
    onStack: ((List<ClusterMember>) -> Unit)?,
): ClickResult {
    val top = features.maxByOrNull { it.properties.chipRank() }
    Logger.i {
        "Węzeł na mapie: dotknięto, elementów: ${features.size}, właściwości: " +
            "${top?.properties?.keys}, numer: ${top?.properties?.get(NodeFeatureKeys.NODE_NUM)}"
    }
    val stack = if (onStack != null && top != null) stackedMembers(features, top) else emptyList()
    return if (stack.isNotEmpty()) {
        onStack?.invoke(stack)
        ClickResult.Consume
    } else {
        top?.properties?.get(NodeFeatureKeys.NODE_NUM)?.let { nodeNum ->
            nodeNum.lenientInt()?.let(click)
            ClickResult.Consume
        } ?: ClickResult.Pass
    }
}

/**
 * How far up the stack a node's chip is drawn, mirroring the Google flavor's `NodeClusterItem.zIndex`: this node and
 * favorites ride above everything else, and nothing distinguishes the rest.
 */
internal fun JsonObject?.chipRank(): Int = when {
    this == null -> MapNodePolicy.PRIORITY_ORDINARY
    this[NodeFeatureKeys.IS_SELF].lenientBoolean() == true -> MapNodePolicy.PRIORITY_PROMINENT
    this[NodeFeatureKeys.IS_FAVORITE].lenientBoolean() == true -> MapNodePolicy.PRIORITY_PROMINENT
    else -> MapNodePolicy.PRIORITY_ORDINARY
}

/**
 * The same ranking as an expression, for the layer's `symbol-sort-key`.
 *
 * With `icon-allow-overlap` on, the spec draws **higher** sort keys over lower ones (it is the other way round when
 * overlap is disallowed, which is what makes this easy to get backwards). Without it the draw order is the source
 * order, so which of two overlapping chips is legible was arbitrary and could change on any data refresh.
 */
private fun chipSortKey(): Expression<FloatValue> = switch(
    condition(feature[NodeFeatureKeys.IS_SELF].asBoolean(), const(MapNodePolicy.PRIORITY_PROMINENT.toFloat())),
    condition(feature[NodeFeatureKeys.IS_FAVORITE].asBoolean(), const(MapNodePolicy.PRIORITY_PROMINENT.toFloat())),
    fallback = const(MapNodePolicy.PRIORITY_ORDINARY.toFloat()),
)

/**
 * The chip layer itself: one rasterized image per distinct chip, picked per feature by the [MapChipKey.featureValue]
 * the feature carries under [NodeFeatureKeys.CHIP].
 *
 * Shared by the node maps and the discovery map, which draws differently coloured chips for the same reason.
 */
@Suppress("SpreadOperator") // switch() only takes its cases as varargs
@Composable
internal fun MapChipLayer(
    id: String,
    source: VectorSource,
    chips: List<MapChipKey>,
    filter: Expression<BooleanValue>? = null,
    onClick: FeaturesClickHandler? = null,
) {
    // Only ever grows (until the ceiling): a chip that scrolls out of view stays registered, so panning back does not
    // drop and re-upload its image, and a newly visible node adds one image instead of swapping the whole set.
    val seen = remember { LinkedHashSet<MapChipKey>() }
    val distinct =
        remember(chips) {
            val wanted = chips.distinct().take(MAX_CHIP_IMAGES)
            if (seen.size + wanted.count { it !in seen } > MAX_CHIP_IMAGES) seen.clear()
            seen.addAll(wanted)
            seen.toList()
        }
    val images = rememberChipImages(distinct)
    val blank = rememberBlankPainter()

    SymbolLayer(
        id = id,
        source = source,
        filter = filter,
        iconImage =
        switch(
            input = feature[NodeFeatureKeys.CHIP].asString(),
            *images.map { (key, chip) -> case(key, image(chip.painter, chip.size)) }.toTypedArray(),
            // Nothing at all for a node past the image ceiling, rather than someone else's chip.
            fallback = image(blank, DpSize(1.dp, 1.dp)),
        ),
        // The bottom of the sprite is the tail's tip, so this — not the sprite's centre — is what lands on
        // the node's exact coordinate.
        iconAnchor = const(SymbolAnchor.Bottom),
        iconAllowOverlap = const(true),
        sortKey = chipSortKey(),
        onClick = onClick,
    )
}

/** A rasterized chip and the size MapLibre should draw it at. */
private class ChipImage(val painter: Painter, val size: DpSize)

/**
 * Rasterizes every distinct chip once.
 *
 * One `remember` over the whole set rather than a `remember` per chip: [Painter] identity is part of the key
 * maplibre-compose reference-counts style images by, so a painter rebuilt on recomposition would drop and re-upload
 * every chip on the map each time the node list emits.
 */
@Composable
private fun rememberChipImages(chips: List<MapChipKey>): Map<String, ChipImage> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.labelSmall

    // Vector painters have to be built in composition, so they are resolved here and handed to the rasterizer rather
    // than looked up inside it.
    val glyphs =
        mapOf(
            MapChipGlyph.SENSOR to rememberVectorPainter(MeshtasticIcons.Temperature),
            MapChipGlyph.SOCIAL to rememberVectorPainter(MeshtasticIcons.Person),
        )

    // One painter per chip for as long as its inputs hold: a painter's identity is what the map keys its image by, so
    // rebuilding every chip because one more was added would re-upload them all.
    val cache = remember(measurer, density, textStyle, glyphs) { HashMap<String, ChipImage>() }
    return remember(chips, cache) {
        chips.associate { chip ->
            val key = chip.featureValue()
            key to cache.getOrPut(key) { chip.rasterize(measurer, textStyle, density, glyphs) }
        }
    }
}

private fun MapChipKey.rasterize(
    measurer: TextMeasurer,
    textStyle: TextStyle,
    density: Density,
    glyphs: Map<MapChipGlyph, Painter>,
): ChipImage {
    val style =
        textStyle.copy(textDecoration = TextDecoration.LineThrough.takeIf { struckThrough }, color = Color(foreground))
    // A chip carrying a glyph shows the icon instead of the name, so it measures no text.
    val layout = if (glyph == null) measurer.measure(AnnotatedString(label), style, density = density) else null

    // NodeChip is a Card with 8dp of horizontal padding around text that is at least 64dp wide and 28dp tall. Sizing
    // the sprite the same way is what keeps a map chip the size of the chip in the node list. An icon chip needs no
    // room for text, so it is a square badge instead.
    val width =
        with(density) {
            layout?.let { maxOf(MIN_WIDTH_DP.dp, it.size.width.toDp() + HORIZONTAL_PADDING_DP.dp * 2) } ?: HEIGHT_DP.dp
        }
    val cornerRadiusPx = with(density) { CORNER_RADIUS_DP.dp.toPx() }
    val borderPx = if (outlined) with(density) { BORDER_DP.dp.toPx() } else 0f

    return ChipImage(
        painter =
        ChipPainter(
            chip = this,
            layout = layout,
            cornerRadiusPx = cornerRadiusPx,
            borderPx = borderPx,
            glyphPainter = glyph?.let(glyphs::get),
            glyphSizePx = with(density) { GLYPH_DP.dp.toPx() },
            tailHeightPx = with(density) { TAIL_HEIGHT_DP.dp.toPx() },
            tailWidthPx = with(density) { TAIL_WIDTH_DP.dp.toPx() },
            gapPx = with(density) { GAP_DP.dp.toPx() },
        ),
        // Taller than the chip itself: the extra strip at the bottom is the tail plus a blank gap, drawn (or
        // left blank) by ChipPainter.
        size = DpSize(width, HEIGHT_DP.dp + TAIL_HEIGHT_DP.dp + GAP_DP.dp),
    )
}

/** Draws the chip: the marker's colour behind, and its name or its icon centred in the foreground colour. */
private class ChipPainter(
    private val chip: MapChipKey,
    private val layout: TextLayoutResult?,
    private val cornerRadiusPx: Float,
    private val borderPx: Float,
    private val glyphPainter: Painter?,
    private val glyphSizePx: Float,
    private val tailHeightPx: Float,
    private val tailWidthPx: Float,
    private val gapPx: Float,
) : Painter() {
    // Never read: the caller always names an explicit size, which is what makes the chip's width follow its text.
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        // The body is the chip proper, then the tail, then a blank gap at the very bottom — the anchor point
        // stays there so the dot drawn separately at the node's exact coordinate shows through beneath the
        // tail's tip instead of touching it.
        val bodyHeightPx = size.height - tailHeightPx - gapPx

        drawRoundRect(
            color = Color(chip.background),
            size = Size(size.width, bodyHeightPx),
            cornerRadius = CornerRadius(cornerRadiusPx),
        )
        if (borderPx > 0f) {
            // Inset by half the stroke: a centred stroke on the sprite's own edge would be clipped to half width.
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(borderPx / 2f, borderPx / 2f),
                size = Size(size.width - borderPx, bodyHeightPx - borderPx),
                cornerRadius = CornerRadius(cornerRadiusPx),
                style = Stroke(width = borderPx),
            )
        }
        if (tailHeightPx > 0f) {
            // The tail ends at bodyHeightPx + tailHeightPx, not at size.height — the remaining strip down to
            // size.height is the blank gap that keeps the tip clear of the dot at the node's exact coordinate.
            val centerX = size.width / 2f
            val halfTail = tailWidthPx / 2f
            val tailTipY = bodyHeightPx + tailHeightPx
            val tailPath =
                Path().apply {
                    moveTo(centerX - halfTail, bodyHeightPx)
                    lineTo(centerX + halfTail, bodyHeightPx)
                    lineTo(centerX, tailTipY)
                    close()
                }
            // A sharp fill of the triangle, then the same outline stroked with round joins on top — the
            // rounded stroke smooths every corner (the tip and both where it meets the chip body) into one
            // continuous curve instead of a sharp point or a separate circle stuck on top of one.
            drawPath(path = tailPath, color = Color(chip.background))
            drawPath(
                path = tailPath,
                color = Color(chip.background),
                style = Stroke(width = TAIL_ROUNDING_DP.dp.toPx() * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        if (glyphPainter != null) {
            val glyph = Size(glyphSizePx, glyphSizePx)
            translate(left = (size.width - glyphSizePx) / 2f, top = (bodyHeightPx - glyphSizePx) / 2f) {
                with(glyphPainter) { draw(glyph, colorFilter = ColorFilter.tint(Color(chip.foreground))) }
            }
        } else if (layout != null) {
            drawText(
                textLayoutResult = layout,
                topLeft =
                Offset(x = (size.width - layout.size.width) / 2f, y = (bodyHeightPx - layout.size.height) / 2f),
            )
        }
    }
}

/** The image a node past [MAX_CHIP_IMAGES] resolves to: one transparent pixel, drawn once. */
@Composable
private fun rememberBlankPainter(): Painter = remember {
    object : Painter() {
        override val intrinsicSize: Size = Size.Unspecified

        override fun DrawScope.onDraw() = Unit
    }
}

/** Material 3's `shapes.small`, which is the shape NodeChip asks its Card for. */
private const val CORNER_RADIUS_DP = 6
private const val MIN_WIDTH_DP = 44
private const val HEIGHT_DP = 20
private const val HORIZONTAL_PADDING_DP = 6

/** Border width for an outlined chip, matching the discovery map's own 1dp. */
private const val BORDER_DP = 1

/** Icon size inside a glyph chip, matching the discovery marker's own 16dp. */
private const val GLYPH_DP = 12

/** The little triangular tail under the chip, pointing at the node's exact coordinate. */
private const val TAIL_HEIGHT_DP = 6
private const val TAIL_WIDTH_DP = 8

/** Blank space between the tail's tip and the node's exact coordinate, so the tail does not touch the dot. */
private const val GAP_DP = 8

/** Fillet radius rounding every corner of the tail — the tip and both where it meets the chip body. */
private const val TAIL_ROUNDING_DP = 2

/**
 * A ceiling on how many chip images one layer will hold.
 *
 * Each distinct chip is a bitmap uploaded to the style, so this bounds both the memory and the size of the `match`
 * expression. Far above any real mesh in view; it exists so a pathological node list degrades instead of stalling — and
 * what it degrades to is the plain coloured dot `NodeLayers` draws underneath, which stays visible and tappable.
 */
private const val MAX_CHIP_IMAGES = 250

/** Half the width and the height of the area a chip covers above its node, used to find the chip a tap landed on. */
private const val TAP_HALF_WIDTH_DP = 44f
private const val RANK_TIE_DP = 10f
private const val TAP_TOP_MARGIN_DP = 8f
private const val TAP_HEIGHT_DP = 38f

/** How far above the node's spot the middle of the bubble sits: gap + tail + half the body (8 + 6 + 20/2). */
private const val CHIP_CENTRE_DP = (GAP_DP + TAIL_HEIGHT_DP + HEIGHT_DP / 2).toFloat()
private const val STACK_TOLERANCE_DP = 2f

/** Larger than any distance on screen, so the rank decides first and the distance only between equal ranks. */
private const val RANK_WEIGHT = 100_000f

/**
 * Finds the node whose chip (the name bubble above the node's spot) was tapped, in screen coordinates.
 *
 * Done on the screen, not with an invisible shape on the map, so it behaves the same on every platform and keeps
 * working when the map is turned or tilted. The bubble stands above its node, so the node's dot lies just below the
 * tap: every dot in that box is a candidate and the one whose bubble centre is closest to the tap wins (favourites and
 * our own node first, as they are drawn on top). Returns whether a node (or a stack of them) was opened.
 */
internal suspend fun nodeTapAt(
    state: MapState,
    tap: Position,
    click: (Int) -> Unit,
    onStack: (List<ClusterMember>) -> Unit,
): Boolean {
    val at = state.screenLocationFromPosition(tap) ?: return false
    val scale = screenScale(state, tap, at)
    val search = chipCandidates(state, at, scale)
    val candidates = search.candidates
    // The closest bubble wins; rank (favourites on top) only decides between bubbles that are about as close.
    val closest = candidates.minOfOrNull { it.third }
    val pool = candidates.filter { closest != null && it.third <= closest + RANK_TIE_DP }
    val best = pool.maxByOrNull { it.second * RANK_WEIGHT - it.third }
    Logger.i {
        "Dotyk w węzeł: ekran ${at.x.value.roundTo1()}, ${at.y.value.roundTo1()}, skala ekranu ${scale.roundTo2()}, " +
            "zoom ${state.cameraPosition.zoom.toFloat().roundTo1()}, kropek na ekranie ${search.rendered}, " +
            "najbliższa kropka: ${search.nearest}, kandydatów ${candidates.size}, wybrany ${best?.first?.properties?.get(
                NodeFeatureKeys.NODE_NUM,
            )}"
    }
    val near = best?.let { top -> candidates.filter { it.third <= top.third + STACK_TOLERANCE_DP }.map { it.first } }
    val stack = if (best != null && near != null) stackedMembers(near, best.first) else emptyList()
    val nodeNum = best?.first?.properties?.get(NodeFeatureKeys.NODE_NUM).lenientInt()
    return when {
        best == null -> false

        stack.isNotEmpty() -> {
            onStack(stack)
            true
        }

        nodeNum != null -> {
            click(nodeNum)
            true
        }

        else -> false
    }
}

/** What [chipCandidates] found: the dots inside the tap box, and a short account of the geometry for the log. */
private class ChipSearch(
    val candidates: List<Triple<Feature<*, JsonObject?>, Int, Float>>,
    val rendered: Int,
    val nearest: String,
)

/**
 * How many units the map's screen positions are per real dp, found by asking it where two spots a known distance apart
 * land. It should be 1; the iPhone has been seen answering in a different unit, which put every tap box off by that
 * factor (the box then only reached a dot's own spot and never the bubble above it). Distances measured on screen are
 * divided by it. Answers close to 1 are taken as 1, and nonsense is ignored.
 */
private suspend fun screenScale(state: MapState, tap: Position, at: DpOffset): Float {
    val metersPerDp = state.viewport?.metersPerDpAtCenter ?: 0.0
    val probe = if (metersPerDp > 0.0) probeSpotEast(state, tap, metersPerDp) else null
    val east = probe?.let { state.screenLocationFromPosition(it) }
    val scale = east?.let { (hypot((it.x - at.x).value, (it.y - at.y).value) / SCALE_PROBE_DP).toFloat() } ?: 1f
    return if (scale in SCALE_LIMIT_MIN..SCALE_LIMIT_MAX && scale !in SCALE_TRUSTED_MIN..SCALE_TRUSTED_MAX) scale else 1f
}

/** The spot [SCALE_PROBE_DP] dp east of [tap], worked out from the map's own metres per dp (given for its centre). */
private fun probeSpotEast(state: MapState, tap: Position, metersPerDp: Double): Position {
    val toRad = PI / HALF_TURN_DEG
    // A spot further north or south than the centre has shorter degrees, so fewer metres per dp.
    val localMetersPerDp = metersPerDp * cos(tap.latitude * toRad) / cos(state.cameraPosition.center.latitude * toRad)
    val degrees = SCALE_PROBE_DP * localMetersPerDp / (EARTH_RADIUS_M * toRad * cos(tap.latitude * toRad))
    return Position(tap.longitude + degrees, tap.latitude)
}

private fun Float.roundTo1(): Float = (this * 10f).toInt() / 10f

private fun Float.roundTo2(): Float = (this * 100f).toInt() / 100f

/**
 * The dots just below the tap, each with its rank (favourites on top) and its distance from the bubble's centre.
 *
 * The map is asked about the whole screen and the box is applied here, on the same screen positions the tap itself came
 * from: a box handed to the map can be read in a slightly different scale (seen on the iPhone), which put some
 * bubbles' taps off their targets while others, near the corner the scale starts from, were right. [scale] corrects
 * the same kind of difference in the screen positions themselves.
 */
private suspend fun chipCandidates(state: MapState, at: DpOffset, scale: Float): ChipSearch {
    val whole = DpRect(left = 0.dp, top = 0.dp, right = WHOLE_SCREEN_DP.dp, bottom = WHOLE_SCREEN_DP.dp)
    val dots = state.queryRenderedFeatures(whole, layerIds = setOf(NODE_DOT_LAYER_ID))
    var nearest = "brak"
    var nearestDistance = Float.MAX_VALUE
    val found =
        dots.mapNotNull { feature ->
            val spot = (feature.geometry as? Point)?.coordinates
            val dot = spot?.let { state.screenLocationFromPosition(it) }
            val dx = ((dot?.x?.value ?: Float.NaN) - at.x.value) / scale
            val below = ((dot?.y?.value ?: Float.NaN) - at.y.value) / scale
            val dy = below - CHIP_CENTRE_DP
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearest = "w bok ${dx.roundTo1()}, w dół ${below.roundTo1()}"
            }
            // NaN (no position) fails every comparison below, so such a dot is left out.
            val inside = abs(dx) <= TAP_HALF_WIDTH_DP && below >= -TAP_TOP_MARGIN_DP && below <= TAP_HEIGHT_DP
            if (inside) Triple(feature, feature.properties.chipRank(), distance) else null
        }
    return ChipSearch(found, dots.size, nearest)
}

/** Larger than any screen, so a query with it covers every dot the map draws. */
private const val WHOLE_SCREEN_DP = 4000

/** How far east of the tap the screen scale is probed, in dp. */
private const val SCALE_PROBE_DP = 100.0
private const val HALF_TURN_DEG = 180.0
private const val EARTH_RADIUS_M = 6_371_008.8

/** A measured scale this close to 1 is just rounding and a tilted view; it is left alone. */
private const val SCALE_TRUSTED_MIN = 0.8f
private const val SCALE_TRUSTED_MAX = 1.25f

/** Beyond these the measurement is taken as wrong, not as a unit difference. */
private const val SCALE_LIMIT_MIN = 0.2f
private const val SCALE_LIMIT_MAX = 6f
