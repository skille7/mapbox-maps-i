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
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.IdGenerator.generateRandomSourceId
import com.mapbox.maps.extension.compose.style.LongValue
import com.mapbox.maps.extension.compose.style.StringValue
import com.mapbox.maps.extension.compose.style.sources.ClusterProperties
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.PromoteIdValue
import com.mapbox.maps.extension.compose.style.sources.SourceState
import com.mapbox.maps.extension.compose.style.sources.TileCacheBudget
import java.util.Objects

/**
 * Create and [rememberSaveable] a [GeoJsonSourceState] using [GeoJsonSourceState.Saver].
 * [init] will be called when the [GeoJsonSourceState] is first created to configure its
 * initial state.
 *
 * Note: Saving large inlined GeoJsonData has performance impact, consider using [remember] or other way to retain the data.
 *
 * @param key An optional key to be used as a key for the saved value. If not provided we use the
 * automatically generated by the Compose runtime which is unique for the every exact code location
 * in the composition tree.
 * @param sourceId The optional sourceId for the source state, by default, a random source ID will be used.
 * @param init A function initialise this [GeoJsonSourceState].
 */
@Composable
@MapboxExperimental
public inline fun rememberGeoJsonSourceState(
  key: String? = null,
  sourceId: String = remember {
    generateRandomSourceId("geojson")
  },
  crossinline init: GeoJsonSourceState.() -> Unit = {}
): GeoJsonSourceState = rememberSaveable(key = key, saver = GeoJsonSourceState.Saver) {
  GeoJsonSourceState(sourceId).apply(init)
}

/**
 * A GeoJSON data source.
 *
 * @see [The online documentation](https://docs.mapbox.com/style-spec/reference/sources#geojson)
 *
 * @param sourceId The id of the source state, by default a random generated ID will be used.
 * @param initialProperties The initial mutable properties of the source.
 * @param initialData The initial [GeoJSONData] of the source.
 */
@MapboxExperimental
public class GeoJsonSourceState private constructor(
  sourceId: String,
  sourceType: String,
  initialProperties: Map<String, Pair<Boolean, Value>>,
  initialData: GeoJSONData = GeoJSONData.DEFAULT,
  maxZoom: LongValue,
  attribution: StringValue,
  buffer: LongValue,
  tolerance: DoubleValue,
  cluster: BooleanValue,
  clusterRadius: LongValue,
  clusterMaxZoom: LongValue,
  clusterMinPoints: LongValue,
  clusterProperties: ClusterProperties,
  lineMetrics: BooleanValue,
  generateId: BooleanValue,
  promoteId: PromoteIdValue,
  prefetchZoomDelta: LongValue,
  tileCacheBudget: TileCacheBudget,
) : SourceState(
  sourceId = sourceId,
  sourceType = sourceType,
  initialProperties = initialProperties,
  initialGeoJsonData = initialData,
) {
  public constructor(
    sourceId: String = generateRandomSourceId("geojson"),
  ) : this(
    sourceId = sourceId,
    sourceType = "geojson",
    initialProperties = emptyMap(),
    maxZoom = LongValue.INITIAL,
    attribution = StringValue.INITIAL,
    buffer = LongValue.INITIAL,
    tolerance = DoubleValue.INITIAL,
    cluster = BooleanValue.INITIAL,
    clusterRadius = LongValue.INITIAL,
    clusterMaxZoom = LongValue.INITIAL,
    clusterMinPoints = LongValue.INITIAL,
    clusterProperties = ClusterProperties.INITIAL,
    lineMetrics = BooleanValue.INITIAL,
    generateId = BooleanValue.INITIAL,
    promoteId = PromoteIdValue.INITIAL,
    prefetchZoomDelta = LongValue.INITIAL,
    tileCacheBudget = TileCacheBudget.INITIAL,
  )

  /**
   * Sets GeoJson `data` property as [GeoJSONData].
   *
   * The data will be scheduled and applied on a worker thread and no validation happens synchronously.
   * If [data] is invalid - `MapLoadingError` with `type = metadata` will be invoked.
   */
  public var data: GeoJSONData
    get() = geoJSONData
    set(value) {
      geoJSONData = value
    }

  private val maxZoomState: MutableState<LongValue> = mutableStateOf(maxZoom)

  /**
   * Maximum zoom level at which to create vector tiles (higher means greater detail at high zoom
   * levels).
   * Default value: 18.
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
  private val bufferState: MutableState<LongValue> = mutableStateOf(buffer)

  /**
   * Size of the tile buffer on each side. A value of 0 produces no buffer. A
   * value of 512 produces a buffer as wide as the tile itself. Larger values produce fewer
   * rendering artifacts near tile edges and slower performance.
   * Default value: 128. Value range: [0, 512]
   */
  public var buffer: LongValue by bufferState

  @Composable
  private fun UpdateBuffer() {
    bufferState.value.apply {
      if (notInitial) {
        setBuilderProperty("buffer", value)
      }
    }
  }
  private val toleranceState: MutableState<DoubleValue> = mutableStateOf(tolerance)

  /**
   * Douglas-Peucker simplification tolerance (higher means simpler geometries and faster performance).
   * Default value: 0.375.
   */
  public var tolerance: DoubleValue by toleranceState

  @Composable
  private fun UpdateTolerance() {
    toleranceState.value.apply {
      if (notInitial) {
        setBuilderProperty("tolerance", value)
      }
    }
  }
  private val clusterState: MutableState<BooleanValue> = mutableStateOf(cluster)

  /**
   * If the data is a collection of point features, setting this to true clusters the points
   * by radius into groups. Cluster groups become new `Point` features in the source with additional properties:
   *  - `cluster` Is `true` if the point is a cluster
   *  - `cluster_id` A unqiue id for the cluster to be used in conjunction with the
   * [cluster inspection methods](https://www.mapbox.com/mapbox-gl-js/api/#geojsonsource#getclusterexpansionzoom)
   *  - `point_count` Number of original points grouped into this cluster
   *  - `point_count_abbreviated` An abbreviated point count
   * Default value: false.
   */
  public var cluster: BooleanValue by clusterState

  @Composable
  private fun UpdateCluster() {
    clusterState.value.apply {
      if (notInitial) {
        setBuilderProperty("cluster", value)
      }
    }
  }
  private val clusterRadiusState: MutableState<LongValue> = mutableStateOf(clusterRadius)

  /**
   * Radius of each cluster if clustering is enabled. A value of 512 indicates a radius equal
   * to the width of a tile.
   * Default value: 50. Minimum value: 0.
   */
  public var clusterRadius: LongValue by clusterRadiusState

  @Composable
  private fun UpdateClusterRadius() {
    clusterRadiusState.value.apply {
      if (notInitial) {
        setBuilderProperty("clusterRadius", value)
      }
    }
  }
  private val clusterMaxZoomState: MutableState<LongValue> = mutableStateOf(clusterMaxZoom)

  /**
   * Max zoom on which to cluster points if clustering is enabled. Defaults to one zoom less
   * than maxzoom (so that last zoom features are not clustered). Clusters are re-evaluated at integer zoom
   * levels so setting clusterMaxZoom to 14 means the clusters will be displayed until z15.
   */
  public var clusterMaxZoom: LongValue by clusterMaxZoomState

  @Composable
  private fun UpdateClusterMaxZoom() {
    clusterMaxZoomState.value.apply {
      if (notInitial) {
        setBuilderProperty("clusterMaxZoom", value)
      }
    }
  }
  private val clusterMinPointsState: MutableState<LongValue> = mutableStateOf(clusterMinPoints)

  /**
   * Minimum number of points necessary to form a cluster if clustering is enabled. Defaults to `2`.
   * Default value: 2.
   */
  public var clusterMinPoints: LongValue by clusterMinPointsState

  @Composable
  private fun UpdateClusterMinPoints() {
    clusterMinPointsState.value.apply {
      if (notInitial) {
        setBuilderProperty("clusterMinPoints", value)
      }
    }
  }
  private val clusterPropertiesState: MutableState<ClusterProperties> = mutableStateOf(clusterProperties)

  /**
   * An object defining custom properties on the generated clusters if clustering is enabled, aggregating values from
   * clustered points. Has the form `{"property_name": [operator, map_expression]}`. `operator` is any expression function that accepts at
   * least 2 operands (e.g. `"+"` or `"max"`) — it accumulates the property value from clusters/points the
   * cluster contains; `map_expression` produces the value of a single point.
   *
   * Example: `{"sum": ["+", ["get", "scalerank"]]}`.
   *
   * For more advanced use cases, in place of `operator`, you can use a custom reduce expression
   * that references a special `["accumulated"]` value, e.g.:
   * `{"sum": [["+", ["accumulated"], ["get", "sum"]], ["get", "scalerank"]]}`
   */
  public var clusterProperties: ClusterProperties by clusterPropertiesState

  @Composable
  private fun UpdateClusterProperties() {
    clusterPropertiesState.value.apply {
      if (notInitial) {
        setBuilderProperty("clusterProperties", value)
      }
    }
  }
  private val lineMetricsState: MutableState<BooleanValue> = mutableStateOf(lineMetrics)

  /**
   * Whether to calculate line distance metrics. This is required for line layers that specify `line-gradient` values.
   * Default value: false.
   */
  public var lineMetrics: BooleanValue by lineMetricsState

  @Composable
  private fun UpdateLineMetrics() {
    lineMetricsState.value.apply {
      if (notInitial) {
        setBuilderProperty("lineMetrics", value)
      }
    }
  }
  private val generateIdState: MutableState<BooleanValue> = mutableStateOf(generateId)

  /**
   * Whether to generate ids for the geojson features. When enabled, the `feature.id` property will be auto
   * assigned based on its index in the `features` array, over-writing any previous values.
   * Default value: false.
   */
  public var generateId: BooleanValue by generateIdState

  @Composable
  private fun UpdateGenerateId() {
    generateIdState.value.apply {
      if (notInitial) {
        setBuilderProperty("generateId", value)
      }
    }
  }
  private val promoteIdState: MutableState<PromoteIdValue> = mutableStateOf(promoteId)

  /**
   * A property to use as a feature id (for feature state). Either a property name, or
   * an object of the form `{<sourceLayer>: <propertyName>}`.
   */
  public var promoteId: PromoteIdValue by promoteIdState

  @Composable
  private fun UpdatePromoteId() {
    promoteIdState.value.apply {
      if (notInitial) {
        setBuilderProperty("promoteId", value)
      }
    }
  }
  private val prefetchZoomDeltaState: MutableState<LongValue> = mutableStateOf(prefetchZoomDelta)

  /**
   * When loading a map, if PrefetchZoomDelta is set to any number greater than 0, the map
   * will first request a tile at zoom level lower than zoom - delta, but so that
   * the zoom level is multiple of delta, in an attempt to display a full map at
   * lower resolution as quick as possible. It will get clamped at the tile source minimum zoom.
   * Default value: 4.
   */
  public var prefetchZoomDelta: LongValue by prefetchZoomDeltaState

  @Composable
  private fun UpdatePrefetchZoomDelta() {
    prefetchZoomDeltaState.value.apply {
      if (notInitial) {
        setProperty("prefetch-zoom-delta", value)
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
    UpdateMaxZoom()
    UpdateAttribution()
    UpdateBuffer()
    UpdateTolerance()
    UpdateCluster()
    UpdateClusterRadius()
    UpdateClusterMaxZoom()
    UpdateClusterMinPoints()
    UpdateClusterProperties()
    UpdateLineMetrics()
    UpdateGenerateId()
    UpdatePromoteId()
    UpdatePrefetchZoomDelta()
    UpdateTileCacheBudget()
  }

  private fun getProperties(): Map<String, Value> =
    listOfNotNull(
      ("maxzoom" to maxZoom.value).takeIf { maxZoom.notInitial },
      ("attribution" to attribution.value).takeIf { attribution.notInitial },
      ("buffer" to buffer.value).takeIf { buffer.notInitial },
      ("tolerance" to tolerance.value).takeIf { tolerance.notInitial },
      ("cluster" to cluster.value).takeIf { cluster.notInitial },
      ("clusterRadius" to clusterRadius.value).takeIf { clusterRadius.notInitial },
      ("clusterMaxZoom" to clusterMaxZoom.value).takeIf { clusterMaxZoom.notInitial },
      ("clusterMinPoints" to clusterMinPoints.value).takeIf { clusterMinPoints.notInitial },
      ("clusterProperties" to clusterProperties.value).takeIf { clusterProperties.notInitial },
      ("lineMetrics" to lineMetrics.value).takeIf { lineMetrics.notInitial },
      ("generateId" to generateId.value).takeIf { generateId.notInitial },
      ("promoteId" to promoteId.value).takeIf { promoteId.notInitial },
      ("prefetch-zoom-delta" to prefetchZoomDelta.value).takeIf { prefetchZoomDelta.notInitial },
      ("tile-cache-budget" to tileCacheBudget.value).takeIf { tileCacheBudget.notInitial },
    ).toMap()

  /**
   * See [Any.equals]
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GeoJsonSourceState

    if (sourceId != other.sourceId) return false
    if (data != other.data) return false
    if (maxZoom != other.maxZoom) return false
    if (attribution != other.attribution) return false
    if (buffer != other.buffer) return false
    if (tolerance != other.tolerance) return false
    if (cluster != other.cluster) return false
    if (clusterRadius != other.clusterRadius) return false
    if (clusterMaxZoom != other.clusterMaxZoom) return false
    if (clusterMinPoints != other.clusterMinPoints) return false
    if (clusterProperties != other.clusterProperties) return false
    if (lineMetrics != other.lineMetrics) return false
    if (generateId != other.generateId) return false
    if (promoteId != other.promoteId) return false
    if (prefetchZoomDelta != other.prefetchZoomDelta) return false
    if (tileCacheBudget != other.tileCacheBudget) return false

    return true
  }

  /**
   * See [Any.hashCode]
   */
  override fun hashCode(): Int {
    return Objects.hash(
      sourceId,
      data,
      maxZoom,
      attribution,
      buffer,
      tolerance,
      cluster,
      clusterRadius,
      clusterMaxZoom,
      clusterMinPoints,
      clusterProperties,
      lineMetrics,
      generateId,
      promoteId,
      prefetchZoomDelta,
      tileCacheBudget,
    )
  }

  /**
   * Returns a string representation of the object.
   */
  override fun toString(): String =
    "GeoJsonSourceState(sourceId=$sourceId,  data=$data, maxZoom=$maxZoom, attribution=$attribution, buffer=$buffer, tolerance=$tolerance, cluster=$cluster, clusterRadius=$clusterRadius, clusterMaxZoom=$clusterMaxZoom, clusterMinPoints=$clusterMinPoints, clusterProperties=$clusterProperties, lineMetrics=$lineMetrics, generateId=$generateId, promoteId=$promoteId, prefetchZoomDelta=$prefetchZoomDelta, tileCacheBudget=$tileCacheBudget)"

  /**
   * Public companion object.
   */
  public companion object {
    /**
     * The default saver implementation for [GeoJsonSourceState]
     */
    public val Saver: Saver<GeoJsonSourceState, Holder> = Saver(
      save = { it.save() },
      restore = { holder ->
        GeoJsonSourceState(
          sourceId = holder.sourcedId,
          sourceType = "geojson",
          initialProperties = holder.savedProperties,
          initialData = holder.geoJSONData,
          maxZoom = holder.savedProperties["maxzoom"]?.let { LongValue(it.second) } ?: LongValue.INITIAL,
          attribution = holder.savedProperties["attribution"]?.let { StringValue(it.second) } ?: StringValue.INITIAL,
          buffer = holder.savedProperties["buffer"]?.let { LongValue(it.second) } ?: LongValue.INITIAL,
          tolerance = holder.savedProperties["tolerance"]?.let { DoubleValue(it.second) } ?: DoubleValue.INITIAL,
          cluster = holder.savedProperties["cluster"]?.let { BooleanValue(it.second) } ?: BooleanValue.INITIAL,
          clusterRadius = holder.savedProperties["clusterRadius"]?.let { LongValue(it.second) } ?: LongValue.INITIAL,
          clusterMaxZoom = holder.savedProperties["clusterMaxZoom"]?.let { LongValue(it.second) } ?: LongValue.INITIAL,
          clusterMinPoints = holder.savedProperties["clusterMinPoints"]?.let { LongValue(it.second) } ?: LongValue.INITIAL,
          clusterProperties = holder.savedProperties["clusterProperties"]?.let { ClusterProperties(it.second) } ?: ClusterProperties.INITIAL,
          lineMetrics = holder.savedProperties["lineMetrics"]?.let { BooleanValue(it.second) } ?: BooleanValue.INITIAL,
          generateId = holder.savedProperties["generateId"]?.let { BooleanValue(it.second) } ?: BooleanValue.INITIAL,
          promoteId = holder.savedProperties["promoteId"]?.let { PromoteIdValue(it.second) } ?: PromoteIdValue.INITIAL,
          prefetchZoomDelta = holder.savedProperties["prefetch-zoom-delta"]?.let { LongValue(it.second) } ?: LongValue.INITIAL,
          tileCacheBudget = holder.savedProperties["tile-cache-budget"]?.let { TileCacheBudget(it.second) } ?: TileCacheBudget.INITIAL,
        )
      }
    )
  }
}
// End of generated file.