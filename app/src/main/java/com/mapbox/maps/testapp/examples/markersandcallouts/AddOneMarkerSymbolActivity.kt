package com.mapbox.maps.testapp.examples.markersandcallouts

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.viewannotation.ViewAnnotationPlugin
import com.mapbox.maps.plugin.viewannotation.viewAnnotation
import com.mapbox.maps.testapp.R

/**
 * Example showing how to add a marker on map with symbol layer
 */
class AddOneMarkerSymbolActivity : AppCompatActivity(), OnMapClickListener {

  private lateinit var viewAnnotationPlugin: ViewAnnotationPlugin
  private var count = 1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val mapView = MapView(
      this,
      MapInitOptions(
        context = this,
        textureView = false
      )
    )
    setContentView(mapView)

    viewAnnotationPlugin = mapView.viewAnnotation
    mapView.getMapboxMap().also {
      it.setCamera(
        CameraOptions.Builder()
          .center(Point.fromLngLat(LONGITUDE, LATITUDE))
          .zoom(8.0)
          .build()
      )
      // we need to wait until map is loaded to add annotations as we rely on MapOptions.size
      // guess it should be handled in our plugin internally but leaving as is for now
      it.addOnMapLoadedListener {
        mapView.getMapboxMap().addOnMapClickListener(this)
      }
    }.loadStyle(
      styleExtension = style(Style.MAPBOX_STREETS) {
        // prepare blue marker from resources
        +image(BLUE_ICON_ID) {
          bitmap(BitmapFactory.decodeResource(resources, R.drawable.blue_marker_view))
        }
        +geoJsonSource(SOURCE_ID) {
          geometry(Point.fromLngLat(LONGITUDE, LATITUDE))
        }
        +symbolLayer(LAYER_ID, SOURCE_ID) {
          iconImage(BLUE_ICON_ID)
          iconAnchor(IconAnchor.BOTTOM)
        }
      }
    )
  }

  companion object {
    private const val BLUE_ICON_ID = "blue"
    private const val SOURCE_ID = "source_id"
    private const val LAYER_ID = "layer_id"
    private const val LATITUDE = 55.665957
    private const val LONGITUDE = 12.550343
  }

  override fun onMapClick(point: Point): Boolean {
    // TODO revisit API and allow be nullable
    viewAnnotationPlugin.addViewAnnotation(
      R.layout.item_callout_view,
      ViewAnnotationOptions.Builder()
        .geometry(point)
        .build()
    ) { view ->
      view.findViewById<TextView>(R.id.textNativeView).text = "Callout ${count++}"
      view.findViewById<ImageView>(R.id.closeNativeView).setOnClickListener {
//        view.visibility = View.GONE
        viewAnnotationPlugin.removeViewAnnotation(view)
      }
    }
    return true
  }
}