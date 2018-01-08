package com.rosinante.firebasebinplaces

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.widget.Button
import android.widget.Toast

import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.places.Places
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

import java.util.HashMap

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick


class MapsActivity : FragmentActivity(), OnMapReadyCallback, ChildEventListener {


    @BindView(R.id.checkout_button) private var checkoutButton: Button? = null
    private var mMap: GoogleMap? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private val bounds = LatLngBounds.Builder()
    private var firebaseDatabase: FirebaseDatabase? = null
    private var databaseReference: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        checkPermission(applicationContext)
        ButterKnife.bind(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .build()
        mGoogleApiClient!!.connect()
        firebaseDatabase = FirebaseDatabase.getInstance()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        databaseReference = firebaseDatabase!!.reference
        databaseReference!!.child("checkout").addChildEventListener(this)
    }


    private fun addPointToViewPort(newPoint: LatLng) {
        bounds.include(newPoint)
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), checkoutButton!!.height))
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
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkoutButton!!.viewTreeObserver.addOnGlobalLayoutListener { mMap!!.setPadding(0, checkoutButton!!.height, 0, 0) }
        checkPermission(applicationContext)
        mMap!!.isMyLocationEnabled = true
        mMap!!.setOnMyLocationChangeListener { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            addPointToViewPort(latLng)
            mMap!!.setOnMyLocationChangeListener(null)
        }
    }

    @OnClick(R.id.checkout_button)
    fun onClick() {
        try {
            val intentBuilder = PlacePicker.IntentBuilder()
            val intent = intentBuilder.build(this)
            startActivityForResult(intent, REQUEST_PLACE_PICKER)
        } catch (e: GooglePlayServicesRepairableException) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, e.connectionStatusCode,
                    REQUEST_PLACE_PICKER)
        } catch (e: GooglePlayServicesNotAvailableException) {
            Toast.makeText(this, "Please install Google Play Services!", Toast.LENGTH_LONG).show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_PLACE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                val place = PlacePicker.getPlace(data, this)

                val checkoutData = HashMap<String, Any>()
                checkoutData.put("time", ServerValue.TIMESTAMP)
                checkoutData.put("location", place.address)

                databaseReference!!.child("checkout").child(place.id).setValue(checkoutData)

            } else if (resultCode == PlacePicker.RESULT_ERROR) {
                Toast.makeText(this, "Places API failure! Check the API is enabled for your key",
                        Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onChildAdded(dataSnapshot: DataSnapshot, s: String) {
        val placeId = dataSnapshot.key
        if (placeId != null) {
            Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId)
                    .setResultCallback { places ->
                        val location = places.get(0).latLng
                        addPointToViewPort(location)
                        mMap!!.addMarker(MarkerOptions().position(location))
                        places.release()
                    }
        }
    }

    override fun onChildChanged(dataSnapshot: DataSnapshot, s: String) {
        // TODO
    }

    override fun onChildRemoved(dataSnapshot: DataSnapshot) {
        // TODO
    }

    override fun onChildMoved(dataSnapshot: DataSnapshot, s: String) {
        // TODO
    }

    override fun onCancelled(firebaseError: DatabaseError) {
        // TODO
    }

    companion object {
        private val REQUEST_PLACE_PICKER = 1

        fun checkPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }
}
