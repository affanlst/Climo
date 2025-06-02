package com.vergiawan.climo.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.atmosphere.generated.atmosphere
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.vergiawan.climo.LocationViewModel
import com.vergiawan.climo.R

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fabHiker: FloatingActionButton
    private lateinit var fabUser: FloatingActionButton
    private val viewModel: LocationViewModel by activityViewModels()

    private var geoLat: Double? = null
    private var geoLng: Double? = null
    private var userLocation: Point? = null
    private var markerSource: GeoJsonSource? = null

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.mapboxMap.setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        userLocation = it
        mapView.mapboxMap.setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.mapboxMap.pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean = false
        override fun onMoveBegin(detector: MoveGestureDetector) = onCameraTrackingDismissed()
        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        fabHiker = view.findViewById(R.id.fabHiker)
        fabUser = view.findViewById(R.id.fabUser)

        geoLat = viewModel.dataLat.value ?: 0.0
        geoLng = viewModel.dataLng.value ?: 0.0

        fabHiker.setOnClickListener {
            if (geoLat != null && geoLng != null) {
                moveCameraTo(Point.fromLngLat(geoLng!!, geoLat!!))
            }
        }

        fabUser.setOnClickListener {
            userLocation?.let {
                moveCameraTo(it)
            }
        }

        viewModel.dataLat.observe(viewLifecycleOwner) {
            geoLat = it
            updateMarkerLocation()
        }

        viewModel.dataLng.observe(viewLifecycleOwner) {
            geoLng = it
            updateMarkerLocation()
        }

        onMapReady()
    }

    private fun onMapReady() {
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .zoom(12.0)
                .build()
        )

        mapView.mapboxMap.loadStyle(
            style(Style.SATELLITE_STREETS) {
                +image(
                    "marker-icon",
                    ContextCompat.getDrawable(requireContext(), R.drawable.hiker_marker)!!
                        .toBitmap()
                )

                val source = geoJsonSource("marker-source") {
                    geometry(Point.fromLngLat(geoLng!!, geoLat!!))
                }
                markerSource = source
                +source

                +symbolLayer("hiker-location", "marker-source") {
                    iconImage("marker-icon")
                    iconAnchor(IconAnchor.BOTTOM)
                }

                +projection(ProjectionName.GLOBE)
                +atmosphere {
                    color(Color.rgb(220, 159, 159))
                    highColor(Color.rgb(220, 159, 159))
                    horizonBlend(0.4)
                }
                +rasterDemSource("raster-dem") {
                    url("mapbox://mapbox.terrain-rgb")
                }
                +terrain("raster-dem")
            }
        ) {
            setupGestureListener()
            initLocationComponent()
        }
    }

    private fun moveCameraTo(point: Point) {
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(14.0)
                .build()
        )
    }

    private fun updateMarkerLocation() {
        if (geoLat != null && geoLng != null) {
            markerSource?.geometry(Point.fromLngLat(geoLng!!, geoLat!!))
        }
    }

    private fun setupGestureListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = createDefault2DPuck(withBearing = true)
        }

        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
    }

    private fun onCameraTrackingDismissed() {
        Log.i("cameraTracking", "onCameraTrackingDismissed()")
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }
}
