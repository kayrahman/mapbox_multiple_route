package com.nkr.mapboxdemo

import android.annotation.SuppressLint
import android.graphics.Point
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.google.gson.GsonBuilder
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.nkr.mapboxdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    //---------------------------------------------------------//

    private val mapboxNavigation by lazy {
        if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(this)
                    .accessToken(getString(R.string.mapbox_access_token))
                    .build()
            )
        }
    }

    private val originLocation = Location("test").apply {
        longitude = -122.4192
        latitude = 37.7627
        bearing = 10f
    }

    // private val destination = Point.fromLngLat(-122.4106, 37.7676)
    private val destination = com.mapbox.geojson.Point.fromLngLat(-122.4106, 37.7676)





    //---------------------------------------------------------//


    var mapView: MapView? = null
    

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = findViewById(R.id.mapView)
        mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS)

        fetchARoute()


    }


    //-------------------------------------------------//










    /**
     * The method instantiates a [RouteOptions] object and fetches route between the origin and
     * destination pair. There are several [RouteOptions] that you can specify, but this example
     * mentions only what is relevant.
     */
    @SuppressLint("SetTextI18n")
    private fun fetchARoute() {

        val originPoint = com.mapbox.geojson.Point.fromLngLat(
            originLocation.longitude,
            originLocation.latitude
        )

        val routeOptions = RouteOptions.builder()
            // applies the default parameters to route options
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            // lists the coordinate pair i.e. origin and destination
            // If you want to specify waypoints you can pass list of points instead of null
            .coordinatesList(listOf(originPoint, destination))
            // set it to true if you want to receive alternate routes to your destination
            .alternatives(false)
            // provide the bearing for the origin of the request to ensure
            // that the returned route faces in the direction of the current user movement
            .bearingsList(
                listOf(
                    Bearing.builder()
                        .angle(originLocation.bearing.toDouble())
                        .degrees(45.0)
                        .build(),
                    null
                )
            )
            .build()



        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
                /**
                 * The callback is triggered when the routes are ready to be displayed.
                 */
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                // GSON instance used only to print the response prettily
                    val gson = GsonBuilder().setPrettyPrinting().create()

                    val routeLines = routes.map { RouteLine(it, null) }

                    val routeLineOptions = MapboxRouteLineOptions.Builder(applicationContext).build()
                    val routeLineApi = MapboxRouteLineApi(routeLineOptions)
                    val routeLineView = MapboxRouteLineView(routeLineOptions)
                    routeLineApi.setRoutes(routeLines) { value ->
                        mapView?.getMapboxMap()?.getStyle()?.let {
                            routeLineView.renderRouteDrawData(
                                it, value)
                        }
                    }


                }

                /**
                 * The callback is triggered if the request to fetch a route was canceled.
                 */
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {

                }

                /**
                 * The callback is triggered if the request to fetch a route failed for any reason.
                 */
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {

                }
            }
        )
    }

        //-------------------------------------------------//


        override fun onStart() {
            super.onStart()
            mapView?.onStart()
        }

        override fun onStop() {
            super.onStop()
            mapView?.onStop()
        }

        override fun onLowMemory() {
            super.onLowMemory()
            mapView?.onLowMemory()
        }

        override fun onDestroy() {
            super.onDestroy()
            mapView?.onDestroy()
        }


    }
