package com.mapbox.maps.plugin.annotation

import android.graphics.PointF
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapboxStyleManager
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.all
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.gt
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.gte
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.has
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.lt
import com.mapbox.maps.extension.style.image.addImage
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.addPersistentLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.logE
import com.mapbox.maps.logW
import com.mapbox.maps.plugin.InvalidPluginConfigurationException
import com.mapbox.maps.plugin.Plugin.Companion.MAPBOX_GESTURES_PLUGIN_ID
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.delegates.MapDelegateProvider
import com.mapbox.maps.plugin.delegates.MapListenerDelegate
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Base class for annotation managers
 */
@MainThread
abstract class AnnotationManagerImpl<G : Geometry, T : Annotation<G>, S : AnnotationOptions<G, T>, D : OnAnnotationDragListener<T>, U : OnAnnotationClickListener<T>, V : OnAnnotationLongClickListener<T>, I : OnAnnotationInteractionListener<T>, L : Layer>
internal constructor(
  /** The delegateProvider */
  final override val delegateProvider: MapDelegateProvider,
  annotationConfig: AnnotationConfig?,
  id: Long,
  typeName: String,
  createLayerFunction: (layerId: String, sourceId: String) -> L
) : AnnotationManager<G, T, S, D, U, V, I> {
  protected val dataDrivenPropertyUsageMap: MutableMap<String, Boolean> = mutableMapOf()
  private val mapCameraManagerDelegate = delegateProvider.mapCameraManagerDelegate
  private val mapFeatureQueryDelegate = delegateProvider.mapFeatureQueryDelegate
  private val mapListenerDelegate: MapListenerDelegate = delegateProvider.mapListenerDelegate
  private var width = 0
  private var height = 0
  private val mapClickResolver = MapClick()
  private val mapLongClickResolver = MapLongClick()
  private val mapMoveResolver = MapMove()
  private var draggingAnnotation: T? = null
  private val annotationMap = LinkedHashMap<String, T>()
  private val dragAnnotationMap = LinkedHashMap<String, T>()

  private var gesturesPlugin: GesturesPlugin = delegateProvider.mapPluginProviderDelegate.getPlugin(
    MAPBOX_GESTURES_PLUGIN_ID
  ) ?: throw InvalidPluginConfigurationException(
    "Can't look up an instance of plugin, is it available on the clazz path and loaded through the map?"
  )

  /** The layer created by this manager. Annotations will be added to this layer.*/
  internal var layer: L
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @JvmSynthetic
    set

  /** The source created by this manager. Feature data will be added to this source.*/
  internal var source: GeoJsonSource
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @JvmSynthetic
    set

  /** The drag layer created by this manager. The dragging annotation will be added to this layer.*/
  internal var dragLayer: L
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @JvmSynthetic
    set

  /** The drag source created by this manager. The feature data of dragging annotation will be added to this source.*/
  internal var dragSource: GeoJsonSource
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @JvmSynthetic
    set

  /**
   * The added annotations
   */
  override val annotations: List<T>
    get() {
      return annotationMap.values.plus(dragAnnotationMap.values)
    }

  /**
   * The added dragListeners
   */
  override val dragListeners: MutableList<D> = mutableListOf()

  /**
   * The Added clickListeners
   */
  override val clickListeners: MutableList<U> = mutableListOf()

  /**
   * The added longClickListeners
   */
  override val longClickListeners: MutableList<V> = mutableListOf()

  /**
   * The added interactionListener
   */
  override val interactionListener: MutableList<I> = mutableListOf()

  init {
    val annotationSourceOptions = annotationConfig?.annotationSourceOptions
    val styleManager = delegateProvider.mapStyleManagerDelegate

    val layerId = annotationConfig?.layerId ?: "mapbox-android-$typeName-layer-$id"
    val sourceId = annotationConfig?.sourceId ?: "mapbox-android-$typeName-source-$id"

    source = createSource(sourceId, annotationSourceOptions)
    layer = createLayerFunction(layerId, sourceId)

    val dragLayerId = "mapbox-android-$typeName-draglayer-$id"
    val dragSourceId = "mapbox-android-$typeName-dragsource-$id"
    dragSource = createDragSource(dragSourceId, annotationSourceOptions)
    dragLayer = createLayerFunction(dragLayerId, dragSourceId)
    if (!styleManager.styleSourceExists(source.sourceId)) {
      styleManager.addSource(source)
    }
    if (!styleManager.styleLayerExists(layer.layerId)) {
      var layerAdded = false
      annotationConfig?.belowLayerId?.let { belowLayerId ->
        // Check whether the below layer exists in the current style.
        if (styleManager.styleLayerExists(belowLayerId)) {
          styleManager.addPersistentLayer(
            layer,
            LayerPosition(null, annotationConfig.belowLayerId, null)
          )
          layerAdded = true
        } else {
          logW(
            TAG,
            "Layer with id $belowLayerId doesn't exist in style ${styleManager.styleURI}, will add annotation layer directly."
          )
        }
      }
      if (!layerAdded) {
        styleManager.addPersistentLayer(layer)
      }
    }
    if (!styleManager.styleSourceExists(dragSource.sourceId)) {
      styleManager.addSource(dragSource)
    }
    if (!styleManager.styleLayerExists(dragLayer.layerId)) {
      // Add drag layer above the annotation layer
      styleManager.addPersistentLayer(
        dragLayer,
        LayerPosition(layer.layerId, null, null)
      )
    }
    if (layer is SymbolLayer || layer is CircleLayer) {
      // Only apply cluster for PointAnnotations or CircleAnnotations
      initClusterLayers(styleManager, annotationSourceOptions)
    }
    updateSource()

    gesturesPlugin.addOnMapClickListener(mapClickResolver)
    gesturesPlugin.addOnMapLongClickListener(mapLongClickResolver)
    gesturesPlugin.addOnMoveListener(mapMoveResolver)
  }

  /**
   * Get the key of the id of the annotation.
   *
   * @return the key of the id of the annotation
   */
  abstract fun getAnnotationIdKey(): String

  /**
   * Set filter on the managed annotations.
   */
  abstract var layerFilter: Expression?

  private fun createSource(
    sourceId: String,
    annotationSourceOptions: AnnotationSourceOptions?
  ) = geoJsonSource(sourceId) {
    annotationSourceOptions?.let { options ->
      options.maxZoom?.let {
        maxzoom(it)
      }
      options.buffer?.let {
        buffer(it)
      }
      options.lineMetrics?.let {
        lineMetrics(it)
      }
      options.tolerance?.let {
        tolerance(it)
      }
      options.clusterOptions?.let { clusterOptions ->
        cluster(clusterOptions.cluster)
        clusterMaxZoom(clusterOptions.clusterMaxZoom)
        clusterRadius(clusterOptions.clusterRadius)
        clusterOptions.clusterProperties?.let {
          clusterProperties(it)
        }
      }
    }
  }

  private fun createDragSource(
    dragSourceId: String,
    annotationSourceOptions: AnnotationSourceOptions?
  ): GeoJsonSource = geoJsonSource(dragSourceId) {
    annotationSourceOptions?.let { options ->
      options.maxZoom?.let {
        maxzoom(it)
      }
      options.buffer?.let {
        buffer(it)
      }
      options.lineMetrics?.let {
        lineMetrics(it)
      }
      options.tolerance?.let {
        tolerance(it)
      }
    }
  }

  private fun initClusterLayers(
    style: MapboxStyleManager,
    annotationSourceOptions: AnnotationSourceOptions?
  ) {
    annotationSourceOptions?.clusterOptions?.let {
      it.colorLevels.forEachIndexed { level, _ ->
        val clusterLevelLayer =
          createClusterLevelLayer(level, it.colorLevels, annotationSourceOptions)
        if (!style.styleLayerExists(clusterLevelLayer.layerId)) {
          style.addPersistentLayer(clusterLevelLayer)
        }
      }
      val clusterTextLayer = createClusterTextLayer(annotationSourceOptions)
      if (!style.styleLayerExists(clusterTextLayer.layerId)) {
        style.addPersistentLayer(clusterTextLayer)
      }
    }
  }

  private fun createClusterLevelLayer(
    level: Int,
    colorLevels: List<Pair<Int, Int>>,
                                      annotationSourceOptions: AnnotationSourceOptions?
  ) =
    circleLayer("mapbox-android-cluster-circle-layer-$level", source.sourceId) {
      circleColor(colorLevels[level].second)
      annotationSourceOptions?.clusterOptions?.let {
        if (it.circleRadiusExpression == null) {
          circleRadius(it.circleRadius)
        } else {
          circleRadius(it.circleRadiusExpression as Expression)
        }
      }
      val pointCount = Expression.toNumber(get(POINT_COUNT))
      filter(
        if (level == 0) all(
          has(POINT_COUNT),
          gte(pointCount, literal(colorLevels[level].first.toLong()))
        ) else all(
          has(POINT_COUNT),
          gt(pointCount, literal(colorLevels[level].first.toLong())),
          lt(pointCount, literal(colorLevels[level - 1].first.toLong()))
        )
      )
    }

  private fun createClusterTextLayer(annotationSourceOptions: AnnotationSourceOptions?) = symbolLayer(CLUSTER_TEXT_LAYER_ID, source.sourceId) {
    annotationSourceOptions?.clusterOptions?.let {
      textField(if (it.textField == null) DEFAULT_TEXT_FIELD else it.textField as Expression)
      if (it.textSizeExpression == null) {
        textSize(it.textSize)
      } else {
        textSize(it.textSizeExpression as Expression)
      }
      if (it.textColorExpression == null) {
        textColor(it.textColor)
      } else {
        textColor(it.textColorExpression as Expression)
      }
      textIgnorePlacement(true)
      textAllowOverlap(true)
    }
  }

  /**
   * Create an annotation with the option
   */
  override fun create(option: S): T {
    return option.build(UUID.randomUUID().toString(), this).also {
      annotationMap[it.id] = it
      updateSource()
    }
  }

  /**
   * Create some annotations with the options
   */
  override fun create(options: List<S>): List<T> {
    val list = options.map { option ->
      option.build(UUID.randomUUID().toString(), this).also {
        annotationMap[it.id] = it
      }
    }
    updateSource()
    return list
  }

  /**
   * Delete the annotation
   */
  override fun delete(annotation: T) {
    if (annotationMap.remove(annotation.id) != null) {
      updateSource()
    } else if (dragAnnotationMap.remove(annotation.id) != null) {
      updateDragSource()
    } else {
      logE(
        TAG,
        "Can't delete annotation: $annotation, the annotation isn't an active annotation."
      )
    }
  }

  /**
   * Delete annotations in the list
   */
  override fun delete(annotations: List<T>) {
    var needUpdateSource = false
    var needUpdateDragSource = false
    annotations.forEach {
      if (annotationMap.remove(it.id) != null) {
        needUpdateSource = true
      } else if (dragAnnotationMap.remove(it.id) != null) {
        needUpdateDragSource = true
      }
    }
    if (needUpdateSource) {
      updateSource()
    }
    if (needUpdateDragSource) {
      updateDragSource()
    }
  }

  /**
   * Delete all the added annotations
   */
  override fun deleteAll() {
    if (annotationMap.isNotEmpty()) {
      annotationMap.clear()
      updateSource()
    }
    if (dragAnnotationMap.isNotEmpty()) {
      dragAnnotationMap.clear()
      updateDragSource()
    }
  }

  private fun updateDragSource() {
    val style = delegateProvider.mapStyleManagerDelegate
    if (!style.styleSourceExists(dragSource.sourceId) || !style.styleLayerExists(dragLayer.layerId)) {
      logE(
        TAG,
        "Can't update dragSource: drag source or layer has not been added to style."
      )
      return
    }
    addIconToStyle(style, dragAnnotationMap.values)
    val features = convertAnnotationsToFeatures(dragAnnotationMap.values)
    dragSource.featureCollection(FeatureCollection.fromFeatures(features))
  }

  /**
   * Trigger an update to the underlying source
   */
  private fun updateSource() {
    val style = delegateProvider.mapStyleManagerDelegate
    if (!style.styleSourceExists(source.sourceId) || !style.styleLayerExists(layer.layerId)) {
      logE(TAG, "Can't update source: source or layer has not been added to style.")
      return
    }
    addIconToStyle(style, annotationMap.values)
    val features = convertAnnotationsToFeatures(annotationMap.values)
    source.featureCollection(FeatureCollection.fromFeatures(features))
  }

  private fun addIconToStyle(style: MapboxStyleManager, annotations: Collection<T>) {
    annotations
      .filter { it.getType() == AnnotationType.PointAnnotation }
      .forEach {
        val symbol = it as PointAnnotation
        symbol.iconImage?.let { image ->
          if (image.startsWith(PointAnnotation.ICON_DEFAULT_NAME_PREFIX)) {
            // User set the bitmap icon, add the icon to style
            symbol.iconImageBitmap?.let { bitmap ->
              val imagePlugin = image(image, bitmap)
              style.addImage(imagePlugin)
            }
          }
        }
      }
  }

  private fun convertAnnotationsToFeatures(annotations: Collection<T>): List<Feature> =
    annotations.map {
      it.setUsedDataDrivenProperties()
      Feature.fromGeometry(it.geometry, it.getJsonObjectCopy(), it.id)
    }

  /**
   * Update the annotation
   */
  override fun update(annotation: T) {
    when {
      annotationMap.containsKey(annotation.id) -> {
        annotationMap[annotation.id] = annotation
        updateSource()
      }

      dragAnnotationMap.containsKey(annotation.id) -> {
        dragAnnotationMap[annotation.id] = annotation
        updateDragSource()
      }

      else -> {
        logE(
          TAG,
          "Can't update annotation: $annotation, the annotation isn't an active annotation."
        )
      }
    }
  }

  /**
   * Update annotations in the list
   */
  override fun update(annotations: List<T>) {
    var needUpdateSource = false
    var needUpdateDragSource = false
    annotations.forEach {
      when {
        annotationMap.containsKey(it.id) -> {
          annotationMap[it.id] = it
          needUpdateSource = true
        }

        dragAnnotationMap.containsKey(it.id) -> {
          dragAnnotationMap[it.id] = it
          needUpdateDragSource = true
        }

        else -> {
          logE(
            TAG,
            "Can't update annotation: $it, the annotation isn't an active annotation."
          )
        }
      }
    }
    if (needUpdateSource) {
      updateSource()
    }
    if (needUpdateDragSource) {
      updateDragSource()
    }
  }

  /**
   * Invoked when Mapview or Annotation manager is destroyed.
   */
  override fun onDestroy() {
    val style = delegateProvider.mapStyleManagerDelegate
    if (style.styleLayerExists(layer.layerId)) {
      style.removeStyleLayer(layer.layerId)
    }
    if (style.styleSourceExists(source.sourceId)) {
      style.removeStyleSource(source.sourceId)
    }
    if (style.styleLayerExists(dragLayer.layerId)) {
      style.removeStyleLayer(dragLayer.layerId)
    }
    if (style.styleSourceExists(dragSource.sourceId)) {
      style.removeStyleSource(dragSource.sourceId)
    }

    gesturesPlugin.removeOnMapClickListener(mapClickResolver)
    gesturesPlugin.removeOnMapLongClickListener(mapLongClickResolver)
    gesturesPlugin.removeOnMoveListener(mapMoveResolver)
    annotationMap.clear()
    dragAnnotationMap.clear()
    dragListeners.clear()
    clickListeners.clear()
    longClickListeners.clear()
    interactionListener.clear()
  }

  /**
   * Toggles the annotation's selection state.
   * If the annotation is deselected, it becomes selected.
   * If the annotation is selected, it becomes deselected.
   * @param annotation: The annotation to select.
   */
  override fun selectAnnotation(annotation: T) {
    when {
      annotationMap.containsKey(annotation.id) -> {
        annotation.isSelected = !annotation.isSelected
        annotationMap[annotation.id] = annotation
        interactionListener.forEach {
          if (annotation.isSelected) {
            it.onSelectAnnotation(annotation)
          } else {
            it.onDeselectAnnotation(annotation)
          }
        }
      }

      dragAnnotationMap.containsKey(annotation.id) -> {
        annotation.isSelected = !annotation.isSelected
        dragAnnotationMap[annotation.id] = annotation
        interactionListener.forEach {
          if (annotation.isSelected) {
            it.onSelectAnnotation(annotation)
          } else {
            it.onDeselectAnnotation(annotation)
          }
        }
      }

      else -> {
        logE(
          TAG,
          "Can't select annotation: $annotation, the annotation isn't an active annotation."
        )
      }
    }
  }

  /**
   * Class handle the map click event
   */
  inner class MapClick : OnMapClickListener {
    /**
     * Called when the user clicks on the map view.
     * Note that calling this method is blocking main thread until querying map for features is finished.
     *
     * @param point The projected map coordinate the user clicked on.
     * @return True if this click should be consumed and not passed further to other listeners registered afterwards,
     * false otherwise.
     */
    override fun onMapClick(point: Point): Boolean {
      queryMapForFeatures(point)?.let {
        clickListeners.forEach { listener ->
          if (listener.onAnnotationClick(it)) {
            return true
          }
        }
        selectAnnotation(it)
      }
      return false
    }
  }

  /**
   * Class handle the map long click event
   */
  inner class MapLongClick : OnMapLongClickListener {
    /**
     * Called when the user long clicks on the map view.
     *
     * @param point The projected map coordinate the user clicked on.
     * @return True if this click should be consumed and not passed further to other listeners registered afterwards,
     * false otherwise.
     */
    override fun onMapLongClick(point: Point): Boolean {
      if (longClickListeners.isEmpty()) {
        return false
      }
      queryMapForFeatures(point)?.let {
        longClickListeners.forEach { listener ->
          if (listener.onAnnotationLongClick(it)) {
            return true
          }
        }
      }
      return false
    }
  }

  /**
   * Class handle the map move event
   */
  inner class MapMove : OnMoveListener {
    /**
     * Called when the move gesture is starting.
     */
    override fun onMoveBegin(detector: MoveGestureDetector) {
      if (detector.pointersCount == 1) {
        queryMapForFeatures(
          ScreenCoordinate(
            detector.focalPoint.x.toDouble(),
            detector.focalPoint.y.toDouble()
          )
        )?.let {
          startDragging(it)
        }
      }
    }

    /**
     * Called when the move gesture is executing.
     */
    override fun onMove(detector: MoveGestureDetector): Boolean {
      // Updating symbol's position
      draggingAnnotation?.let { annotation ->
        if (detector.pointersCount > 1 || !annotation.isDraggable) {
          // Stopping the drag when we don't work with a simple, on-pointer move anymore
          stopDragging()
          return true
        }
        val moveObject = detector.getMoveObject(0)
        val x = moveObject.currentX
        val y = moveObject.currentY
        val pointF = PointF(x, y)
        if (pointF.x < 0 || pointF.y < 0 || pointF.x > width || pointF.y > height) {
          stopDragging()
          return true
        }

        if (annotationMap.containsKey(annotation.id)) {
          // Delete the dragging annotation from original source and add it to drag source
          annotationMap.remove(annotation.id)
          dragAnnotationMap[annotation.id] = annotation
          updateSource()
        }

        annotation.getOffsetGeometry(
          delegateProvider.mapCameraManagerDelegate, moveObject
        )?.let { geometry ->
          annotation.geometry = geometry
          updateDragSource()
          dragListeners.forEach {
            it.onAnnotationDrag(annotation)
          }
          return true
        }

        /* The dragging annotation has been removed from original source,
         update drag source to make sure it is shown in drag layer.
         */
        updateDragSource()
      }
      return false
    }

    /**
     * Called when the move gesture is ending.
     */
    override fun onMoveEnd(detector: MoveGestureDetector) {
      // Stopping the drag when move ends
      stopDragging()
    }

    private fun startDragging(annotation: T): Boolean {
      if (!annotation.isDraggable) {
        return false
      }
      dragListeners.forEach { it.onAnnotationDragStarted(annotation) }
      draggingAnnotation = annotation
      return true
    }

    private fun stopDragging() {
      draggingAnnotation?.let { annotation ->
        dragListeners.forEach { it.onAnnotationDragFinished(annotation) }
        draggingAnnotation = null
      }
    }
  }

  /**
   * Invoked when MapView's width and height have changed.
   * @param width the width of mapView
   * @param height the height of mapView
   */
  override fun onSizeChanged(width: Int, height: Int) {
    this.width = width
    this.height = height
  }

  /**
   * Enable a data-driven property
   */
  override fun enableDataDrivenProperty(property: String) {
    if (dataDrivenPropertyUsageMap[property] == false) {
      dataDrivenPropertyUsageMap[property] = true
      setDataDrivenPropertyIsUsed(property)
    }
  }

  /**
   * Update a data-driven property to used state. Please visit [The online documentation](https://docs.mapbox.com/android/maps/guides/data-driven-styling/) for more details about data-driven-styling
   */
  protected abstract fun setDataDrivenPropertyIsUsed(property: String)

  /**
   * Query the rendered annotation around the point
   *
   * @param point the point for querying
   * @return the queried annotation at this point
   */
  fun queryMapForFeatures(point: Point): T? {
    val screenCoordinate = mapCameraManagerDelegate.pixelForCoordinate(point)
    return queryMapForFeatures(screenCoordinate)
  }

  /**
   * Query the rendered annotation around the point
   *
   * @param screenCoordinate the screenCoordinate for querying
   * @return the queried annotation on this screenCoordinate
   */
  fun queryMapForFeatures(screenCoordinate: ScreenCoordinate): T? {
    var annotation: T? = null
    val layerList = mutableListOf<String>()
    layerList.add(layer.layerId)
    layerList.add(dragLayer.layerId)
    val latch = CountDownLatch(1)
    mapFeatureQueryDelegate.executeOnRenderThread {
      mapFeatureQueryDelegate.queryRenderedFeatures(
        RenderedQueryGeometry(screenCoordinate),
        RenderedQueryOptions(
          layerList,
          literal(true)
        )
      ) { features ->
        features.value?.firstOrNull()?.queriedFeature?.feature?.getProperty(getAnnotationIdKey())
          ?.let { annotationId ->
            val id = annotationId.asString
            when {
              annotationMap.containsKey(id) -> {
                annotation = annotationMap[id]
              }

              dragAnnotationMap.containsKey(id) -> {
                annotation = dragAnnotationMap[id]
              }

              else -> {
                logE(
                  TAG,
                  "The queried id: $id, doesn't belong to an active annotation."
                )
              }
            }
          }
        latch.countDown()
      }
    }
    latch.await(QUERY_WAIT_TIME, TimeUnit.SECONDS)
    return annotation
  }

  protected fun setLayerProperty(value: Value, propertyName: String) {
    try {
      with(delegateProvider.mapStyleManagerDelegate) {
        setStyleLayerProperty(layer.layerId, propertyName, value)
        setStyleLayerProperty(dragLayer.layerId, propertyName, value)
      }
    } catch (e: IllegalArgumentException) {
      throw IllegalArgumentException(
        "Incorrect property value for $propertyName: ${e.message}",
        e.cause
      )
    }
  }

  /**
   * Static variables and methods.
   */
  private companion object {
    /**
     * Tag for log
     */
    private const val TAG = "AnnotationManagerImpl"
    private const val POINT_COUNT = "point_count"

    /** At most wait 2 seconds to prevent ANR */
    private const val QUERY_WAIT_TIME = 2L
    private const val CLUSTER_TEXT_LAYER_ID = "mapbox-android-cluster-text-layer"
    private val DEFAULT_TEXT_FIELD = get("point_count")
  }
}