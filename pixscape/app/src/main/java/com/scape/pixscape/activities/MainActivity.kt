package com.scape.pixscape.activities

import android.app.ActionBar
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomSheetDialog
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.github.ybq.android.spinkit.style.Circle
import com.github.ybq.android.spinkit.style.DoubleBounce
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.GoogleMap.OnMapLoadedCallback
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.CameraPosition
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.scape.pixscape.PixscapeApp
import com.scape.pixscape.R
import com.scape.pixscape.helpers.GoogleMapAnimationHelper
import com.scape.pixscape.helpers.GoogleMapHelper
import com.scape.pixscape.utils.NetworkChangeReceiver
import com.scape.scapekit.*
import com.scape.scapekit.helper.PermissionHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_map_type.*

typealias ScapeLatLng = com.scape.scapekit.LatLng

/**
 * This is a simple example that shows how to integrate ScapeKit SDK in an application
 * that uses Google Maps to display current location, requested on demand by the user.
 *
 * The View is split into: a Camera preview and Maps view.
 */
class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnMapLoadedCallback, ScapeSessionObserver {

    companion object {
        private const val TAG = "MainActivity"
        private const val ZOOM_IN_LEVEL = 18f
        private const val TILT_LEVEL = 45f
        private const val NO_TILT_LEVEL = 0f
        private const val DELAY_LOCALIZATION_FEEDBACK:Long = 2000
        private const val DELAY_ENABLE_LOCALIZATION: Long = 7000
    }

    private lateinit var mMap: GoogleMap
    var previousGPSMarkers: ArrayList<Marker> = ArrayList(10)
    var previousScapeMarkers: ArrayList<Marker> = ArrayList(10)

    var latestRawLatLng: LatLng? = null

    private var scapeSession: ScapeSession? = null
    private lateinit var scapeClient: ScapeClient

    private lateinit var connectivityReceiver: NetworkChangeReceiver

    private var isLoading = false
    private var doubleBounce: DoubleBounce = DoubleBounce()
    private var is2DEnabled = true

    private var isScapeReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initActivityIndicator()

        scapeClient = PixscapeApp.sharedInstance.scapeClient

        connectivityReceiver = NetworkChangeReceiver()

        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()

        if (PermissionHelper.checkPermissions(this).isEmpty() && !scapeClient.isStarted) {
            Log.d(TAG, "onResume")
            scapeClient.start(clientStarted = {
                Log.i(TAG, "ScapeClient started after resume")

            }, clientFailed = {
                Log.i(TAG, "ScapeClient failed to start after resume $it")
                displayToast(it)
            })


            // resume Camers
        }

        registerConnectivityReceiver()
    }

    override fun onPause() {
        super.onPause()

        scapeClient.stop({}, {})

        // pause camera
    }

    override fun onDestroy() {

        unregisterConnectivityReceiver()

        scapeClient.terminate({}, {})

        super.onDestroy()
    }

    /**
     * Check if permissions required by ScapeKit have been granted and prompt the user to grant the ones that haven't been granted yet.
     */
    private fun checkAndRequestPermissions() {
        val deniedPermissions = PermissionHelper.checkPermissions(this)

        if (deniedPermissions.isEmpty()) {
            initScapeClient()
        }
        else {
            PermissionHelper.requestPermissions(this, deniedPermissions)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (PermissionHelper.checkPermissions(this).isEmpty()) {
            initScapeClient()
        }

        PermissionHelper.processResult(this, requestCode, permissions, grantResults)
    }

    // region Connectivity
    private fun registerConnectivityReceiver() {
        val netFilter = IntentFilter()
        netFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")

        this.registerReceiver(connectivityReceiver, netFilter)
    }

    private fun unregisterConnectivityReceiver() {
        try {
            this.unregisterReceiver(connectivityReceiver)
        }
        catch (e: IllegalArgumentException) {
        }
    }

    // endregion Connectivity

    private fun initScapeClient() {

        scapeClient.start(clientFailed = {
            Log.d(TAG, "clientFailed $it")
        }, clientStarted = {
            scapeSession = scapeClient.scapeSession

            setupCamera()

            initMap()

            localize_button.setOnClickListener{
                toggleLocalizeInProgress(true)
                scapeSession?.getMeasurements(this)
            }
        })
    }

    private fun initActivityIndicator() {
        doubleBounce.bounds = Rect(0,0,100,100)
        doubleBounce.color = ContextCompat.getColor(this, R.color.colorScapeDark0)
        progress_bar.indeterminateDrawable = doubleBounce

        doubleBounce.start()

        Handler().postDelayed({
            progress_text.visibility = View.INVISIBLE
            localize_button.visibility = View.VISIBLE
        }, DELAY_ENABLE_LOCALIZATION)
    }

    @SuppressWarnings("unused")
    private fun setupCircleActivityIndicator() {
        val circleDrawable = Circle()
        circleDrawable.bounds = Rect(0, 0, 200, 200)
        circleDrawable.color = ContextCompat.getColor(this, R.color.colorScapeDark0)
        progress_bar.indeterminateDrawable = circleDrawable
    }

    private fun setupCamera() {
        scapeSession = scapeClient.scapeSession

        // use Camera preview
    }

    private fun delayActivityIndicatorDismiss() {

        Handler().postDelayed({
            localize_button.visibility = View.VISIBLE
            progress_bar.visibility = View.INVISIBLE
            progress_text.visibility = View.INVISIBLE

        }, DELAY_LOCALIZATION_FEEDBACK)
    }

    private fun toggleLocalizeInProgress(isLoading: Boolean) {
        this.isLoading = isLoading

        if (!isLoading) {
            Log.d(TAG, "Localize finished")
        }
        else {
            Log.d(TAG, "Localize in progress")
            progress_text.text = getString(R.string.localisation_in_progress)
            doubleBounce.color = ContextCompat.getColor(this, R.color.colorScapeDark0)

            localize_button.visibility = View.INVISIBLE
            progress_bar.visibility = View.VISIBLE
            progress_text.visibility = View.VISIBLE
        }
    }

    // region ScapeSessionObserver - VPS updates
    override fun onScapeMeasurementsRequested(p0: ScapeSession?, p1: Double) {
    }

    override fun onDeviceMotionMeasurementsUpdated(p0: ScapeSession?, measurements: MotionMeasurements?) {
        Log.d(TAG, "onDeviceMotionMeasurementsUpdated $measurements")

    }

    override fun onScapeMeasurementsUpdated(p0: ScapeSession?, measurements: ScapeMeasurements?) {
        Log.d(TAG, "onScapeMeasurementsUpdated $measurements")

        runOnUiThread {
            toggleLocalizeInProgress(false)

            if (measurements != null) {
                when (measurements.measurementsStatus) {
                    ScapeMeasurementsStatus.RESULTS_FOUND -> {
                       progress_text.text = getString(R.string.localisation_ready)

                       delayActivityIndicatorDismiss()
                       addMarkerAndMoveMapToGeoPosition(measurements.latLng, true)
                    }
                    ScapeMeasurementsStatus.UNAVAILABLE_AREA -> {
                        progress_text.text = getString(R.string.Localisation_unavailable_area)
                        doubleBounce.color = Color.RED

                        delayActivityIndicatorDismiss()
                    }
                    ScapeMeasurementsStatus.NO_RESULTS -> {
                        progress_text.text = getString(R.string.localisation_error)
                        doubleBounce.color = Color.RED

                        delayActivityIndicatorDismiss()
                    }
                    ScapeMeasurementsStatus.INTERNAL_ERROR -> {
                        progress_text.text = getString(R.string.localisation_generic_error)
                        doubleBounce.color = Color.RED

                        delayActivityIndicatorDismiss()
                    }
                }
            }
        }
    }

    override fun onDeviceLocationMeasurementsUpdated(p0: ScapeSession?, measurements: LocationMeasurements?) {
        Log.d(TAG, "onDeviceLocationMeasurementsUpdated $measurements")

        measurements?.let {
            runOnUiThread{
                addMarkerAndMoveMapToGeoPosition(it.latLng, false)
            }

            measurements.latLng?.let {
                latestRawLatLng = LatLng(it.latitude, it.longitude)
            }
        }
    }

    override fun onCameraTransformUpdated(p0: ScapeSession?, p1: ArrayList<Double>?) {

    }

    override fun onScapeSessionError(scapeSession: ScapeSession?, scapeSessionState: ScapeSessionState, errMessage: String) {
        Log.d(TAG, "onScapeSessionError $scapeSessionState $errMessage")

        runOnUiThread {
            toggleLocalizeInProgress(false)

            progress_text.text = if (scapeSessionState == ScapeSessionState.LOCATION_SENSORS_ERROR)
                                        getString(R.string.localisation_sensors_error)
                                    else getString(R.string.localisation_error)
            doubleBounce.color = Color.RED

            delayActivityIndicatorDismiss()
        }
    }
    // endregion ScapeSessionObserver


    // region Map controller

    /**
     * Obtain the SupportMapFragment and get notified when the map is ready to be used.
     */
    private fun initMap() {
        val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // should be done once map is ready
        setupMapTypeActions()
    }

    /**
     * Manipulates the map once available.
     *
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Scape's office.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady")

        mMap = googleMap
        mMap.setOnMapLoadedCallback(this)

        mMap.uiSettings.setAllGesturesEnabled(true)
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.isBuildingsEnabled = true

        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    /**
     * Add a marker in London.
     */
    fun animateCameraToStartPosition() {
        val london = LatLng(51.509954, 0.135169)
        val defMarker = mMap.addMarker(GoogleMapHelper.createMarker(london,
                false,
                "London",
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)))

        previousGPSMarkers.add(defMarker)

        // Move the camera over the start position.
        animateCameraToLatLng(london, is2DEnabled)
    }

    /**
     * Setup bottom sheet dialog for map view type.
     * Map defaults to Standard view.
     */
    fun setupMapTypeActions() {

        map_type_btn.setOnClickListener {
            showMapMenu()
        }
    }

    @SuppressWarnings("InflateParams")
    private fun showMapMenu() {
        val mapTypeDialog = BottomSheetDialog(this, R.style.PickerDialog)
        mapTypeDialog.setCanceledOnTouchOutside(true)
        val dialogView = layoutInflater.inflate(R.layout.dialog_map_type, null)
        mapTypeDialog.setContentView(dialogView)

        val window = mapTypeDialog.window
        window?.setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)

        mapTypeDialog.show()

        val btn1 = dialogView.findViewById<Button>(R.id.standard_btn)
        val btn2 = dialogView.findViewById<Button>(R.id.satellite_btn)
        val btn3 = dialogView.findViewById<Button>(R.id.hybrid_btn)

        btn1.setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            mapTypeDialog.dismiss()
        }
        btn2.setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            mapTypeDialog.dismiss()
        }
        btn3.setOnClickListener {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            mapTypeDialog.dismiss()
        }
    }

    private fun transitionMenuUp() {
        hybrid_btn.animate().translationY(-55f)
        satellite_btn.animate().translationY(-105f)
        standard_btn.animate().translationY(-155f)
    }

    override fun onMapLoaded() {
        Log.d(TAG, "onMapLoaded")
    }

    /**
     * Remove latest Map Marker and add a new one with the new position and move the camera to the Scape measurements.
     */
    private fun addMarkerAndMoveMapToGeoPosition(measurements: ScapeLatLng?, isScapeMeasurements: Boolean) {

        measurements?.let {
            val currentLatLng =
                    LatLng(measurements.latitude, measurements.longitude)

            if (isScapeMeasurements) {
                if (previousScapeMarkers.isNotEmpty())
                    previousScapeMarkers[previousScapeMarkers.size - 1].remove()

                val marker = mMap.addMarker(GoogleMapHelper.createMarker(
                        currentLatLng,
                        false,
                        getString(R.string.scape),
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))

                previousScapeMarkers.add(marker)
                marker.showInfoWindow()

                animateCameraToLatLng(currentLatLng, is2DEnabled)
            }
            else {
                if (previousGPSMarkers.isNotEmpty())
                    previousGPSMarkers[previousGPSMarkers.size - 1].remove()

                val marker = mMap.addMarker(GoogleMapHelper.createMarker(
                        currentLatLng,
                        false,
                        getString(R.string.gps),
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))

                previousGPSMarkers.add(marker)
                marker.showInfoWindow()

                animateCameraToLatLng(currentLatLng, is2DEnabled)
            }
        }
    }

    private fun animateCameraToLatLng(latLng: LatLng, is2DEnabled: Boolean) {
        val pos: CameraPosition = GoogleMapAnimationHelper.createCameraPosition(
                latLng,
                ZOOM_IN_LEVEL,
                if (is2DEnabled) NO_TILT_LEVEL else TILT_LEVEL)

        val cameraUpdate = CameraUpdateFactory.newCameraPosition(pos)
        mMap.moveCamera(cameraUpdate)
        mMap.animateCamera(cameraUpdate)


    }
    // endregion Map controller

    /**
     * Display long toast  to notify the user of progress.
     *
     * @param message The toast message.
     */
    private fun displayToast(message: String) {
        runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show() }
    }

    // region Scene.OnUpdateListener
//    override fun onUpdate(p0: FrameTime?) {
//        var arFrame: Frame?
//
//        try {
//            arFrame = (sceneform_fragment as? ArFragment)?.arSceneView?.session?.update()
//            if(arFrame != null) {
//                scapeClient.scapeSession?.setARFrame(arFrame)
//            }
//        } catch (e: CameraNotAvailableException) {
//            Log.e("onUpdate", e.toString())
//        } catch (t: Throwable) {
//            Log.e("onUpdate", "Exception in Scene.OnUpdateListener $t")
//        }
//    }
    // endregion Scene.OnUpdateListener

}
