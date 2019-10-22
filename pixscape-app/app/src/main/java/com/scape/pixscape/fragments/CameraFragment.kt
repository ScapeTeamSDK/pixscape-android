package com.scape.pixscape.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.exlyo.gmfmt.FloatingMarkerTitlesOverlay
import com.exlyo.gmfmt.MarkerInfo
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.*
import com.google.android.libraries.maps.model.CameraPosition
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MapStyleOptions
import com.google.android.libraries.maps.model.Marker
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.data.kml.KmlLayer
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.scape.pixscape.R
import com.scape.pixscape.activities.MainActivity
import com.scape.pixscape.activities.TraceDetailsActivity
import com.scape.pixscape.adapter.ViewPagerAdapter
import com.scape.pixscape.events.LocationMeasurementEvent
import com.scape.pixscape.events.ScapeMeasurementEvent
import com.scape.pixscape.events.ScapeSessionStateEvent
import com.scape.pixscape.events.TimerEvent
import com.scape.pixscape.manager.TrackTraceManager
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.utils.*
import com.scape.scapekit.ScapeMeasurementsStatus
import com.scape.scapekit.ScapeSessionState
import com.scape.scapekit.setByteBuffer
import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.fragment_track_trace.*
import kotlinx.android.synthetic.main.view_pager_tabs.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.and

enum class TimerState { Idle, Running, Paused }

internal class CameraFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    ViewPager.OnPageChangeListener {

    private var luma: ByteBuffer? = null
    private var cameraIntrinsics: CameraIntrinsics? = null

    private lateinit var container: RelativeLayout
    private lateinit var viewFinder: CameraView

    private var miniMapView: MapView? = null
    private var miniMap: GoogleMap? = null
    private var floatingMarkerTitlesOverlay: FloatingMarkerTitlesOverlay? = null
    private var connectivitySnackBar: Snackbar? = null
    private var errorMessageSnackBar: Snackbar? = null

    companion object {
        private const val TAG = "CameraFragment"
        private var isContinuousModeEnabled: Boolean = false

        const val TIME_DATA_KEY = "com.scape.pixscape.camerafragment.timedatakey"
        const val MODE_DATA_KEY = "com.scape.pixscape.camerafragment.modedatakey"
        const val BROADCAST_CONNECTIVITY_OFF = "com.scape.pixscape.camerafragment.broadcastnetworkoff"
        const val BROADCAST_CONNECTIVITY_ON = "com.scape.pixscape.camerafragment.broadcastnetworkon"
        const val ROUTE_GPS_SECTIONS_DATA_KEY = "com.scape.pixscape.camerafragment.routegpssectionsdatakey"
        const val ROUTE_SCAPE_SECTIONS_DATA_KEY = "com.scape.pixscape.camerafragment.routescapesectionsdatakey"


        private var timerState = TimerState.Idle
        private var measuredTimeInMillis = 0L
        private var distanceInMeters = 0.0
        private var gpsRouteSections: List<RouteSection> = ArrayList()
        private var scapeRouteSections: List<RouteSection> = ArrayList()
        private var scapeSessionState: ScapeSessionState = ScapeSessionState.NO_ERROR
        private var scapeMeasurementsStatus: ScapeMeasurementsStatus = ScapeMeasurementsStatus.RESULTS_FOUND
        private var scapeConfidenceScore = 0.0
    }

    private val frameProcessor = FrameProcessor { frame ->
        val width = frame.size.width
        val height = frame.size.height
        val size = width * height

        if(cameraIntrinsics == null) {
            cameraIntrinsics = viewFinder.cameraIntrinsics
        }

        if (luma == null) {
            luma = ByteBuffer.allocateDirect(size)
        }

        for (pixelPointer in 0 until size) {
            luma?.put(pixelPointer, frame.data[pixelPointer] and 0xFF.toByte())
        }

        val intrinsics = cameraIntrinsics
        if (intrinsics != null) {
            //Log.e("frameProcessor", "$intrinsics $width $height")

            val scapeClient = TrackTraceManager.sharedInstance(context!!).scapeClient

            scapeClient?.scapeSession?.setCameraIntrinsics(intrinsics.focalLengthX,
                intrinsics.focalLengthY,
                intrinsics.principalPointX,
                intrinsics.principalPointY)
            val byteBuffer = luma
            if(byteBuffer != null) {
                scapeClient?.scapeSession?.setByteBuffer(byteBuffer, width, height)
            }
        }
    }

    private val networkConnectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action) {
                BROADCAST_CONNECTIVITY_OFF -> {
                    connectivitySnackBar = container.showSnackbar(getString(R.string.localization_network_error),
                        R.color.red,
                        Snackbar.LENGTH_INDEFINITE)
                }
                BROADCAST_CONNECTIVITY_ON -> {
                    connectivitySnackBar?.dismiss()
                }
            }
        }
    }

    // region EventBus

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LocationMeasurementEvent) {
        Log.d(TAG, "on LocationMeasurementEvent received")

        if (activity == null) return
        gpsRouteSections = event.gpsLocations

        distanceInMeters = gpsRouteSections.sumByDouble { section -> section.distance.toDouble() }

        fillMap()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ScapeMeasurementEvent) {
        Log.d(TAG, "on ScapeMeasurementEvent received")

        if (activity == null) return

        scapeRouteSections = event.scapeLocations

        fillMap()

        // if we have any scape error then the service will
        // trigger BROADCAST_ACTION_MEASUREMENTS_ERROR which effectively stops the service,
        // but if we have a succesful scape result then we need to stop the service here when in single mode
        if(!isContinuousModeEnabled) {
            stopSingleLocalization()
        }
        else {
            errorMessageSnackBar?.dismiss()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ScapeSessionStateEvent) {
        Log.d(TAG, "on ScapeSessionStateEvent received")

        if(activity == null) return
        scapeSessionState = event.state
        scapeMeasurementsStatus = event.status
        scapeConfidenceScore = event.confidenceScore

        showErrorMessage()

        if(!isContinuousModeEnabled) {
            stopSingleLocalization()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: TimerEvent) {
        measuredTimeInMillis = event.timeInMillis

        if (measuredTimeInMillis == 0L)
            Log.w("Broadcastreceiver", "service returned time 0")
        time.base = SystemClock.elapsedRealtime() - measuredTimeInMillis
    }

    // endregion EventBus

    // region Private

    private fun bindings() {
        viewFinder.setLifecycleOwner(this)

        viewFinder.addFrameProcessor(frameProcessor)

        card_view_minimap_container.setOnClickListener {
            view_pager.currentItem = 2
        }

        view_camera_center.setOnClickListener {
            if (view_pager.currentItem != 1) {
                view_pager.currentItem = 1
            } else if(!view_switch_bottom.isOn) { // we are not in the continuous mode
                startSingleShotLocalization()
            }
        }

        play_timer_button.setOnClickListener {
            if(timerState == TimerState.Idle) {
                timerState = TimerState.Running

                play_timer_button.hide()

                pause_timer_button.show()

                view_switch_bottom?.visibility = View.GONE

                startContinuousLocalization()

            } else if(timerState == TimerState.Paused) {
                timerState = TimerState.Running

                TrackTraceManager?.sharedInstance(context!!).resumeUpdatingContinuousLocation()

                play_timer_button.hide()
                stop_timer_button.visibility = View.GONE

                pause_timer_button.show()
            }
        }

        pause_timer_button.setOnClickListener {
            TrackTraceManager.sharedInstance(context!!).pauseUpdatingContinuousLocation()

            pause_timer_button.hide()

            play_timer_button.show()
            stop_timer_button.visibility = View.VISIBLE

            timerState = TimerState.Paused

            errorMessageSnackBar?.dismiss()
        }

        stop_timer_button.setOnClickListener {

            errorMessageSnackBar?.dismiss()

            stopTimerAndContinuousLocalization()

            startTrackTraceActivity()
        }
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes */
    @SuppressLint("RestrictedApi")
    private fun views() {
        // Inflate a new views containing all UI for controlling the camera
        val cameraUiContainer = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        val supportFragmentManager = fragmentManager ?: return

        //viewpager custom adapter
        val adapter = ViewPagerAdapter(supportFragmentManager)
        view_pager.offscreenPageLimit = 1
        view_pager.adapter = adapter

        // initialize views pager
        main_tab_view.setUpWithViewPager(view_pager)

        //set default
        view_pager.currentItem = 1

        //listener to viewpager scroll page
        view_pager.addOnPageChangeListener(this)
    }

    private fun startSingleShotLocalization() {
        Log.d(TAG, "startSingleShotLocalization")

        val signalStrength = checkNetworkSignalStrength(context!!)
        Log.d(TAG, "Network signal strength: $signalStrength")

        when (signalStrength) {
            NetworkSignalStrength.Weak -> {
                container.showSnackbar(getString(R.string.network_signal_weak),
                    R.color.red,
                    SNACKBAR_DURATION_LONG)
                return

            }
            NetworkSignalStrength.Fair -> {
                container.showSnackbar(getString(R.string.network_signal_fair),
                    R.color.red,
                    SNACKBAR_DURATION_LONG)
                return
            }
            else -> {}
        }

        view_switch_bottom?.visibility = View.GONE
        dots_view?.visibility = View.VISIBLE
        container.showSnackbar("Locking your position, please wait..", R.color.scape_blue, SNACKBAR_DURATION_SHORT)

        isContinuousModeEnabled = false

        TrackTraceManager.sharedInstance(activity!!).startUpdatingLocation(false)
    }

    private fun startContinuousLocalization() {
        isContinuousModeEnabled = true

        TrackTraceManager.sharedInstance(context!!).startUpdatingLocation(isContinuousModeEnabled)
    }

    private fun stopContinuousLocalization() {
        TrackTraceManager.sharedInstance(context!!).stopUpdatingLocation(isContinuousModeEnabled)

    }

    private fun stopSingleLocalization() {
        dots_view?.visibility = View.GONE

        view_switch_bottom?.postDelayed({view_switch_bottom?.visibility = View.VISIBLE}, 2000)

        TrackTraceManager.sharedInstance(context!!).stopUpdatingLocation(isContinuousModeEnabled)
    }

    private fun stopTimerAndContinuousLocalization() {
        timerState = TimerState.Idle

        miniMap?.clear()

        time?.base = SystemClock.elapsedRealtime()

        pause_timer_button?.hide()
        stop_timer_button?.visibility = View.GONE

        view_switch_bottom?.postDelayed({view_switch_bottom?.visibility = View.VISIBLE}, 2000)

        if(isContinuousModeEnabled) {
            play_timer_button?.show()
        }

        stopContinuousLocalization()
    }

    private fun startTrackTraceActivity() {
        try {
            @Suppress("UNCHECKED_CAST") val localGpsRouteSections = gpsRouteSections as ArrayList<Parcelable>
            @Suppress("UNCHECKED_CAST") val localScapeRouteSections = scapeRouteSections as ArrayList<Parcelable>

            if (localGpsRouteSections.size <= 2) return // do not show TraceDetailsActivity if we do not have enough traces

            gpsRouteSections = ArrayList()
            scapeRouteSections = ArrayList()

            floatingMarkerTitlesOverlay?.clearMarkers()
            miniMap?.clear()

            val bundle = Bundle().apply {
                putParcelableArrayList(ROUTE_GPS_SECTIONS_DATA_KEY, localGpsRouteSections)
                putParcelableArrayList(ROUTE_SCAPE_SECTIONS_DATA_KEY, localScapeRouteSections)
            }

            val intent = Intent(activity!!, TraceDetailsActivity::class.java)
                .putExtra(TIME_DATA_KEY, measuredTimeInMillis)
                .putExtras(bundle)
                .putExtra(MODE_DATA_KEY, false)
            startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, t.toString())
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupMiniMap() {

        miniMapView?.isClickable = false

        miniMap?.setOnMarkerClickListener(this)
        miniMap?.apply {
            isMyLocationEnabled = false
            isTrafficEnabled = false
        }
        miniMap?.uiSettings?.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = false
            isZoomGesturesEnabled = false
            isScrollGesturesEnabled = false
            isMapToolbarEnabled = false
        }

        val mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.style_json)
        miniMap?.setMapStyle(mapStyleOptions)

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val layer = KmlLayer(miniMap, downloadKmlFileAsync().await(), context)
                layer.addLayerToMap()
            } catch (ex: Exception) {
                Log.e(TAG, "downloadKmlFileAsync failed, reason: $ex")
            }
        }

        LocationServices.getFusedLocationProviderClient(activity!!)
            .lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val position = CameraPosition.Builder()
                        .target(LatLng(it.latitude, it.longitude))
                        .zoom(15.0f)
                        .build()
                    miniMap?.animateCamera(CameraUpdateFactory.newCameraPosition(position))
                }
            }


        val waterMark: View = miniMapView?.findViewWithTag("GoogleWatermark") ?: return
        waterMark.apply {
            scaleX = 0.5f
            scaleY = 0.5f
            translationX = -30f
            translationY = 15f
        }
    }

    private fun fillMap() {
        floatingMarkerTitlesOverlay?.setSource(miniMap)

        try {
            floatingMarkerTitlesOverlay?.clearMarkers()
            miniMap?.clear()
        } catch (ex: UninitializedPropertyAccessException) {
            Log.w("Google fullMap", "fillMap() invoked with uninitialized fullMap")
            return
        }

        for (i in 0 until gpsRouteSections.size) {
            miniMap?.placeMarker(gpsRouteSections[i].beginning.toLatLng(),
                resources,
                R.drawable.circle_marker,
                R.color.color_primary_dark)
            miniMap?.placeMarker(gpsRouteSections[i].end.toLatLng(),
                resources,
                R.drawable.circle_marker,
                R.color.color_primary_dark)
        }

        if (gpsRouteSections.isNotEmpty()) {
            miniMap?.placeMarker(gpsRouteSections.last().end.toLatLng(),
                resources,
                R.drawable.gps_user_location,
                R.color.color_primary_dark)

            floatingMarkerTitlesOverlay?.addMarker(UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
                MarkerInfo(gpsRouteSections.last().end.toLatLng(),
                    "GPS",
                    Color.BLACK))

            miniMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsRouteSections.last().end.toLatLng(),
                18f))
        }

        for (i in 0 until scapeRouteSections.size) {
            miniMap?.placeMarker(scapeRouteSections[i].beginning.toLatLng(),
                resources,
                R.drawable.circle_marker,
                R.color.scape_color)
            miniMap?.placeMarker(scapeRouteSections[i].end.toLatLng(),
                resources,
                R.drawable.circle_marker,
                R.color.scape_color)
        }

        if (scapeRouteSections.isNotEmpty()) {
            miniMap?.placeMarker(scapeRouteSections.last().end.toLatLng(),
                resources,
                R.drawable.gps_user_location,
                R.color.scape_color)

            floatingMarkerTitlesOverlay?.addMarker(UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
                MarkerInfo(scapeRouteSections.last().end.toLatLng(),
                    "SCAPE",
                    Color.BLACK))

            miniMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(scapeRouteSections.last().end.toLatLng(),
                18f))
        }
    }

    /**
     * In continuous mode display indefinite Snackbar until scape measurements are found.
     * In single mode display short-lived Snackbar.
     */
    private fun showErrorMessage() {
        lateinit var errMessage: String

        when(scapeSessionState) {
            ScapeSessionState.AUTHENTICATION_ERROR,
            ScapeSessionState.NETWORK_ERROR -> {
                errMessage = getString(R.string.localization_network_error)
            }
            ScapeSessionState.LOCKING_POSITION_ERROR -> {
                errMessage = if (scapeMeasurementsStatus == ScapeMeasurementsStatus.UNAVAILABLE_AREA)
                    getString(R.string.localization_unavailable_area)
                else
                    getString(R.string.localization_error)
            }
            ScapeSessionState.NO_ERROR                                              -> {
                if (scapeConfidenceScore < MIN_SCAPE_CONFIDENCE_SCORE) {
                    errMessage = getString(R.string.localization_error)
                }
            }
            ScapeSessionState.LOCATION_SENSORS_ERROR,
            ScapeSessionState.MOTION_SENSORS_ERROR                                  -> {
                errMessage = getString(R.string.localization_sensors_error)
            }
            ScapeSessionState.IMAGE_SENSORS_ERROR                                   -> {
                errMessage = getString(R.string.localization_image_error)
            }
            ScapeSessionState.UNEXPECTED_ERROR                                      -> {
                errMessage = getString(R.string.localization_generic_error)
            }
        }

        if (isContinuousModeEnabled) {
            if (errorMessageSnackBar != null && errorMessageSnackBar?.isShown!!) {
                errorMessageSnackBar?.updateSnackBarText(errMessage)
            }
            else {
                errorMessageSnackBar = container.showSnackbar(errMessage,
                    R.color.red,
                    Snackbar.LENGTH_INDEFINITE)
            }
        }
        else {
            container.showSnackbar(errMessage,
                R.color.red,
                SNACKBAR_DURATION_LONG
            )
        }
    }

    // endregion

    // region Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // start client here to ensure that we have GPS updates as soon as possible
        TrackTraceManager.sharedInstance(context!!).scapeClient?.start({}, { error -> Log.e("CameraFragment", error)})

        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()

        miniMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()

        miniMapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        miniMapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        miniMapView?.onLowMemory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        EventBus.getDefault().unregister(this)

        TrackTraceManager.sharedInstance(activity!!)?.scapeClient?.stop({}, {})

        viewFinder.removeFrameProcessor(frameProcessor)

        // clean up broadcast receiver
        try {
            activity!!.unregisterReceiver(networkConnectivityReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("TrackTraceFrag", "Network receiver exception $e")
        }

        // clean up viewpager
        main_tab_view.resetViewPager(view_pager)
        view_pager.removeOnPageChangeListener(this)
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        container = view as RelativeLayout

        miniMapView = container.findViewById(R.id.mini_map_view)
        miniMapView?.onCreate(savedInstanceState)

        miniMapView?.getMapAsync(this)
        MapsInitializer.initialize(activity as MainActivity)

        floatingMarkerTitlesOverlay = container.findViewById(R.id.map_floating_markers_overlay)

        viewFinder = container.findViewById(R.id.view_finder)

        // Wait for the views to be properly laid out
        viewFinder.post {
            // Build UI controls and bind all camera use cases
            views()

            bindings()
        }

        val networkIntentFilter = IntentFilter().apply {
            addAction(BROADCAST_CONNECTIVITY_OFF)
            addAction(BROADCAST_CONNECTIVITY_ON)
        }
        activity?.registerReceiver(networkConnectivityReceiver, networkIntentFilter)
    }

    // endregion Fragment

    // region onPageChangeListener

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        val currentContext = context ?: return

        val colorWhite = ContextCompat.getColor(currentContext, R.color.color_white)

        when (position) {
            0 -> {
                view_finder.visibility = View.GONE
                full_map_view.visibility = View.GONE

                main_view.setBackgroundColor(colorWhite)
                //while paage scroll to screen 1 positionoffset go up, so alpha go down
                main_view.alpha = (1 - positionOffset)

                // hide all play/pause/stop buttons, timer and minimap when not on main screen
                play_timer_button.hide()
                pause_timer_button.hide()
                stop_timer_button.visibility = View.GONE

                time.visibility = View.GONE
                card_view_minimap_container.visibility = View.GONE
            }
            1 -> {
                view_finder?.visibility = View.VISIBLE
                full_map_view.visibility = View.GONE

                main_view.setBackgroundColor(colorWhite)
                //default posotionOfSet is 0, while page scroll to 2, go up, so aplha up
                main_view.alpha = positionOffset

                card_view_minimap_container.visibility = View.VISIBLE

                when (timerState) {
                    TimerState.Idle   -> {
                        if(!view_switch_bottom.isOn) return

                        play_timer_button.show()

                        time.visibility = View.VISIBLE
                    }
                    TimerState.Paused -> {
                        play_timer_button.show()
                        stop_timer_button.visibility = View.VISIBLE

                        time.visibility = View.VISIBLE
                    }
                    TimerState.Running -> {
                        pause_timer_button.show()

                        time.visibility = View.VISIBLE
                    }
                }
            }
            2-> {
                view_finder?.visibility = View.GONE

                // hide all play/pause/stop buttons, timer and minimap when not on main screen
                play_timer_button.hide()
                pause_timer_button.hide()
                stop_timer_button.visibility = View.GONE

                time.visibility = View.GONE
                card_view_minimap_container.visibility = View.GONE

                full_map_view.visibility = View.VISIBLE
            }
        }
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageSelected(position: Int) {

    }

    // endregion

    // region GoogleMap

    override fun onMarkerClick(marker: Marker?) = false

    override fun onMapReady(map: GoogleMap?) {
        miniMap = map

        setupMiniMap()
    }

    // endregion

}