package com.nkr.mapboxdemo

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.GsonBuilder
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.examples.basics.RenderRouteLineActivity
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.nkr.mapboxdemo.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mapbox.geojson.LineString
import com.mapbox.maps.extension.observable.eventdata.MapLoadedEventData
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadedListener

import android.R.style





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

        binding.btnAcRender.setOnClickListener {
            startActivity(Intent(this,RenderRouteLineActivity::class.java))
        }

        initStyle()

       // fetchARoute()

        val dest_one = com.mapbox.geojson.Point.fromLngLat(90.42607813911587 ,23.800920189381237)
        val dest_two = com.mapbox.geojson.Point.fromLngLat(90.42305180879318,23.802176921921394)
        val dest_three = com.mapbox.geojson.Point.fromLngLat(90.4221583180499,23.798392170778254)
        val dest_4 = com.mapbox.geojson.Point.fromLngLat(90.43681818535491,23.79992122350774 )
        val dest_5 = com.mapbox.geojson.Point.fromLngLat(90.43894692431009,23.806673101634708)
        val dest_6 = com.mapbox.geojson.Point.fromLngLat(90.48269752897203,23.801599909470372)
        val dest_7 = com.mapbox.geojson.Point.fromLngLat(90.46090608787506,23.802628155598427)
        val dest_8 = com.mapbox.geojson.Point.fromLngLat(90.45478839115248,23.80152631375682)
        val dest_9 = com.mapbox.geojson.Point.fromLngLat(90.4495316550393,23.799103067378482)
        val dest_10 = com.mapbox.geojson.Point.fromLngLat(90.43612231026098,23.78594280711822)

        val lat_longs = arrayListOf<com.mapbox.geojson.Point>(dest_one,dest_two,dest_three,dest_4,dest_5,
        dest_10
           // ,dest_5
            // ,dest_6,dest_7,dest_8,dest_9,dest_10
        )

    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapView?.getMapboxMap()?.loadStyleUri(
            Style.MAPBOX_STREETS,
            {
                updateCamera(Point.fromLngLat(90.42607813911587, 23.800920189381237), null)
               // viewBinding.startNavigation.visibility = View.VISIBLE

                       it.addSource(GeoJsonSource(
                    "line_source", FeatureCollection.fromFeatures(
                        arrayOf(
                            Feature.fromGeometry(
                                LineString.fromLngLats(routeCoordinates)
                            )
                        )
                    )
                ))


            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    Log.e(
                        MainActivity::class.java.simpleName,
                        "Error loading map: " + eventData.message
                    )
                }
            }
        )
    }

    private fun updateCamera(point: Point, bearing: Double?) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapView?.camera?.easeTo(
            CameraOptions.Builder()
                .center(point)
                .bearing(bearing)
                .pitch(45.0)
                .zoom(13.0)
                //.padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }


    private fun findRoutes(origin_point: Point, dest_point: Point) {
        val origin = com.mapbox.geojson.Point.fromLngLat(
            origin_point.longitude(),
            origin_point.latitude()
        )

        val dest = com.mapbox.geojson.Point.fromLngLat(
            dest_point.longitude(),
            dest_point.latitude()
        )


        val routeOptions = RouteOptions.builder()
            // applies the default parameters to route options
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(this)
            // lists the coordinate pair i.e. origin and destination
            // If you want to specify waypoints you can pass list of points instead of null
            .coordinatesList(listOf(origin, dest))
            // set it to true if you want to receive alternate routes to your destination
            .alternatives(false)
            // provide the bearing for the origin of the request to ensure
            // that the returned route faces in the direction of the current user movement
           /* .bearingsList(
                listOf(
                    Bearing.builder()
                        .angle(originLocation.bearing.toDouble())
                        .degrees(45.0)
                        .build(),
                    null
                )
            )*/
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
                    Log.d("map_route","routes_ready")
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
                    Log.d("map_route","cancel")
                }

                /**
                 * The callback is triggered if the request to fetch a route failed for any reason.
                 */
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    Log.d("map_route","failure")
                }
            }
        )


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
                    Log.d("map_route","routes_ready")
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
                    Log.d("map_route","cancel")
                }

                /**
                 * The callback is triggered if the request to fetch a route failed for any reason.
                 */
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    Log.d("map_route","failure")
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
