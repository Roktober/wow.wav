package com.example.wowwaw

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.wowwaw.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.example.wowwaw.PermissionUtils.isPermissionGranted
import com.example.wowwaw.PermissionUtils.requestPermission
import com.example.wowwaw.map.pointDrawer
import com.example.wowwaw.services.FileSynchronizationService
import com.example.wowwaw.services.SoundDetectorService
import com.example.wowwaw.viewModels.MyViewModelFactory
import com.example.wowwaw.viewModels.SoundLocationViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import timber.log.Timber

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var fileSynchronizationService: FileSynchronizationService
    private lateinit var soundDetectorService: SoundDetectorService
    private lateinit var locationManager: LocationManager
    private lateinit var pointDrawer: pointDrawer

    private lateinit var pickFileButton: Button
    private var permissionDenied = false
    private lateinit var mMap: GoogleMap
    private val soundLocationViewModel: SoundLocationViewModel by viewModels {
        MyViewModelFactory(
            application
        )
    }

    @SuppressLint("MissingPermission")
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            if (service is FileSynchronizationService.LocalBinder) {
                fileSynchronizationService = service.getService()
            }
            if (service is SoundDetectorService.LocalBinder) {
                soundDetectorService = service.getService()
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, BuildConfig.LOCATION_REFRESH_MIN_TIME_MS.toLong(),
                    BuildConfig.LOCATION_REFRESH_MIN_DISTANCE_M.toFloat(),
                    soundDetectorService
                )
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
        pointDrawer = pointDrawer(mMap)
        subscribeSoundViewModel()
    }

    override fun onStart() {
        super.onStart()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        Intent(this, FileSynchronizationService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Intent(this, SoundDetectorService::class.java).also { intent ->
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        pickFileButton = findViewById(R.id.pick_file_btn)
        pickFileButton.setOnClickListener { onPickFileButtonClick(it) }

        startService(Intent(this, FileSynchronizationService::class.java))
        startService(Intent(this, SoundDetectorService::class.java))
    }

    private fun subscribeSoundViewModel() {
        soundLocationViewModel.data.observe(this, {
            pointDrawer.drawRandomColoredPoints(it)
        })
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    private fun onPickFileButtonClick(view: android.view.View) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        onSelectFileLauncher.launch(intent)
    }

    @SuppressLint("MissingPermission")
    private val onSelectFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resIntent: Intent? = result.data
                resIntent?.data?.let {
                    val currentLocation = locationManager.getLastKnownLocation("gps")
                    Timber.i("Try to get last know location: $currentLocation")
                    if (currentLocation != null) {
                        fileSynchronizationService.addFile(it, currentLocation)
                    }
                }
            } else {
                Timber.e("Cant select file, result: ${result.resultCode}")
            }
        }

    // LOCATION PERMISSION

    private fun showMissingPermissionError() {
        newInstance(true).show(supportFragmentManager, "dialog")
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            permissionDenied = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!::mMap.isInitialized) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
        }
    }

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
