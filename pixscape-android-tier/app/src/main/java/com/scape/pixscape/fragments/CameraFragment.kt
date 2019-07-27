package com.scape.pixscape.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.scape.pixscape.PixscapeApplication
import com.scape.pixscape.R
import com.scape.pixscape.activities.TraceDetailsActivity
import com.scape.pixscape.adapter.ViewPagerAdapter
import com.scape.pixscape.models.dto.RouteSection
import com.scape.pixscape.services.TrackTraceService
import com.scape.pixscape.view.MainTabView
import com.scape.capture.preview.CapturePreviewObserver
import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.android.synthetic.main.view_pager_tabs.*

enum class TimerState { Idle, Running, Paused }

internal class CameraFragment : Fragment() {

    private lateinit var container: RelativeLayout
    private lateinit var viewFinder: SurfaceView

    private var mPreview: CapturePreviewObserver? = null

    private var sharedPref: SharedPreferences? = null

    companion object {
        const val BROADCAST_ACTION_TIME = "com.scape.pixscape.camerafragment.broadcastreceivertime"
        const val BROADCAST_ACTION_GPS_LOCATION = "com.scape.pixscape.camerafragment.broadcastreceivergpslocation"
        const val BROADCAST_ACTION_SCAPE_LOCATION = "com.scape.pixscape.camerafragment.broadcastreceiverscapelocation"
        const val BROADCAST_ACTION_STOP_TIMER = "com.scape.pixscape.camerafragment.broadcastreceiverstoptimer"
        const val TIME_DATA_KEY = "com.scape.pixscape.camerafragment.timedatakey"
        const val ROUTE_GPS_SECTIONS_DATA_KEY = "com.scape.pixscape.camerafragment.routegpssectionsdatakey"
        const val ROUTE_SCAPE_SECTIONS_DATA_KEY = "com.scape.pixscape.camerafragment.routescapesectionsdatakey"
        const val MODE_DATA_KEY = "com.scape.pixscape.camerafragment.modedatakey"

        private const val REQUEST_CHECK_SETTINGS = 2

        private var timerState = TimerState.Idle
        private var measuredTimeInMillis = 0L
        private var distanceInMeters = 0.0
        private var gpsRouteSections: List<RouteSection> = ArrayList()
        private var scapeRouteSections: List<RouteSection> = ArrayList()
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
                    context?.let {
                        distanceInMeters = gpsRouteSections.sumByDouble { section -> section.distance.toDouble() }

                        if (sharedPref == null) return
                    }
                }
                BROADCAST_ACTION_SCAPE_LOCATION -> {
                    if (activity == null) return
                    scapeRouteSections = intent.getParcelableArrayListExtra(TrackTraceService.ROUTE_SCAPE_SECTIONS_DATA_KEY)
                    context?.let {
                        // don't compute metrics for scape routes

                        if (sharedPref == null) return
                    }
                }
                BROADCAST_ACTION_STOP_TIMER   -> {
                    stopTimer()
                }
            }
        }
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes */
    @SuppressLint("RestrictedApi")
    private fun updateCameraUi() {
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

        val scapeColor = ContextCompat.getColor(currentContext, R.color.scapeColor)

        //listener to viewpager scroll page
        view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                when (position) {
                    0 -> {
                        main_view.setBackgroundColor(scapeColor)
                        //while paage scroll to screen 1 positionoffset go up, so alpha go down
                        main_view.alpha = (1 - positionOffset)

                        // hide all play/pause/stop buttons and timer when not on main screen
                        play_timer_button.hide()
                        pause_timer_button.hide()
                        stop_timer_button.hide()

                        time?.visibility = View.GONE
                    }
                    1 -> {
                        main_view.setBackgroundColor(scapeColor)
                        //default posotionOfSet is 0, while page scroll to 2, go up, so aplha up
                        main_view.alpha = positionOffset

                        when (timerState) {
                            TimerState.Idle   -> {
                                if(!view_switch_bottom.isChecked) return

                                play_timer_button.show()

                                time?.visibility = View.VISIBLE
                            }
                            TimerState.Paused -> {
                                play_timer_button.show()
                                stop_timer_button.show()

                                time?.visibility = View.VISIBLE
                            }
                            TimerState.Running -> {
                                pause_timer_button.show()

                                time?.visibility = View.VISIBLE
                            }
                        }
                    }
                    2-> {
                        // hide all play/pause/stop buttons and timer when not on main screen
                        play_timer_button.hide()
                        pause_timer_button.hide()
                        stop_timer_button.hide()

                        time?.visibility = View.GONE
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageSelected(position: Int) {}
        })

    }

    private fun stopTimer(){
        timerState = TimerState.Idle

        activity?.stopService(Intent(context, TrackTraceService::class.java))
        activity!!.unregisterReceiver(trackTraceBroadcastReceiver)

        @Suppress("UNCHECKED_CAST") val localGpsRouteSections = gpsRouteSections as ArrayList<Parcelable>
        @Suppress("UNCHECKED_CAST") val localScapeRouteSections = scapeRouteSections as ArrayList<Parcelable>

        gpsRouteSections = ArrayList()
        scapeRouteSections = ArrayList()

        //map.clear()

        time.base = SystemClock.elapsedRealtime()

        pause_timer_button.hide()
        stop_timer_button.hide()
        play_timer_button.show()

        view_switch_bottom.visibility = View.VISIBLE

        if (localGpsRouteSections.size < 1) return

        val intent = Intent(activity!!, TraceDetailsActivity::class.java)
                .putExtra(TIME_DATA_KEY, measuredTimeInMillis)
                .putParcelableArrayListExtra(ROUTE_GPS_SECTIONS_DATA_KEY, localGpsRouteSections)
                .putParcelableArrayListExtra(ROUTE_SCAPE_SECTIONS_DATA_KEY, localScapeRouteSections)
                .putExtra(MODE_DATA_KEY, false)
        startActivity(intent)
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

    private fun bindings() {
        play_timer_button.setOnClickListener {
            if(timerState == TimerState.Idle) {
                timerState = TimerState.Running

                play_timer_button.hide()

                pause_timer_button.show()

                view_switch_bottom.visibility = View.GONE

                val intentFilter = IntentFilter()
                intentFilter.addAction(BROADCAST_ACTION_GPS_LOCATION)
                intentFilter.addAction(BROADCAST_ACTION_SCAPE_LOCATION)
                intentFilter.addAction(BROADCAST_ACTION_TIME)
                intentFilter.addAction(BROADCAST_ACTION_STOP_TIMER)

                activity!!.registerReceiver(trackTraceBroadcastReceiver, intentFilter)

                val intent = Intent(context, TrackTraceService::class.java).putExtra(MainTabView.CONTINUOUS_MODE, true)
                ContextCompat.startForegroundService(context!!, intent)
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

        stop_timer_button.setOnLongClickListener {
            stopTimer()
            return@setOnLongClickListener true
        }
    }

    // region Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true

        // start client here to ensure that we have GPS updates as soon as possible
        PixscapeApplication.sharedInstance?.scapeClient?.start({}, { error -> Log.e("CameraFragment", error)})
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        container = view as RelativeLayout
        viewFinder = container.findViewById(R.id.view_finder)

        mPreview = CapturePreviewObserver(viewFinder, 0).apply { lifecycle.addObserver(this) }

        // Wait for the views to be properly laid out
        viewFinder.post {
            // Build UI controls and bind all camera use cases
            updateCameraUi()

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

}