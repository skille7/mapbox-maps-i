// This file is generated.

package com.mapbox.maps.extension.compose.style.sources.generated

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.mapbox.bindgen.Value
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.style.DoubleListValue
import com.mapbox.maps.extension.compose.style.IdGenerator.generateRandomSourceId
import com.mapbox.maps.extension.compose.style.LongValue
import com.mapbox.maps.extension.compose.style.StringListValue
import com.mapbox.maps.extension.compose.style.StringValue
import com.mapbox.maps.extension.compose.style.sources.RasterLayers
import com.mapbox.maps.extension.compose.style.sources.SourceState
import com.mapbox.maps.extension.compose.style.sources.TileCacheBudget
import java.util.Objects

/**
 * Create and [rememberSaveable] a [RasterArraySourceState] using [RasterArraySourceState.Saver].
 * [init] will be called when the [RasterArraySourceState] is first created to configure its
 * initial state.
 *
 * @param key An optional key to be used as a key for the saved value. If not provided we use the
 * automatically generated by the Compose runtime which is unique for the every exact code location
 * in the composition tree.
 * @param sourceId The optional sourceId for the source state, by default, a random source ID will be used.
 * @param init A function initialise this [RasterArraySourceState].
 */
@Composable
@MapboxExperimental
public inline fun rememberRasterArraySourceState(
  key: String? = null,
  sourceId: String = remember {
    generateRandomSourceId("raster-array")
  },
  crossinline init: RasterArraySourceState.() -> Unit = {}
): RasterArraySourceState = rememberSaveable(key = key, saver = RasterArraySourceState.Saver) {
  RasterArraySourceState(sourceId).apply(init)
}

/**
 * A raster array source
 *
 * @see [The online documentation](https://docs.mapbox.com/style-spec/reference/sources#raster_array)
 *
 * @param sourceId The id of the source state, by default a random generated ID will be used.
 * @param initialProperties The initial mutable properties of the source.
 */
@MapboxExperimental
public class RasterArraySourceState private constructor(
  sourceId: String,
  sourceType: String,
  initialProperties: Map<String, Pair<Boolean, Value>>,
  url: StringValue,
  tiles: StringListValue,
  bounds: DoubleListValue,
  minZoom: LongValue,
  maxZoom: LongValue,
  tileSize: LongValue,
  attribution: StringValue,
  rasterLayers: RasterLayers,
  tileCacheBudget: TileCacheBudget,
) : SourceState(
  sourceId = sourceId,
  sourceType = sourceType,
  initialProperties = initialProperties,
) {
  public constructor(
    sourceId: String = generateRandomSourceId("raster-array"),
  ) : this(
    sourceId = sourceId,
    sourceType = "raster-array",
    initialProperties = emptyMap(),
    url = StringValue.INITIAL,
    tiles = StringListValue.INITIAL,
    bounds = DoubleListValue.INITIAL,
    minZoom = LongValue.INITIAL,
    maxZoom = LongValue.INITIAL,
    tileSize = LongValue.INITIAL,
    attribution = StringValue.INITIAL,
    rasterLayers = RasterLayers.INITIAL,
    tileCacheBudget = TileCacheBudget.INITIAL,
  )

  private val urlState: MutableState<StringValue> = mutableStateOf(url)

  /**
   * A URL to a TileJSON resource. Supported protocols are `http:`, `https:`, and `mapbox://<Tileset ID>`.
   */
  public var url: StringValue by urlState

  @Composable
  private fun UpdateUrl() {
    urlState.value.apply {
      if (notInitial) {
        setBuilderProperty("url", value)
      }
    }
  }
  private val tilesState: MutableState<StringListValue> = mutableStateOf(tiles)

  /**
   * An array of one or more tile source URLs, as in the TileJSON spec.
   */
  public var tiles: StringListValue by tilesState

  @Composable
  private fun UpdateTiles() {
    tilesState.value.apply {
      if (notInitial) {
        setBuilderProperty("tiles", value)
      }
    }
  }
  private val boundsState: MutableState<DoubleListValue> = mutableStateOf(bounds)

  /**
   * An array containing the longitude and latitude of the southwest and northeast corners of the source's
   * bounding box in the following order: `[sw.lng, sw.lat, ne.lng, ne.lat]`. When this property is included in
   * a source, no tiles outside of the given bounds are requested by Mapbox GL.
   * Default value: [-180,-85.051129,180,85.051129].
   */
  public var bounds: DoubleListValue by boundsState

  @Composable
  private fun UpdateBounds() {
    boundsState.value.apply {
      if (notInitial) {
        setBuilderProperty("bounds", value)
      }
    }
  }
  private val minZoomState: MutableState<LongValue> = mutableStateOf(minZoom)

  /**
   * Minimum zoom level for which tiles are available, as in the TileJSON spec.
   * Default value: 0.
   */
  public var minZoom: LongValue by minZoomState

  @Composable
  private fun UpdateMinZoom() {
    minZoomState.value.apply {
      if (notInitial) {
        setBuilderProperty("minzoom", value)
      }
    }
  }
  private val maxZoomState: MutableState<LongValue> = mutableStateOf(maxZoom)

  /**
   * Maximum zoom level for which tiles are available, as in the TileJSON spec. Data from tiles
   * at the maxzoom are used when displaying the map at higher zoom levels.
   * Default value: 22.
   */
  public var maxZoom: LongValue by maxZoomState

  @Composable
  private fun UpdateMaxZoom() {
    maxZoomState.value.apply {
      if (notInitial) {
        setBuilderProperty("maxzoom", value)
      }
    }
  }
  private val tileSizeState: MutableState<LongValue> = mutableStateOf(tileSize)

  /**
   * The minimum visual size to display tiles for this layer. Only configurable for raster layers.
   * Default value: 512.
   */
  public var tileSize: LongValue by tileSizeState

  @Composable
  private fun UpdateTileSize() {
    tileSizeState.value.apply {
      if (notInitial) {
        setBuilderProperty("tileSize", value)
      }
    }
  }
  private val attributionState: MutableState<StringValue> = mutableStateOf(attribution)

  /**
   * Contains an attribution to be displayed when the map is shown to a user.
   */
  public var attribution: StringValue by attributionState

  @Composable
  private fun UpdateAttribution() {
    attributionState.value.apply {
      if (notInitial) {
        setBuilderProperty("attribution", value)
      }
    }
  }
  private val rasterLayersState: MutableState<RasterLayers> = mutableStateOf(rasterLayers)

  /**
   * Contains the description of the raster data layers and the bands contained within the tiles.
   */
  public var rasterLayers: RasterLayers by rasterLayersState

  @Composable
  private fun UpdateRasterLayers() {
    rasterLayersState.value.apply {
      if (notInitial) {
        setBuilderProperty("rasterLayers", value)
      }
    }
  }
  private val tileCacheBudgetState: MutableState<TileCacheBudget> = mutableStateOf(tileCacheBudget)

  /**
   * This property defines a source-specific resource budget, either in tile units or in megabytes. Whenever the
   * tile cache goes over the defined limit, the least recently used tile will be evicted from
   * the in-memory cache. Note that the current implementation does not take into account resources allocated by
   * the visible tiles.
   */
  public var tileCacheBudget: TileCacheBudget by tileCacheBudgetState

  @Composable
  private fun UpdateTileCacheBudget() {
    tileCacheBudgetState.value.apply {
      if (notInitial) {
        setProperty("tile-cache-budget", value)
      }
    }
  }

  @Composable
  override fun UpdateProperties() {
    UpdateUrl()
    UpdateTiles()
    UpdateBounds()
    UpdateMinZoom()
    UpdateMaxZoom()
    UpdateTileSize()
    UpdateAttribution()
    UpdateRasterLayers()
    UpdateTileCacheBudget()
  }

  private fun getProperties(): Map<String, Value> =
    listOfNotNull(
      ("url" to url.value).takeIf { url.notInitial },
      ("tiles" to tiles.value).takeIf { tiles.notInitial },
      ("bounds" to bounds.value).takeIf { bounds.notInitial },
      ("minzoom" to minZoom.value).takeIf { minZoom.notInitial },
      ("maxzoom" to maxZoom.value).takeIf { maxZoom.notInitial },
      ("tileSize" to tileSize.value).takeIf { tileSize.notInitial },
      ("attribution" to attribution.value).takeIf { attribution.notInitial },
      ("rasterLayers" to rasterLayers.value).takeIf { rasterLayers.notInitial },
      ("tile-cache-budget" to tileCacheBudget.value).takeIf { tileCacheBudget.notInitial },
    ).toMap()

  /**
   * See [Any.equals]
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RasterArraySourceState

    if (sourceId != other.sourceId) return false
    if (url != other.url) return false
    if (tiles != other.tiles) return false
    if (bounds != other.bounds) return false
    if (minZoom != other.minZoom) return false
    if (maxZoom != other.maxZoom) return false
    if (tileSize != other.tileSize) return false
    if (attribution != other.attribution) return false
    if (rasterLayers != other.rasterLayers) return false
    if (tileCacheBudget != other.tileCacheBudget) return false

    return true
  }

  /**
   * See [Any.hashCode]
   */
  override fun hashCode(): Int {
    return Objects.hash(
      sourceId,
      url,
      tiles,
      bounds,
      minZoom,
      maxZoom,
      tileSize,
      attribution,
      rasterLayers,
      tileCacheBudget,
    )
  }

  /**
   * Returns a string representation of the object.
   */
  override fun toString(): String =
    "RasterArraySourceState(sourceId=$sourceId, url=$url, tiles=$tiles, bounds=$bounds, minZoom=$minZoom, maxZoom=$maxZoom, tileSize=$tileSize, attribution=$attribution, rasterLayers=$rasterLayers, tileCacheBudget=$tileCacheBudget)"

  /**
   * Public companion object.
   */
  public companion object {
    /**
     * The default saver implementation for [RasterArraySourceState]
     */
    public val Saver: Saver<RasterArraySourceState, Holder> = Saver(
      save = { it.save() },
      restore = { holder ->
        RasterArraySourceState(
          sourceId = holder.sourcedId,
          sourceType = "raster-array",
          initialProperties = holder.savedProperties,
          url = holder.savedProperties["url"]?.let { StringValue(it.second) } ?: StringValue.INITIAL,
          tiles = holder.savedProperties["tiles"]?.let { StringListValue(it.second) } ?: StringListValue.INITIAL,
          bounds = holder.savedProperties["bounds"]?.let { DoubleListValue(it.second) } ?: DoubleListValue.INITIAL,
          minZoom = holder.savedProperties["minzoom"]?.let { LongValue(it.second) } ?: LongValue.INITIAL,
          maxZoom = holder.savedProperties["maxzoom"]?.let { LongValue(it.second) } ?: LongValue.INITIAL,
          tileSize = holder.savedProperties["tileSize"]?.let { LongValue(it.second) } ?: LongValue.INITIAL,
          attribution = holder.savedProperties["attribution"]?.let { StringValue(it.second) } ?: StringValue.INITIAL,
          rasterLayers = holder.savedProperties["rasterLayers"]?.let { RasterLayers(it.second) } ?: RasterLayers.INITIAL,
          tileCacheBudget = holder.savedProperties["tile-cache-budget"]?.let { TileCacheBudget(it.second) } ?: TileCacheBudget.INITIAL,
        )
      }
    )
  }
}
// End of generated file.