package com.mapbox.maps.plugin.locationcomponent

import android.content.Context
import android.hardware.SensorManager
import android.location.Location
import android.view.WindowManager
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.common.ShadowLogger
import com.mapbox.common.location.compat.*
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.PuckBearingSource
import com.mapbox.maps.plugin.locationcomponent.generated.LocationComponentSettings
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowLogger::class])
class LocationProviderImplTest {
  private val context = mockk<Context>(relaxed = true)
  private val windowManager = mockk<WindowManager>(relaxed = true)
  private val sensorManager = mockk<SensorManager>(relaxed = true)
  private val locationEngine = mockk<LocationEngine>(relaxed = true)
  private val locationConsumer1 = mockk<LocationConsumer>(relaxed = true)
  private val locationConsumer2 = mockk<LocationConsumer>(relaxed = true)
  private val locationComponentSettings = LocationComponentSettings(locationPuck = mockk())
  private val locationEngineRequestSlot = CapturingSlot<LocationEngineRequest>()
  private val locationEngineCallbackSlot =
    CapturingSlot<LocationEngineCallback<LocationEngineResult>>()

  private lateinit var locationProviderImpl: LocationProviderImpl

  @Before
  fun setup() {
    mockkStatic(LocationEngineProvider::class)
    every { context.getSystemService(Context.WINDOW_SERVICE) } returns windowManager
    every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
    every { LocationEngineProvider.getBestLocationEngine(context) } returns locationEngine
    locationComponentSettings.puckBearingSource = PuckBearingSource.COURSE
    locationProviderImpl = LocationProviderImpl(context, locationComponentSettings)
  }

  @Test
  fun testEmptyConsumer() {
    verify(exactly = 0) {
      locationEngine.requestLocationUpdates(
        capture(locationEngineRequestSlot),
        any(),
        any()
      )
    }
    verify(exactly = 0) {
      locationEngine.getLastLocation(any())
    }
  }

  @Test
  fun testAddLocationConsumerWithoutPermission() {
    mockkStatic(PermissionsManager::class)
    every { PermissionsManager.areLocationPermissionsGranted(any()) } returns false
    locationProviderImpl.registerLocationConsumer(locationConsumer1)
    verify(exactly = 0) {
      locationEngine.requestLocationUpdates(
        any(),
        any(),
        any()
      )
    }
    verify(exactly = 0) {
      locationEngine.getLastLocation(any())
    }
  }

  @Test
  fun testAddLocationConsumerWithPermission() {
    mockkStatic(PermissionsManager::class)
    every { PermissionsManager.areLocationPermissionsGranted(any()) } returns true
    locationProviderImpl.registerLocationConsumer(locationConsumer1)
    verify {
      locationEngine.requestLocationUpdates(
        capture(locationEngineRequestSlot),
        any(),
        any()
      )
    }
    assertEquals(
      LocationComponentConstants.DEFAULT_INTERVAL_MILLIS,
      locationEngineRequestSlot.captured.interval
    )
    assertEquals(
      LocationComponentConstants.DEFAULT_FASTEST_INTERVAL_MILLIS,
      locationEngineRequestSlot.captured.fastestInterval
    )
    assertEquals(
      LocationEngineRequest.PRIORITY_HIGH_ACCURACY,
      locationEngineRequestSlot.captured.priority
    )
    verify(exactly = 1) {
      locationEngine.getLastLocation(any())
    }
  }

  @Test
  fun testAddTwoLocationConsumer() {
    locationProviderImpl.registerLocationConsumer(locationConsumer1)
    locationProviderImpl.registerLocationConsumer(locationConsumer2)
    verify(exactly = 1) {
      locationEngine.requestLocationUpdates(
        capture(locationEngineRequestSlot),
        any(),
        any()
      )
    }
    verify(exactly = 2) {
      locationEngine.getLastLocation(any())
    }
  }

  @Test
  fun testRemoveLocationConsumer() {
    locationProviderImpl.registerLocationConsumer(locationConsumer1)
    locationProviderImpl.registerLocationConsumer(locationConsumer2)
    locationProviderImpl.unRegisterLocationConsumer(locationConsumer2)
    verify(exactly = 0) {
      locationEngine.removeLocationUpdates(any() as LocationEngineCallback<LocationEngineResult>)
    }
  }

  @Test
  fun testRemoveAllLocationConsumer() {
    locationProviderImpl.registerLocationConsumer(locationConsumer1)
    locationProviderImpl.registerLocationConsumer(locationConsumer2)
    locationProviderImpl.unRegisterLocationConsumer(locationConsumer2)
    locationProviderImpl.unRegisterLocationConsumer(locationConsumer1)
    verify(exactly = 1) {
      locationEngine.removeLocationUpdates(any() as LocationEngineCallback<LocationEngineResult>)
    }
  }

  @Test
  fun testLocationUpdate() {
    val locationEngineResult = mockk<LocationEngineResult>(relaxed = true)
    val location = mockk<Location>(relaxed = true)
    every { locationEngineResult.lastLocation } returns location
    every { location.longitude } returns 12.0
    every { location.latitude } returns 34.0
    every { location.bearing } returns 90.0f

    locationProviderImpl.registerLocationConsumer(locationConsumer1)
    locationProviderImpl.registerLocationConsumer(locationConsumer2)
    verify(exactly = 1) {
      locationEngine.requestLocationUpdates(
        any(),
        capture(locationEngineCallbackSlot),
        any()
      )
    }
    locationEngineCallbackSlot.captured.onSuccess(locationEngineResult)
    verify { locationConsumer1.onLocationUpdated(Point.fromLngLat(12.0, 34.0)) }
    verify { locationConsumer1.onBearingUpdated(90.0) }
    verify { locationConsumer2.onLocationUpdated(Point.fromLngLat(12.0, 34.0)) }
    verify { locationConsumer2.onBearingUpdated(90.0) }
  }

  @Test
  fun testLocationUpdateWithCompass() {
    locationComponentSettings.puckBearingSource = PuckBearingSource.HEADING
    locationProviderImpl = LocationProviderImpl(context, locationComponentSettings)
    val locationEngineResult = mockk<LocationEngineResult>(relaxed = true)
    val location = mockk<Location>(relaxed = true)
    every { locationEngineResult.lastLocation } returns location
    every { location.longitude } returns 12.0
    every { location.latitude } returns 34.0
    every { location.bearing } returns 90.0f

    locationProviderImpl.registerLocationConsumer(locationConsumer1)
    locationProviderImpl.registerLocationConsumer(locationConsumer2)
    verify(exactly = 1) {
      locationEngine.requestLocationUpdates(
        any(),
        capture(locationEngineCallbackSlot),
        any()
      )
    }
    locationEngineCallbackSlot.captured.onSuccess(locationEngineResult)
    verify { locationConsumer1.onLocationUpdated(Point.fromLngLat(12.0, 34.0)) }
    verify(exactly = 0) { locationConsumer1.onBearingUpdated(90.0) }
    verify { locationConsumer2.onLocationUpdated(Point.fromLngLat(12.0, 34.0)) }
    verify(exactly = 0) { locationConsumer2.onBearingUpdated(90.0) }
    locationProviderImpl.onCompassChanged(90.0f)
    verify { locationConsumer1.onBearingUpdated(90.0) }
    verify { locationConsumer2.onBearingUpdated(90.0) }
  }
}