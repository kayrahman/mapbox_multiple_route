package com.mapbox.navigation.examples.basics

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.nkr.mapboxdemo.R
import com.nkr.mapboxdemo.databinding.MapboxActivityRouteLineBinding

/**
 * This example demonstrates the usage of the route line and route arrow API's and UI elements.
 *
 * Before running the example make sure you have put your access_token in the correct place
 * inside [app/src/main/res/values/mapbox_access_token.xml]. If not present then add this file
 * at the location mentioned above and add the following content to it
 *
 * <?xml version="1.0" encoding="utf-8"?>
 * <resources xmlns:tools="http://schemas.android.com/tools">
 *     <string name="mapbox_access_token"><PUT_YOUR_ACCESS_TOKEN_HERE></string>
 * </resources>
 *
 * The example assumes that you have granted location permissions and does not enforce it. However,
 * the permission is essential for proper functioning of this example. The example also uses replay
 * location engine to facilitate navigation without actually physically moving.
 *
 * The example uses camera API's exposed by the Maps SDK rather than using the API's exposed by the
 * Navigation SDK. This is done to make the example concise and keep the focus on actual feature at
 * hand. To learn more about how to use the camera API's provided by the Navigation SDK look at
 * [ShowCameraTransitionsActivity]
 *
 * How to use this example:
 * - The example uses a single hardcoded route with alternatives.
 * - When the example starts, the camera transitions to the location where the route is.
 * - It then draws a route line on the map using the hardcoded route.
 * - Click on start navigation.
 * - You should now be able to navigate to the destination with the route line and route arrows drawn.
 */
class RenderRouteLineActivity : AppCompatActivity() {
    /**
     * Debug tool used to play, pause and seek route progress events that can be used to produce mocked location updates along the route.
     */
    private val mapboxReplayer = MapboxReplayer()

    /**
     * Debug observer that makes sure the replayer has always an up-to-date information to generate mock updates.
     */
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    /**
     * Debug tool that mocks location updates with an input from the [mapboxReplayer].
     */
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)

    private val hardCodedRoute by lazy {
        DirectionsRoute.fromJson(
            resources.getString(R.string.hard_coded_route)
        )
    }

    /**
     * Bindings to the example layout.
     */
    private val viewBinding: MapboxActivityRouteLineBinding by lazy {
        MapboxActivityRouteLineBinding.inflate(layoutInflater)
    }

    /**
     * Mapbox Maps entry point obtained from the [MapView].
     * You need to get a new reference to this object whenever the [MapView] is recreated.
     */
    private val mapboxMap: MapboxMap by lazy {
        viewBinding.mapView.getMapboxMap()
    }

    /**
     * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
     * to the Maps SDK in order to update the user location indicator on the map.
     */
    private val navigationLocationProvider by lazy {
        NavigationLocationProvider()
    }

    private val locationComponent by lazy {
        viewBinding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            // When true, the blue circular puck is shown on the map. If set to false, user
            // location in the form of puck will not be shown on the map.
            enabled = true
        }
    }

    /**
     * Mapbox Navigation entry point. There should only be one instance of this object for the app.
     * You can use [MapboxNavigationProvider] to help create and obtain that instance.
     */
    private val mapboxNavigation by lazy {
        if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(this)
                    .accessToken(getString(R.string.mapbox_access_token))
                    // comment out the location engine setting block to disable simulation
                    .locationEngine(replayLocationEngine)
                    .build()
            )
        }
    }

    /**
     * RouteLine: Various route line related options can be customized here including applying
     * route line color customizations.
     */
    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            /**
             * Route line related colors can be customized via the [RouteLineColorResources]. If using the
             * default colors the [RouteLineColorResources] does not need to be set as seen here, the
             * defaults will be used internally by the builder.
             */
            .routeLineColorResources(RouteLineColorResources.Builder().build())
            .build()
    }

    /**
     * RouteLine: Additional route line options are available through the MapboxRouteLineOptions.
     * Notice here the withRouteLineBelowLayerId option. The map is made up of layers. In this
     * case the route line will be placed below the "road-label" layer which is a good default
     * for the most common Mapbox navigation related maps. You should consider if this should be
     * changed for your use case especially if you are using a custom map style.
     */
    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            /**
             * Remove this line and [onPositionChangedListener] if you don't wish to show the
             * vanishing route line feature
             */
            .withVanishingRouteLineEnabled(true)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .build()
    }

    /**
     * RouteLine: This class is responsible for rendering route line related mutations generated
     * by the [routeLineApi]
     */
    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    /**
     * RouteLine: This class is responsible for generating route line related data which must be
     * rendered by the [routeLineView] in order to visualize the route line on the map.
     */
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    /**
     * RouteArrow: This class is responsible for generating data related to maneuver arrows. The
     * data generated must be rendered by the [routeArrowView] in order to apply mutations to
     * the map.
     */
    private val routeArrowApi: MapboxRouteArrowApi by lazy {
        MapboxRouteArrowApi()
    }

    /**
     * RouteArrow: Customization of the maneuver arrow(s) can be done using the
     * [RouteArrowOptions]. Here the above layer ID is used to determine where in the map layer
     * stack the arrows appear. Above the layer of the route traffic line is being used here. Your
     * use case may necessitate adjusting this to a different layer position.
     */
    private val routeArrowOptions by lazy {
        RouteArrowOptions.Builder(this)
            .withAboveLayerId(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .build()
    }

    /**
     * RouteArrow: This class is responsible for rendering the arrow related mutations generated
     * by the [routeArrowApi]
     */
    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(routeArrowOptions)
    }

    /**
     * RouteLine: This is one way to keep the route(s) appearing on the map in sync with
     * MapboxNavigation. When this observer is called the route data is used to draw route(s)
     * on the map.
     */
    private val routesObserver: RoutesObserver = RoutesObserver { routeUpdateResult ->
        // RouteLine: wrap the DirectionRoute objects and pass them
        // to the MapboxRouteLineApi to generate the data necessary to draw the route(s)
        // on the map.
        val routeLines = routeUpdateResult.routes.map { RouteLine(it, null) }

        routeLineApi.setRoutes(
            routeLines
        ) { value ->
            // RouteLine: The MapboxRouteLineView expects a non-null reference to the map style.
            // the data generated by the call to the MapboxRouteLineApi above must be rendered
            // by the MapboxRouteLineView in order to visualize the changes on the map.
            mapboxMap.getStyle()?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }

    /**
     * RouteLine: This listener is necessary only when enabling the vanishing route line feature
     * which changes the color of the route line behind the puck during navigation. If this
     * option is set to `false` (the default) in MapboxRouteLineOptions then it is not necessary
     * to use this listener.
     */
    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeLineView.renderRouteLineUpdate(this, result)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        // RouteLine: This line is only necessary if the vanishing route line feature
        // is enabled.
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            mapboxMap.getStyle()?.apply {
                routeLineView.renderRouteLineUpdate(this, result)
            }
        }

        // RouteArrow: The next maneuver arrows are driven by route progress events.
        // Generate the next maneuver arrow update data and pass it to the view class
        // to visualize the updates on the map.
        val arrowUpdate = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
        mapboxMap.getStyle()?.apply {
            // Render the result to update the map.
            routeArrowView.renderManeuverUpdate(this, arrowUpdate)
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            updateCamera(
                Point.fromLngLat(
                    enhancedLocation.longitude, enhancedLocation.latitude
                ),
                enhancedLocation.bearing.toDouble()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        init()
    }

    private fun init() {
        initStyle()
        initNavigation()
        initListeners()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS,
            {
                updateCamera(Point.fromLngLat(90.42607813911587,23.800920189381237), null)
                viewBinding.startNavigation.visibility = View.VISIBLE
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    Log.e(
                        RenderRouteLineActivity::class.java.simpleName,
                        "Error loading map: " + eventData.message
                    )
                }
            }
        )
    }

    private val routesReqCallback = object : RouterCallback {
        override fun onRoutesReady(
            routes: List<DirectionsRoute>,
            routerOrigin: RouterOrigin
        ) {

            Log.d("map_route","success")
        }

        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {

            Log.d("map_route","cancelled")
        }

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {

            Log.d("map_route","failure")
        }
    }


    @SuppressLint("MissingPermission")
    private fun initNavigation() {

        val originLocation = Location("test").apply {
            longitude = 90.42607813911287
            latitude = 23.800920189381237
            bearing = 10f
        }

        val destination = Point.fromLngLat(-122.4106, 37.7676)
        val destination_one = com.mapbox.geojson.Point.fromLngLat(90.42607813911587 ,23.800920189381237)
        val destination_two = com.mapbox.geojson.Point.fromLngLat(90.42305180879318,23.802176921921394)
        val destination_three = com.mapbox.geojson.Point.fromLngLat(90.4221583180499,23.798392170778254)
        val destination_4 = com.mapbox.geojson.Point.fromLngLat(90.43681818535491,23.79992122350774 )
        val destination_5 = com.mapbox.geojson.Point.fromLngLat(90.43894692431009,23.806673101634708)
        val destination_6 = com.mapbox.geojson.Point.fromLngLat(90.48269752897203,23.801599909470372)
        val destination_7 = com.mapbox.geojson.Point.fromLngLat(90.46090608787506,23.802628155598427)
        val destination_8 = com.mapbox.geojson.Point.fromLngLat(90.45478839115248,23.80152631375682)
        val destination_9 = com.mapbox.geojson.Point.fromLngLat(90.4495316550393,23.799103067378482)
        val destination_10 = com.mapbox.geojson.Point.fromLngLat(90.43612231026098,23.78594280711822)

        val destination_11 = com.mapbox.geojson.Point.fromLngLat(90.42162433364186,23.80976835069088, )
        val destination_12 = com.mapbox.geojson.Point.fromLngLat(90.40421491615497,23.805957631134746, )
        val destination_13 = com.mapbox.geojson.Point.fromLngLat(90.39571705863702,23.8000469241696, )
        val destination_14 = com.mapbox.geojson.Point.fromLngLat(90.40493126490954,23.7947645582653, )
        val destination_15 = com.mapbox.geojson.Point.fromLngLat(90.40764582360396,23.79256059376679, )
        val destination_16 = com.mapbox.geojson.Point.fromLngLat(90.4146425030557,23.795079407284987, )
        val destination_17 = com.mapbox.geojson.Point.fromLngLat(90.42339791067567,23.79305036688576, )
        val destination_18 = com.mapbox.geojson.Point.fromLngLat(90.4221717272893,23.790856369121762, )
        val destination_19 = com.mapbox.geojson.Point.fromLngLat(90.4212601668327,23.800351927494116, )
        val destination_20 = com.mapbox.geojson.Point.fromLngLat(90.42693988967766,23.813182660437427, )

        val destination_21 = com.mapbox.geojson.Point.fromLngLat(90.39075434980492,23.79409333694959,  )
        val destination_22 = com.mapbox.geojson.Point.fromLngLat(90.37873933339738,23.7877475214995,  )
        val destination_23 = com.mapbox.geojson.Point.fromLngLat(90.39101466441126,23.78076189570033,  )
        val destination_24 = com.mapbox.geojson.Point.fromLngLat(90.37311751463996,23.769925512535046,  )
        val destination_25 = com.mapbox.geojson.Point.fromLngLat(90.37847043819451,23.77764467250323,  )
      //  val destination_26 = com.mapbox.geojson.Point.fromLngLat(90.36744017037817,23.773042920723928,  )
       // val destination_27 = com.mapbox.geojson.Point.fromLngLat(90.36111398755938,23.77858479518869,  )
      //  val destination_28 = com.mapbox.geojson.Point.fromLngLat(90.3748604156022,23.755910242677068,  )
      //  val destination_29 = com.mapbox.geojson.Point.fromLngLat(90.37243001457438,23.743971689108367,  )
     //   val destination_30 = com.mapbox.geojson.Point.fromLngLat(90.3837243478505,23.736369410496373,  )


        val originPoint = com.mapbox.geojson.Point.fromLngLat(
            originLocation.longitude,
            originLocation.latitude
        )

        val routeOptions = RouteOptions.builder()
            // applies the default parameters to route options
            .applyDefaultNavigationOptions()
           // .applyLanguageAndVoiceUnitOptions(this)
            // lists the coordinate pair i.e. origin and destination
            // If you want to specify waypoints you can pass list of points instead of null
           // .coordinatesList(listOf(originPoint, destination))
            .coordinatesList(listOf(destination_one,destination_two,destination_three,
                destination_4,destination_5,destination_6,destination_7,
                destination_8,destination_9,destination_10,destination_11,
                destination_12,destination_13,destination_14,destination_15,
                destination_16,destination_17,destination_18,destination_19,
                destination_20,destination_21,destination_22,destination_23,
                destination_24,destination_25
               // ,destination_26,destination_27, destination_28,destination_29,destination_30
                ))
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
                    null,null,null,null,null,null,null,null,null,
                    null,null,null,null,null,null,null,null,null,null,
                    null,null,null,null,null
                    //,null,null,null,null,null
                )
            )
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .build()



        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
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
                        mapboxMap.getStyle()?.apply {
                            routeLineView.renderRouteDrawData(this, value)
                        }
                    }

                    Log.d("map_route","success")

                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    Log.d("map_route","cancelled")
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    Log.d("map_route","failed : ${reasons.toString()}")
                }
            }
        )
    }

    @SuppressLint("SetTextI18n")
    private fun initListeners() {
        viewBinding.startNavigation.text = "Start Navigation"
        viewBinding.startNavigation.setOnClickListener {
            viewBinding.startNavigation.visibility = View.INVISIBLE
            locationComponent.addOnIndicatorPositionChangedListener(onPositionChangedListener)
            // RouteLine: Hiding the alternative routes when navigation starts.
            mapboxMap.getStyle()?.apply {
                routeLineView.hideAlternativeRoutes(this)
            }
            startSimulation(hardCodedRoute)
        }
    }

    private fun updateCamera(point: Point, bearing: Double?) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        viewBinding.mapView.camera.easeTo(
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

    // Starts the navigation simulator
    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.run {
            stop()
            clearEvents()
            pushRealLocation(this@RenderRouteLineActivity, 0.0)
            val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
            pushEvents(replayEvents)
            seekTo(replayEvents.first())
            play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationComponent.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mapboxNavigation.run {
            // make sure to stop the trip session. In this case it is being called inside `onDestroy`.
            stopTripSession()
            // make sure to unregister the routes observer you have registered.
            unregisterRoutesObserver(routesObserver)
            // make sure to unregister the location observer you have registered.
            unregisterLocationObserver(locationObserver)
            // make sure to unregister the route progress observer you have registered.
            unregisterRouteProgressObserver(routeProgressObserver)
            // make sure to unregister the route progress observer you have registered.
            unregisterRouteProgressObserver(replayProgressObserver)
        }
        mapboxReplayer.finish()
        routeLineView.cancel()
        routeLineApi.cancel()
        mapboxNavigation.onDestroy()
    }
}
