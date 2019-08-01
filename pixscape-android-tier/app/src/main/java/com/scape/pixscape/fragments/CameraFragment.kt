package com.scape.pixscape.fragments

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.maps.*
import com.google.android.libraries.maps.model.*
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.scape.pixscape.PixscapeApplication
import com.scape.pixscape.R
import com.scape.pixscape.activities.MainActivity
import com.scape.pixscape.activities.TraceDetailsActivity
import com.scape.pixscape.adapter.ViewPagerAdapter
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.services.TrackTraceService
import com.scape.pixscape.services.TrackTraceService.Companion.SCAPE_ERROR_STATE_KEY
import com.scape.pixscape.utils.CameraIntrinsics
import com.scape.pixscape.utils.showSnackbar
import com.scape.scapekit.ScapeSessionState
import com.scape.scapekit.setByteBuffer
import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.view_pager_tabs.*
import java.nio.ByteBuffer
import kotlin.experimental.and

enum class TimerState { Idle, Running, Paused }

internal class CameraFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private var luma: ByteBuffer? = null
    private var cameraIntrinsics: CameraIntrinsics? = null

    private lateinit var container: RelativeLayout
    private lateinit var viewFinder: CameraView

    private var miniMapView: MapView? = null
    private var miniMap: GoogleMap? = null

    private var sharedPref: SharedPreferences? = null

    companion object {
        private var isContinuousModeEnabled: Boolean = false
        const val CONTINUOUS_MODE = "com.scape.pixscape.view.maintabview.continuousmode"

        const val BROADCAST_ACTION_TIME = "com.scape.pixscape.camerafragment.broadcastreceivertime"
        const val BROADCAST_ACTION_GPS_LOCATION = "com.scape.pixscape.camerafragment.broadcastreceivergpslocation"
        const val BROADCAST_ACTION_SCAPE_LOCATION = "com.scape.pixscape.camerafragment.broadcastreceiverscapelocation"
        const val BROADCAST_ACTION_STOP_TIMER = "com.scape.pixscape.camerafragment.broadcastreceiverstoptimer"
        const val TIME_DATA_KEY = "com.scape.pixscape.camerafragment.timedatakey"
        const val ROUTE_GPS_SECTIONS_DATA_KEY = "com.scape.pixscape.camerafragment.routegpssectionsdatakey"
        const val ROUTE_SCAPE_SECTIONS_DATA_KEY = "com.scape.pixscape.camerafragment.routescapesectionsdatakey"
        const val MODE_DATA_KEY = "com.scape.pixscape.camerafragment.modedatakey"

        private var timerState = TimerState.Idle
        private var measuredTimeInMillis = 0L
        private var distanceInMeters = 0.0
        private var gpsRouteSections: List<RouteSection> = ArrayList()
        private var scapeRouteSections: List<RouteSection> = ArrayList()
        private var scapeSessionState: ScapeSessionState = ScapeSessionState.NO_ERROR
        private var scapeSessionStateValues = ScapeSessionState.values()
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
            //Log.e("setByteBuffer", "$intrinsics $width $height")

            val scapeClient = PixscapeApplication.sharedInstance?.scapeClient
            scapeClient?.scapeSession?.setCameraIntrinsics(intrinsics.focalLengthX,
                                                           intrinsics.focalLengthY,
                                                           intrinsics.principalPointX,
                                                           intrinsics.principalPointY)
            scapeClient?.scapeSession?.setByteBuffer(luma, width, height)
        }
    }

    private val trackTraceBroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                BROADCAST_ACTION_TIME         -> {
                    measuredTimeInMillis = intent.getLongExtra(TrackTraceService.MILLIS_DATA_KEY, 0L)

                    if (measuredTimeInMillis == 0L) Log.w("Broadcastreceiver", "service returned time 0")
                    time.base = SystemClock.elapsedRealtime() - measuredTimeInMillis
                }
                BROADCAST_ACTION_GPS_LOCATION -> {
                    if (activity == null) return
                    gpsRouteSections = intent.getParcelableArrayListExtra(TrackTraceService.ROUTE_GPS_SECTIONS_DATA_KEY)

                    distanceInMeters = gpsRouteSections.sumByDouble { section -> section.distance.toDouble() }

                    fillMap()

                    if (sharedPref == null) return
                }
                BROADCAST_ACTION_SCAPE_LOCATION -> {
                    if (activity == null) return
                    scapeRouteSections = intent.getParcelableArrayListExtra(TrackTraceService.ROUTE_SCAPE_SECTIONS_DATA_KEY)

                    fillMap()

                    // if we have any scape error then the service will
                    // trigger BROADCAST_ACTION_STOP_TIMER which effectively stops the service,
                    // but if we have a succesful scape result then we need to stop the service here when in single mode
                    if(!isContinuousModeEnabled) {
                        stopTimerAndForegroundService()
                    }

                    // don't compute metrics for scape routes

                    if (sharedPref == null) return
                }
                BROADCAST_ACTION_STOP_TIMER   -> {
                    if(activity == null) return
                    scapeSessionState = scapeSessionStateValues[intent.getIntExtra(SCAPE_ERROR_STATE_KEY,
                                                                                   ScapeSessionState.NO_ERROR.ordinal)]

                    showErrorMessage()

                    stopTimerAndForegroundService()

                    if(isContinuousModeEnabled) {
                        startTrackTraceActivity()
                    }
                }
            }
        }
    }

    // region Private

    private fun showErrorMessage() {
        when(scapeSessionState) {
            ScapeSessionState.AUTHENTICATION_ERROR, ScapeSessionState.NETWORK_ERROR -> {
                container?.showSnackbar("Something went wrong with your internet connection, try again",
                                        R.color.red,
                                        4500)
            }
            ScapeSessionState.LOCKING_POSITION_ERROR -> {
                container?.showSnackbar("Could not lock your position, try again",
                                        R.color.red,
                                        4500)
            }
        }
    }

    private fun bindings() {
        view_camera_center.setOnClickListener {
            if (view_pager.currentItem != 1) {
                view_pager.currentItem = 1
            } else if(!view_switch_bottom.isChecked) { // we are not in the continous mode
                startForegroundTrackService(false)
            }
        }

        play_timer_button.setOnClickListener {
            if(timerState == TimerState.Idle) {
                timerState = TimerState.Running

                play_timer_button.hide()

                pause_timer_button.show()

                view_switch_bottom?.visibility = View.GONE

                startForegroundTrackService(true)

            } else if(timerState == TimerState.Paused) {
                timerState = TimerState.Running

                TrackTraceService.paused = false

                play_timer_button.hide()
                stop_timer_button.hide()

                pause_timer_button.show()
            }
        }

        pause_timer_button.setOnClickListener {
            TrackTraceService.paused = true

            pause_timer_button.hide()

            play_timer_button.show()
            stop_timer_button.show()

            timerState = TimerState.Paused
        }

        stop_timer_button.setOnClickListener {
            stopTimerAndForegroundService()

            startTrackTraceActivity()
        }
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes */
    @SuppressLint("RestrictedApi")
    private fun views() {
        // Inflate a new view containing all UI for controlling the camera
        val cameraUiContainer = View.inflate(requireContext(), R.layout.camera_ui_container, container)

        val supportFragmentManager = fragmentManager ?: return

        //viewpager custom adapter
        val adapter = ViewPagerAdapter(supportFragmentManager)
        view_pager.adapter = adapter

        // initialize view pager
        main_tab_view.setUpWithViewPager(view_pager)

        //set default
        view_pager.currentItem = 1

        val currentContext = context ?: return

        val scapeColor = ContextCompat.getColor(currentContext, R.color.scape_color)

        //listener to viewpager scroll page
        view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                when (position) {
                    0 -> {
                        main_view.setBackgroundColor(scapeColor)
                        //while paage scroll to screen 1 positionoffset go up, so alpha go down
                        main_view.alpha = (1 - positionOffset)

                        // hide all play/pause/stop buttons, timer and minimap when not on main screen
                        play_timer_button.hide()
                        pause_timer_button.hide()
                        stop_timer_button.hide()

                        time.visibility = View.GONE
                        card_view_minimap_container.visibility = View.GONE
                    }
                    1 -> {
                        main_view.setBackgroundColor(scapeColor)
                        //default posotionOfSet is 0, while page scroll to 2, go up, so aplha up
                        main_view.alpha = positionOffset

                        card_view_minimap_container.visibility = View.VISIBLE

                        when (timerState) {
                            TimerState.Idle   -> {
                                if(!view_switch_bottom.isChecked) return

                                play_timer_button.show()

                                time.visibility = View.VISIBLE
                            }
                            TimerState.Paused -> {
                                play_timer_button.show()
                                stop_timer_button.show()

                                time.visibility = View.VISIBLE
                            }
                            TimerState.Running -> {
                                pause_timer_button.show()

                                time.visibility = View.VISIBLE
                            }
                        }
                    }
                    2-> {
                        // hide all play/pause/stop buttons, timer and minimap when not on main screen
                        play_timer_button.hide()
                        pause_timer_button.hide()
                        stop_timer_button.hide()

                        time.visibility = View.GONE
                        card_view_minimap_container.visibility = View.GONE
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageSelected(position: Int) {}
        })

    }

    private fun startForegroundTrackService(isContinuousModeEnabled: Boolean) {
        if(!isContinuousModeEnabled) {
            view_switch_bottom?.visibility = View.GONE
            dots_view?.visibility = View.VISIBLE
            container?.showSnackbar("Localizing, please wait..", R.color.scape_blue, 2500)
        }

        CameraFragment.isContinuousModeEnabled = isContinuousModeEnabled

        val intentFilter = IntentFilter()
        intentFilter.addAction(BROADCAST_ACTION_GPS_LOCATION)
        intentFilter.addAction(BROADCAST_ACTION_SCAPE_LOCATION)
        intentFilter.addAction(BROADCAST_ACTION_TIME)
        intentFilter.addAction(BROADCAST_ACTION_STOP_TIMER)

        activity!!.registerReceiver(trackTraceBroadcastReceiver, intentFilter)

        val intent = Intent(context,
                            TrackTraceService::class.java).putExtra(CONTINUOUS_MODE,
                                                                    isContinuousModeEnabled)
        ContextCompat.startForegroundService(context!!, intent)
    }

    private fun stopForegroundTrackService() {
        activity?.stopService(Intent(context, TrackTraceService::class.java))

        activity?.unregisterReceiver(trackTraceBroadcastReceiver)
    }

    private fun stopTimerAndForegroundService() {
        timerState = TimerState.Idle

        time?.base = SystemClock.elapsedRealtime()

        pause_timer_button?.hide()
        stop_timer_button?.hide()

        dots_view?.visibility = View.GONE

        view_switch_bottom?.postDelayed({view_switch_bottom?.visibility = View.VISIBLE}, 2000)

        if(isContinuousModeEnabled) {
            play_timer_button?.show()
        }

        stopForegroundTrackService()
    }

    private fun startTrackTraceActivity() {
        try {
            @Suppress("UNCHECKED_CAST") val localGpsRouteSections = gpsRouteSections as ArrayList<Parcelable>
            @Suppress("UNCHECKED_CAST") val localScapeRouteSections = scapeRouteSections as ArrayList<Parcelable>

            gpsRouteSections = ArrayList()
            scapeRouteSections = ArrayList()

            if (localGpsRouteSections.isEmpty()) return

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
            Log.e("CameraFragment", t.toString())
        }
    }

    private fun restoreTimerState() {
        timerState = TimerState.values()[sharedPref!!.getInt(getString(R.string.timer_state), 0)]

        when (timerState) {
            TimerState.Idle    -> {
                time.base = SystemClock.elapsedRealtime()
            }
            TimerState.Running -> {
                time.base = SystemClock.elapsedRealtime() - measuredTimeInMillis

                play_timer_button.hide()
                pause_timer_button.show()
            }
            TimerState.Paused  -> {
                time.base = SystemClock.elapsedRealtime() - measuredTimeInMillis

                play_timer_button.show()
                stop_timer_button.show()
            }
        }
    }

    private fun vectorToBitmap(id: Int, color: Int) : BitmapDescriptor {
        val vectorDrawable = ResourcesCompat.getDrawable(resources, id, null)
        val bitmap = Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth,
                                         vectorDrawable.intrinsicHeight,
                                         Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        DrawableCompat.setTint(vectorDrawable, color)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun placeMarkerOnMap(location: LatLng, color: Int) {
        val markerOptions = MarkerOptions().position(location)
        markerOptions.icon(vectorToBitmap(R.drawable.gps_user_location, resources.getColor(color)))
        miniMap?.addMarker(markerOptions)
    }

    @SuppressLint("MissingPermission")
    private fun setupMiniMap() {
        miniMapView?.isClickable = false

        miniMap?.uiSettings?.isZoomControlsEnabled = false
        miniMap?.uiSettings?.isCompassEnabled = false
        miniMap?.uiSettings?.isZoomGesturesEnabled = false
        miniMap?.uiSettings?.isScrollGesturesEnabled = false
        miniMap?.uiSettings?.isMapToolbarEnabled = false
        miniMap?.isMyLocationEnabled = false
        miniMap?.isTrafficEnabled = false

        miniMap?.setOnMarkerClickListener(this)

        val mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.style_json)
        miniMap?.setMapStyle(mapStyleOptions)

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
        waterMark.scaleX = 0.5f
        waterMark.scaleY = 0.5f
        waterMark.translationX = -30f
        waterMark.translationY = 15f
    }

    private fun fillMap() {
        try {
            miniMap?.clear()
        } catch (ex: UninitializedPropertyAccessException) {
            Log.w("Google fullMap", "fillMap() invoked with uninitialized fullMap")
            return
        }

        for (i in 0 until gpsRouteSections.size) {
            miniMap?.addPolyline(PolylineOptions().add(gpsRouteSections[i].beginning.toLatLng(),
                                                       gpsRouteSections[i].end.toLatLng())
                                        .color(ContextCompat.getColor(context!!, R.color.text_color_black))
                                        .width(10f))
        }

        if (gpsRouteSections.isNotEmpty()) {
            placeMarkerOnMap(gpsRouteSections.last().end.toLatLng(), R.color.color_primary_dark)

            miniMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsRouteSections.last().end.toLatLng(),
                                                                     18f))
        }

        for (i in 0 until scapeRouteSections.size) {
            miniMap?.addPolyline(PolylineOptions().add(scapeRouteSections[i].beginning.toLatLng(),
                                                      scapeRouteSections[i].end.toLatLng())
                                        .color(ContextCompat.getColor(context!!, R.color.scape_color))
                                        .width(10f))
        }

        if (scapeRouteSections.isNotEmpty()) {
            placeMarkerOnMap(scapeRouteSections.last().end.toLatLng(), R.color.scape_color)

            miniMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(scapeRouteSections.last().end.toLatLng(),
                                                                     18f))
        }
    }

    // endregion

    // region Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // start client here to ensure that we have GPS updates as soon as possible
        PixscapeApplication.sharedInstance?.scapeClient?.start({}, { error -> Log.e("CameraFragment", error)})
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

        PixscapeApplication.sharedInstance?.scapeClient?.stop({}, {})

        try {
            activity!!.unregisterReceiver(trackTraceBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("TrackTraceFrag", e.toString())
        }

        with(sharedPref!!.edit()) {
            putInt(getString(R.string.timer_state), timerState.ordinal)
            commit()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        container = view as RelativeLayout

        miniMapView = container.findViewById(R.id.mini_map_view)
        miniMapView?.onCreate(savedInstanceState)

        miniMapView?.getMapAsync(this)
        MapsInitializer.initialize(activity as MainActivity)

        viewFinder = container.findViewById(R.id.view_finder)
        viewFinder.setLifecycleOwner(this)

        viewFinder.addFrameProcessor(frameProcessor)

        // Wait for the views to be properly laid out
        viewFinder.post {
            // Build UI controls and bind all camera use cases
            views()

            restoreTimerState()

            bindings()
        }

        sharedPref = activity?.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val intentFilter = IntentFilter()
        intentFilter.addAction(BROADCAST_ACTION_GPS_LOCATION)
        intentFilter.addAction(BROADCAST_ACTION_SCAPE_LOCATION)
        intentFilter.addAction(BROADCAST_ACTION_TIME)
        activity!!.registerReceiver(trackTraceBroadcastReceiver, intentFilter)
    }

    // endregion Fragment

    // region GoogleMap

    override fun onMarkerClick(marker: Marker?) = false

    override fun onMapReady(map: GoogleMap?) {
        miniMap = map

        setupMiniMap()
    }

    // endregion

}