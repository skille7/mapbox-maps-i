package com.mapbox.maps

import android.view.Choreographer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.*
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import com.mapbox.bindgen.Expected
import com.mapbox.maps.viewannotation.*
import com.mapbox.maps.viewannotation.ViewAnnotation
import com.mapbox.maps.viewannotation.ViewAnnotation.Companion.USER_FIXED_DIMENSION
import com.mapbox.maps.viewannotation.ViewAnnotationVisibility
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.collections.HashMap

internal class ViewAnnotationManagerImpl(
  private val mapView: MapView
) : ViewAnnotationManager, ViewAnnotationPositionsUpdateListener {

  private val mapboxMap: MapboxMap = mapView.getMapboxMap()
  private val viewPlugins = mapView.mapController.pluginRegistry.viewPlugins
  private val renderThread = mapView.mapController.renderer.renderThread

  init {
    mapView.requestDisallowInterceptTouchEvent(false)
    mapboxMap.setViewAnnotationPositionsUpdateListener(this)
  }

  private val annotationMap = ConcurrentHashMap<String, ViewAnnotation>()
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal val idLookupMap = ConcurrentHashMap<View, String>()
  private val hiddenViewMap = ConcurrentHashMap<View, Float>()

  // struct needed for drawing, declare it only once
  private val currentViewsDrawnMap = HashMap<String, ScreenCoordinate>()

  // using copy on write as user could remove listener while callback is invoked
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal val viewUpdatedListenerSet = CopyOnWriteArraySet<OnViewAnnotationUpdatedListener>()

  @Volatile
  private var updatedPositionsList: MutableList<ViewAnnotationPositionDescriptor> = mutableListOf()

  override fun addViewAnnotation(
    @LayoutRes resId: Int,
    options: ViewAnnotationOptions,
    asyncInflater: AsyncLayoutInflater,
    asyncInflateCallback: (View) -> Unit
  ) {
    validateOptions(options)
    asyncInflater.inflate(resId, mapView) { view, _, _ ->
      asyncInflateCallback.invoke(prepareViewAnnotation(view, options))
    }
  }

  override fun addViewAnnotation(
    @LayoutRes resId: Int,
    options: ViewAnnotationOptions
  ): View {
    validateOptions(options)
    val view = LayoutInflater.from(mapView.context).inflate(resId, mapView, false)
    return prepareViewAnnotation(view, options)
  }

  override fun addViewAnnotation(view: View, options: ViewAnnotationOptions) {
    if (idLookupMap.containsKey(view)) {
      throw MapboxViewAnnotationException(
        "Trying to add view annotation that was already added before! " +
          "Please consider deleting annotation view ($view) beforehand."
      )
    }
    validateOptions(options)
    prepareViewAnnotation(view, options)
  }

  override fun removeViewAnnotation(view: View): Boolean {
    val id = idLookupMap.remove(view) ?: return false
    val annotation = annotationMap.remove(id) ?: return false
    remove(id, annotation)
    return true
  }

  override fun removeAllViewAnnotations() {
    annotationMap.forEach { (id, annotation) ->
      remove(id, annotation)
    }
    currentViewsDrawnMap.clear()
    annotationMap.clear()
    idLookupMap.clear()
  }

  override fun updateViewAnnotation(
    view: View,
    options: ViewAnnotationOptions,
  ): Boolean {
    val id = idLookupMap[view] ?: return false
    checkAssociatedFeatureIdUniqueness(options)
    annotationMap[id]?.let {
      it.handleVisibilityAutomatically = (options.visible == null)
      if (options.width != null) {
        it.measuredWidth = USER_FIXED_DIMENSION
      }
      if (options.height != null) {
        it.measuredHeight = USER_FIXED_DIMENSION
      }
      getValue(mapboxMap.updateViewAnnotation(id, options))
      return true
    } ?: return false
  }

  override fun getViewAnnotationByFeatureId(featureId: String): View? {
    val (view, _) = findByFeatureId(featureId)
    return view
  }

  override fun getViewAnnotationOptionsByFeatureId(featureId: String): ViewAnnotationOptions? {
    val (_, options) = findByFeatureId(featureId)
    return options
  }

  override fun getViewAnnotationOptionsByView(view: View): ViewAnnotationOptions? {
    val id = idLookupMap[view] ?: return null
    return getValue(mapboxMap.getViewAnnotationOptions(id))
  }

  override fun addOnViewAnnotationUpdatedListener(listener: OnViewAnnotationUpdatedListener) {
    viewUpdatedListenerSet.add(listener)
  }

  override fun removeOnViewAnnotationUpdatedListener(listener: OnViewAnnotationUpdatedListener) {
    viewUpdatedListenerSet.remove(listener)
  }

  @AnyThread
  override fun setViewAnnotationUpdateMode(mode: ViewAnnotationUpdateMode) {
    renderThread.viewAnnotationMode = mode
  }

  /**
   * Always called from render thread in the end of [MapInterface.render] call if positions did change.
   *
   * It's crucial to notify render thread in this callback as depending on mode we're using we need
   * either swap buffers the same or the next frame.
   */
  @WorkerThread
  override fun onViewAnnotationPositionsUpdate(positions: MutableList<ViewAnnotationPositionDescriptor>) {
    logE("KIRYLDD", "onViewAnnotationPositionsUpdate ${positions.joinToString(", ")}")
    // update that flag here if callback was triggered, it will be reset by renderer directly when swapping buffers
    renderThread.hasViewAnnotations = true
    updatedPositionsList = positions
  }

  /**
   * Called as soon as possible on main thread after updated positions arrived on render thread.
   *
   * We need another callback from core as scheduling from render thread to main happens too slow
   * when using Java Main Looper.
   */
  @MainThread
  override fun onViewAnnotationPositionsUpdateMainThread() {
    val immutablePositionListCopy = LinkedList(updatedPositionsList)
    logE("KIRYLDD", "onViewAnnotationPositionsUpdateMainThread ${immutablePositionListCopy.joinToString(", ")}")
    immutablePositionListCopy.forEach { descriptor ->
      annotationMap[descriptor.identifier]?.let { annotation ->
        logE("KIRYLDD", "Translation post to next frame, mode = ${renderThread.viewAnnotationMode.name}")
        Choreographer.getInstance().postFrameCallback {
          annotation.view.translationX = descriptor.leftTopCoordinate.x.toFloat()
          annotation.view.translationY = descriptor.leftTopCoordinate.y.toFloat()
          logE("KIRYLDD", "Translation upd time=$it: x=${annotation.view.translationX}, y=${annotation.view.translationY}")
        }
      } ?: logE(TAG, "Core calculated position for ${descriptor.identifier} but actual view was not added!")
    }
    // adding, removing, changing visibility for Android views should be done from Main UI thread only.
    positionAnnotationViews(immutablePositionListCopy)
  }

  fun destroy() {
    mapboxMap.setViewAnnotationPositionsUpdateListener(null)
    viewUpdatedListenerSet.clear()
    removeAllViewAnnotations()
  }

  private fun validateOptions(options: ViewAnnotationOptions) {
    if (options.geometry == null) {
      throw IllegalArgumentException(EXCEPTION_TEXT_GEOMETRY_IS_NULL)
    }
  }

  private fun checkAssociatedFeatureIdUniqueness(options: ViewAnnotationOptions) {
    options.associatedFeatureId?.let { associatedFeatureId ->
      val (view, _) = findByFeatureId(associatedFeatureId)
      if (view != null) {
        throw MapboxViewAnnotationException(
          String.format(
            EXCEPTION_TEXT_ASSOCIATED_FEATURE_ID_ALREADY_EXISTS,
            associatedFeatureId
          )
        )
      }
    }
  }

  private fun prepareViewAnnotation(inflatedView: View, options: ViewAnnotationOptions): View {
    checkAssociatedFeatureIdUniqueness(options)
    val inflatedViewLayout = inflatedView.layoutParams as FrameLayout.LayoutParams
    val updatedOptions = options.toBuilder()
      .width(options.width ?: inflatedViewLayout.width)
      .height(options.height ?: inflatedViewLayout.height)
      .build()
    val viewAnnotation = ViewAnnotation(
      view = inflatedView,
      handleVisibilityAutomatically = (options.visible == null),
      visibility = ViewAnnotationVisibility.INITIAL,
      viewLayoutParams = inflatedViewLayout,
      measuredWidth = if (options.width != null) USER_FIXED_DIMENSION else inflatedViewLayout.width,
      measuredHeight = if (options.height != null) USER_FIXED_DIMENSION else inflatedViewLayout.height,
    )
    val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
      if (viewAnnotation.measuredWidth != USER_FIXED_DIMENSION &&
        inflatedView.measuredWidth > 0 &&
        inflatedView.measuredWidth != viewAnnotation.measuredWidth
      ) {
        viewAnnotation.measuredWidth = inflatedView.measuredWidth
        getValue(
          mapboxMap.updateViewAnnotation(
            viewAnnotation.id,
            ViewAnnotationOptions.Builder()
              .width(inflatedView.measuredWidth)
              .build()
          )
        )
      }
      if (viewAnnotation.measuredHeight != USER_FIXED_DIMENSION &&
        inflatedView.measuredHeight > 0 &&
        inflatedView.measuredHeight != viewAnnotation.measuredHeight
      ) {
        viewAnnotation.measuredHeight = inflatedView.measuredHeight
        getValue(
          mapboxMap.updateViewAnnotation(
            viewAnnotation.id,
            ViewAnnotationOptions.Builder()
              .height(inflatedView.measuredHeight)
              .build()
          )
        )
      }
      if (viewAnnotation.handleVisibilityAutomatically) {
        val isAndroidViewVisible = (inflatedView.visibility == View.VISIBLE)
        if ((isAndroidViewVisible && viewAnnotation.isVisible) ||
          (!isAndroidViewVisible && viewAnnotation.visibility == ViewAnnotationVisibility.INVISIBLE)
        ) {
          return@OnGlobalLayoutListener
        }
        // hide view below map surface and pull it back when new position from core will arrive
        if (isAndroidViewVisible) {
          hiddenViewMap[inflatedView] = inflatedView.translationZ
          inflatedView.translationZ = mapView.translationZ - 1f
        }
        updateVisibilityAndNotifyUpdateListeners(
          viewAnnotation,
          if (isAndroidViewVisible)
            ViewAnnotationVisibility.VISIBLE_AND_NOT_POSITIONED
          else
            ViewAnnotationVisibility.INVISIBLE
        )
        if (getValue(mapboxMap.getViewAnnotationOptions(viewAnnotation.id))?.visible != isAndroidViewVisible) {
          getValue(
            mapboxMap.updateViewAnnotation(
              viewAnnotation.id,
              ViewAnnotationOptions.Builder()
                .visible(isAndroidViewVisible)
                .build()
            )
          )
        }
      }
    }
    viewAnnotation.attachStateListener = object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View?) {
        inflatedView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
      }

      override fun onViewDetachedFromWindow(v: View?) {
        inflatedView.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
      }
    }
    inflatedView.addOnAttachStateChangeListener(viewAnnotation.attachStateListener)
    annotationMap[viewAnnotation.id] = viewAnnotation
    idLookupMap[inflatedView] = viewAnnotation.id
    getValue(mapboxMap.addViewAnnotation(viewAnnotation.id, updatedOptions))
    return inflatedView
  }

  private fun findByFeatureId(featureId: String): Pair<View?, ViewAnnotationOptions?> {
    annotationMap.forEach { (id, annotation) ->
      getValue(mapboxMap.getViewAnnotationOptions(id))?.let { options ->
        if (options.associatedFeatureId == featureId) {
          return annotation.view to options
        }
      }
    }
    return null to null
  }

  private fun positionAnnotationViews(
    positionDescriptorCoreList: List<ViewAnnotationPositionDescriptor>
  ) {
    // firstly delete views that do not belong to the viewport
    currentViewsDrawnMap.keys.forEach { id ->
      if (positionDescriptorCoreList.indexOfFirst { it.identifier == id } == -1) {
        annotationMap[id]?.let { annotation ->
          // if view is invisible / gone we don't remove it so that visibility logic could
          // still be handled by OnGlobalLayoutListener
          if (annotation.view.visibility == View.VISIBLE) {
            mapView.removeView(annotation.view)
            updateVisibilityAndNotifyUpdateListeners(annotation, ViewAnnotationVisibility.INVISIBLE)
          }
        }
      }
    }
    // add and reposition new and existed views
    positionDescriptorCoreList.forEach { descriptor ->
      annotationMap[descriptor.identifier]?.let { annotation ->
        // update layout params explicitly if user has specified concrete width or height
        annotation.viewLayoutParams.apply {
          if (annotation.measuredWidth == USER_FIXED_DIMENSION) {
            width = descriptor.width
          }
          if (annotation.measuredHeight == USER_FIXED_DIMENSION) {
            height = descriptor.height
          }
        }
        if (!currentViewsDrawnMap.keys.contains(descriptor.identifier) && mapView.indexOfChild(annotation.view) == -1) {
          mapView.addView(annotation.view, annotation.viewLayoutParams)
          updateVisibilityAndNotifyUpdateListeners(
            annotation,
            if (annotation.view.visibility == View.VISIBLE)
              ViewAnnotationVisibility.VISIBLE_AND_POSITIONED
            else
              ViewAnnotationVisibility.INVISIBLE
          )
        }
        if (viewUpdatedListenerSet.isNotEmpty()) {
          viewUpdatedListenerSet.forEach {
            // when using wrap_content dimensions width and height could report -2
            // it makes sense to notify user only when width and height are calculated
            if (descriptor.width > 0 && descriptor.height > 0) {
              it.onViewAnnotationPositionUpdated(
                view = annotation.view,
                leftTopCoordinate = descriptor.leftTopCoordinate,
                width = descriptor.width,
                height = descriptor.height,
              )
            }
          }
        }
        hiddenViewMap[annotation.view]?.let { zIndex ->
          annotation.view.translationZ = zIndex
          hiddenViewMap.remove(annotation.view)
          updateVisibilityAndNotifyUpdateListeners(annotation, ViewAnnotationVisibility.VISIBLE_AND_POSITIONED)
        }
        // as we preserve correct order we bring each view to the front and correct order will be preserved
        annotation.view.bringToFront()
      }
    }
    // bring to front map view plugins so that they are drawn on top of view annotations
    viewPlugins.forEach {
      it.value.bringToFront()
    }
    currentViewsDrawnMap.clear()
    positionDescriptorCoreList.forEach {
      currentViewsDrawnMap[it.identifier] = it.leftTopCoordinate
    }
  }

  private inline fun <reified V> getValue(expected: Expected<String, V>): V? {
    if (expected.isError) {
      throw MapboxViewAnnotationException(expected.error)
    }
    return expected.value
  }

  private fun updateVisibilityAndNotifyUpdateListeners(
    annotation: ViewAnnotation,
    currentVisibility: ViewAnnotationVisibility
  ) {
    // we do nothing if currentVisibility value did not change and additionally if view is just added but not shown
    if (annotation.visibility == currentVisibility ||
      (annotation.visibility == ViewAnnotationVisibility.INITIAL && currentVisibility == ViewAnnotationVisibility.INVISIBLE)
    ) {
      return
    }
    val wasVisibleBefore = annotation.isVisible
    val isVisibleNow = (
      currentVisibility == ViewAnnotationVisibility.VISIBLE_AND_POSITIONED ||
        currentVisibility == ViewAnnotationVisibility.VISIBLE_AND_NOT_POSITIONED
      )
    annotation.visibility = currentVisibility
    if (viewUpdatedListenerSet.isNotEmpty() && isVisibleNow != wasVisibleBefore) {
      viewUpdatedListenerSet.forEach {
        it.onViewAnnotationVisibilityUpdated(
          view = annotation.view,
          visible = isVisibleNow
        )
      }
    }
  }

  private fun remove(internalId: String, annotation: ViewAnnotation) {
    mapView.removeView(annotation.view)
    updateVisibilityAndNotifyUpdateListeners(annotation, ViewAnnotationVisibility.INVISIBLE)
    annotation.view.removeOnAttachStateChangeListener(annotation.attachStateListener)
    annotation.attachStateListener = null
    getValue(mapboxMap.removeViewAnnotation(internalId))
  }

  companion object {
    internal const val TAG = "ViewAnnotation"
    internal const val EXCEPTION_TEXT_GEOMETRY_IS_NULL = "Geometry can not be null!"
    internal const val EXCEPTION_TEXT_ASSOCIATED_FEATURE_ID_ALREADY_EXISTS =
      "View annotation with associatedFeatureId=%s already exists!"
  }
}