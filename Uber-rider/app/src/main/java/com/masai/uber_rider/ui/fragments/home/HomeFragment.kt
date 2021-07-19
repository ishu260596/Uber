package com.masai.uber_rider.ui.fragments.home

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.masai.uber_rider.R
import com.masai.uber_rider.callback.FirebaseDriverInfoListener
import com.masai.uber_rider.callback.FirebaseFailedListener
import com.masai.uber_rider.databinding.FragmentHomeBinding
import com.masai.uber_rider.remote.ApiClient
import com.masai.uber_rider.remote.IGoogleApi
import com.masai.uber_rider.remote.NetworkRetrofit
import com.masai.uber_rider.remote.RetrofitClient
import com.masai.uber_rider.ui.activities.RequestDriverActivity
import com.masai.uber_rider.ui.activities.ui.home.HomeViewModel
import com.masai.uber_rider.utils.Common
import com.masai.uber_rider.utils.EventBus.SelectedPlaceEvent
import com.masai.uber_rider.utils.models.AnimationModel
import com.masai.uber_rider.utils.models.DriverGeoModel
import com.masai.uber_rider.utils.models.DriverInfoModel
import com.masai.uber_rider.utils.models.GeoQueryModel
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class HomeFragment : Fragment(), OnMapReadyCallback, FirebaseDriverInfoListener {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //Location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var slidingUp: SlidingUpPanelLayout
    private lateinit var tvWelcome: TextView
    private lateinit var autoCompleteSupportMapFragment: AutocompleteSupportFragment

    // Load Driver
    private var distance = 1.0
    private val LIMIT_RANGE = 10.0
    private var previousLocation: Location? = null
    private var currentLocation: Location? = null

    private var firstTime = true

    //geoFire
    private var geoFire: GeoFire? = null
    private lateinit var driverLocationRef: DatabaseReference

    //Listener
    lateinit var iFirebaseDriverInfoListener: FirebaseDriverInfoListener
    lateinit var iFirebaseFailedListener: FirebaseFailedListener

    private var cityName: String? = null

    private var compositeDisposable: CompositeDisposable? = null
    private var iGoogleApi: IGoogleApi? = null
    private var apiClient: ApiClient? = null

    override fun onStop() {
        compositeDisposable?.clear()
        super.onStop()
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        RxJavaPlugins.setErrorHandler(Timber::e)

        init()
        initViews(root)

        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return root
    }

    private fun initViews(root: View) {

        slidingUp = root.findViewById(R.id.activity_main) as SlidingUpPanelLayout
        tvWelcome = root.findViewById(R.id.txt_welcome)
        Common.setWelcomeMessage(tvWelcome)
    }

    private fun init() {

        Places.initialize(requireContext(), getString(R.string.google_maps_key))
        autoCompleteSupportMapFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment
        autoCompleteSupportMapFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )
        autoCompleteSupportMapFragment.setHint("Where to")
        autoCompleteSupportMapFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(p0: Place) {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Snackbar.make(
                        requireView(),
                        "Required location permission", Snackbar.LENGTH_SHORT
                    ).show()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                    val origin = LatLng(it.latitude, it.longitude)
                    val destination = LatLng(p0.latLng!!.latitude, p0.latLng!!.longitude)
                    startActivity(Intent(requireContext(), RequestDriverActivity::class.java))
                    EventBus.getDefault().postSticky(SelectedPlaceEvent(origin, destination))

                }
            }

            override fun onError(p0: Status) {
                Snackbar.make(
                    requireView(),
                    " " + p0.statusMessage, Snackbar.LENGTH_SHORT
                ).show()
            }

        })

        iGoogleApi = RetrofitClient.getInstanceK()!!.create(IGoogleApi::class.java)
        apiClient = NetworkRetrofit.getInstanceRe()!!.create(ApiClient::class.java)

        iFirebaseDriverInfoListener = this

        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f
        locationRequest.interval = 5000

        locationCallback = object : LocationCallback() {
            //ctrl+o
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                val newPos = LatLng(p0.lastLocation.latitude, p0.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

                // If user has change location, calculate and load driver again
                if (firstTime) {
                    previousLocation = p0.lastLocation
                    currentLocation = p0.lastLocation

                    setRestrictedPlaceInCountry(p0.lastLocation)

                    firstTime = false
                } else {
                    previousLocation = currentLocation
                    currentLocation = p0.lastLocation
                }
                if (previousLocation!!.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE)
                    loadAvailableDriver()

            }
        }

        fusedLocationProviderClient = LocationServices
            .getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationProviderClient
            .requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper()!!)

        loadAvailableDriver()

    }

    private fun setRestrictedPlaceInCountry(lastLocation: Location) {
        try {
            val geoCoder = Geocoder(requireContext(), Locale.getDefault())
            var addressList: List<Address> = ArrayList()
            addressList = geoCoder.getFromLocation(lastLocation.latitude, lastLocation.longitude, 1)
            if (addressList.size > 0) {
                autoCompleteSupportMapFragment.setCountry(addressList[0].countryCode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAvailableDriver() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
//            Snackbar.make(requireView(), "Permission Location Required", Snackbar.LENGTH_SHORT)
//                .show()

            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { location ->
                // load all driver in city
                val geoCoder = Geocoder(requireContext(), Locale.getDefault())
                var addressList: List<Address> = ArrayList()
                try {
                    addressList = geoCoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addressList.isNotEmpty()) {
                        cityName = addressList[0].locality
                    }

                    if (!TextUtils.isEmpty(cityName)) {
                        //Query
                        driverLocationRef = FirebaseDatabase.getInstance()
                            .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                            .child(cityName!!)

                        geoFire = GeoFire(driverLocationRef)
                        val geoQuery = geoFire!!.queryAtLocation(
                            GeoLocation(
                                location.latitude,
                                location.longitude
                            ), distance
                        )
                        geoQuery.removeAllListeners()
                        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
                            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                                //Common.driversFound.add(DriverGeoModel(key!!, location!!))
                                if (!Common.driversFound.containsKey(key)) {
                                    Common.driversFound[key!!] = DriverGeoModel(key, location)
                                }
                            }

                            override fun onKeyExited(key: String?) {

                            }

                            override fun onKeyMoved(key: String?, location: GeoLocation?) {

                            }

                            override fun onGeoQueryReady() {
                                if (distance <= LIMIT_RANGE) {
                                    distance++
                                    loadAvailableDriver()
                                } else {
                                    distance = 0.0
                                    addDriverMarker()
                                }
                            }

                            override fun onGeoQueryError(error: DatabaseError?) {
                                Snackbar.make(requireView(), error!!.message, Snackbar.LENGTH_SHORT)
                                    .show()
                            }

                        })

                        driverLocationRef.addChildEventListener(object : ChildEventListener {
                            override fun onChildAdded(
                                snapshot: DataSnapshot,
                                previousChildName: String?
                            ) {
                                // Have new driver
                                val geoQueryModel = snapshot.getValue(GeoQueryModel::class.java)
                                val geoLocation =
                                    GeoLocation(geoQueryModel!!.l!![0], geoQueryModel!!.l!![1])
                                val driverGeoModel = DriverGeoModel(snapshot.key, geoLocation)
                                val newDriverLocation = Location("")
                                newDriverLocation.latitude = geoLocation.latitude
                                newDriverLocation.longitude = geoLocation.longitude
                                val newDistance = location.distanceTo(newDriverLocation) / 1000

                                if (newDistance <= LIMIT_RANGE) {
                                    findDriverByKey(driverGeoModel)
                                }


                            }

                            override fun onChildChanged(
                                snapshot: DataSnapshot,
                                previousChildName: String?
                            ) {

                            }

                            override fun onChildRemoved(snapshot: DataSnapshot) {

                            }

                            override fun onChildMoved(
                                snapshot: DataSnapshot,
                                previousChildName: String?
                            ) {

                            }

                            override fun onCancelled(error: DatabaseError) {
                                Snackbar.make(
                                    requireView(),
                                    error!!.message, Snackbar.LENGTH_SHORT
                                )
                                    .show()
                            }

                        })
                    } else {
                        //Query
                        driverLocationRef = FirebaseDatabase.getInstance()
                            .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                            .child("Hamirpur")

                        geoFire = GeoFire(driverLocationRef)
                        val geoQuery = geoFire!!.queryAtLocation(
                            GeoLocation(
                                location.latitude,
                                location.longitude
                            ), distance
                        )
                        geoQuery.removeAllListeners()
                        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
                            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                                //Common.driversFound.add(DriverGeoModel(key!!, location!!))
                                if (!Common.driversFound.containsKey(key)) {
                                    Common.driversFound[key!!] = DriverGeoModel(key, location)
                                }
                            }

                            override fun onKeyExited(key: String?) {

                            }

                            override fun onKeyMoved(key: String?, location: GeoLocation?) {

                            }

                            override fun onGeoQueryReady() {
                                if (distance <= LIMIT_RANGE) {
                                    distance++
                                    loadAvailableDriver()
                                } else {
                                    distance = 0.0
                                    addDriverMarker()
                                }
                            }

                            override fun onGeoQueryError(error: DatabaseError?) {
                                Snackbar.make(requireView(), error!!.message, Snackbar.LENGTH_SHORT)
                                    .show()
                            }

                        })

                        driverLocationRef.addChildEventListener(object : ChildEventListener {
                            override fun onChildAdded(
                                snapshot: DataSnapshot,
                                previousChildName: String?
                            ) {
                                // Have new driver
                                val geoQueryModel = snapshot.getValue(GeoQueryModel::class.java)
                                val geoLocation =
                                    GeoLocation(geoQueryModel!!.l!![0], geoQueryModel!!.l!![1])
                                val driverGeoModel = DriverGeoModel(snapshot.key, geoLocation)
                                val newDriverLocation = Location("")
                                newDriverLocation.latitude = geoLocation.latitude
                                newDriverLocation.longitude = geoLocation.longitude
                                val newDistance = location.distanceTo(newDriverLocation) / 1000

                                if (newDistance <= LIMIT_RANGE) {
                                    findDriverByKey(driverGeoModel)
                                }


                            }

                            override fun onChildChanged(
                                snapshot: DataSnapshot,
                                previousChildName: String?
                            ) {

                            }

                            override fun onChildRemoved(snapshot: DataSnapshot) {

                            }

                            override fun onChildMoved(
                                snapshot: DataSnapshot,
                                previousChildName: String?
                            ) {

                            }

                            override fun onCancelled(error: DatabaseError) {
                                Snackbar.make(
                                    requireView(),
                                    error!!.message, Snackbar.LENGTH_SHORT
                                )
                                    .show()
                            }

                        })
                    }
                    /** if (cityName != null) {
                    //Query
                    driverLocationRef = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                    .child(cityName!!)

                    } else {
                    //Query
                    driverLocationRef = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                    .child("Hamirpur")
                    }

                    geoFire = GeoFire(driverLocationRef)
                    val geoQuery = geoFire!!.queryAtLocation(
                    GeoLocation(
                    location.latitude,
                    location.longitude
                    ), distance
                    )
                    geoQuery.removeAllListeners()
                    geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
                    override fun onKeyEntered(key: String?, location: GeoLocation?) {
                    Common.driversFound.add(DriverGeoModel(key!!, location!!))
                    }

                    override fun onKeyExited(key: String?) {

                    }

                    override fun onKeyMoved(key: String?, location: GeoLocation?) {

                    }

                    override fun onGeoQueryReady() {
                    if (distance <= LIMIT_RANGE) {
                    distance++
                    loadAvailableDriver()
                    } else {
                    distance = 0.0
                    addDriverMarker()
                    }
                    }

                    override fun onGeoQueryError(error: DatabaseError?) {
                    Snackbar.make(requireView(), error!!.message, Snackbar.LENGTH_SHORT)
                    .show()
                    }

                    })

                    driverLocationRef.addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                    ) {
                    // Have new driver
                    val geoQueryModel = snapshot.getValue(GeoQueryModel::class.java)
                    val geoLocation =
                    GeoLocation(geoQueryModel!!.l!![0], geoQueryModel!!.l!![1])
                    val driverGeoModel = DriverGeoModel(snapshot.key, geoLocation)
                    val newDriverLocation = Location("")
                    newDriverLocation.latitude = geoLocation.latitude
                    newDriverLocation.longitude = geoLocation.longitude
                    val newDistance = location.distanceTo(newDriverLocation) / 1000

                    if (newDistance <= LIMIT_RANGE) {
                    findDriverByKey(driverGeoModel)
                    }


                    }

                    override fun onChildChanged(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                    ) {

                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {

                    }

                    override fun onChildMoved(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                    ) {

                    }

                    override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(
                    requireView(),
                    error!!.message, Snackbar.LENGTH_SHORT
                    )
                    .show()
                    }

                    })**/

                } catch (e: IOException) {
                    Snackbar.make(
                        requireView(),
                        "Permission Location Required", Snackbar.LENGTH_SHORT
                    )
                        .show()

                }

            }
    }

    @SuppressLint("CheckResult")
    private fun addDriverMarker() {
        if (Common.driversFound.size > 0) {
            io.reactivex.Observable.fromIterable(Common.driversFound.keys)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ key: String? ->
                    findDriverByKey(Common.driversFound[key!!])

                }, { t: Throwable? ->
                    Snackbar.make(requireView(), t!!.message!!, Snackbar.LENGTH_SHORT)
                        .show()
                })
        } else {
            Snackbar.make(requireView(), "Driver not found", Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun findDriverByKey(driverGeoModel: DriverGeoModel?) {
        FirebaseDatabase.getInstance()
            .getReference(Common.DRIVER_INFO_REFERENCE)
            .child(driverGeoModel!!.key!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChildren()) {
                        driverGeoModel.driverInfoModel =
                            (snapshot.getValue(DriverInfoModel::class.java))
                        Common.driversFound[driverGeoModel.key!!]!!.driverInfoModel =
                            (snapshot.getValue(DriverInfoModel::class.java))
                        iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel)
                    } else {
                        iFirebaseFailedListener.onFirebaseFailed("Key driver not found" + driverGeoModel.key)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    iFirebaseFailedListener.onFirebaseFailed(error.message)
                }

            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Dexter.withContext(requireContext())
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        return
                    }
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationButtonClickListener {
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener { e ->
                                Snackbar.make(
                                    requireView(), e.message!!,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                            .addOnSuccessListener { location ->
                                val userLating = LatLng(location.latitude, location.longitude)
                                mMap.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        userLating,
                                        10f
                                    )
                                )

                            }

                        true
                    }
                    val locationButton = (mapFragment.requireView()!!
                        .findViewById<View>("1".toInt())!!.parent!! as View)
                        .findViewById<View>("2".toInt())

                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 250

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Snackbar.make(
                        requireView(), p0!!.permissionName + " needed for run app",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

            })
            .check()

        // Enable Zoom
        mMap.uiSettings.isZoomControlsEnabled = true
        // layout button

        try {
            val success = googleMap!!.setMapStyle(
                MapStyleOptions
                    .loadRawResourceStyle(requireContext(), R.raw.uber_maps_style)
            )
            if (!success) {
                Snackbar.make(
                    requireView(), "Load map style failed",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Snackbar.make(
                requireView(), "" + e.message,
                Snackbar.LENGTH_LONG
            ).show()
        }

    }

    override fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel) {
        // If already have marker with this key, doesn't set it again
        if (!Common.markerList.containsKey(driverGeoModel!!.key))
            Common.markerList.put(
                driverGeoModel.key!!,
                mMap.addMarker(
                    MarkerOptions()
                        .position(
                            LatLng(
                                driverGeoModel.geoLocation!!.latitude,
                                driverGeoModel!!.geoLocation!!.longitude
                            )
                        )
                        .flat(true)
                        .title(
                            Common.buildName(
                                driverGeoModel.driverInfoModel!!.name,
                            )
                        )
                        .snippet(driverGeoModel.driverInfoModel!!.mobile)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                )
            )

        if (!TextUtils.isEmpty(cityName)) {
            val driverLocation = cityName?.let {
                FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                    .child(it)
                    .child(driverGeoModel!!.key!!)
            }
            driverLocation?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (!snapshot.hasChildren()) {

                        if (Common.markerList[driverGeoModel!!.key] != null) {
                            val marker = Common.markerList[driverGeoModel!!.key!!]
                            marker!!.remove() // Remove marker from map
                            Common.markerList.remove(driverGeoModel!!.key!!)
                            Common.driverSubscribe.remove(driverGeoModel.key!!)
                            driverLocation.removeEventListener(this)

                        }
                    } else {
                        if (Common.markerList[driverGeoModel!!.key] != null) {

                            val geoQueryModel: GeoQueryModel? =
                                snapshot.getValue(GeoQueryModel::class.java)
                            val animationModel = AnimationModel(false, geoQueryModel)

                            if (Common.driverSubscribe[driverGeoModel.key] != null) {
                                val marker = Common.markerList[driverGeoModel!!.key]
                                val oldPosition: AnimationModel? =
                                    Common.driverSubscribe[driverGeoModel.key]

                                val from: String = StringBuilder()
                                    .append(oldPosition?.geoQueryModel?.g?.get(0))
                                    .append(",")
                                    .append(oldPosition?.geoQueryModel?.g?.get(0))
                                    .toString()

                                val to: String = StringBuilder()
                                    .append(animationModel?.geoQueryModel?.g?.get(0))
                                    .append(",")
                                    .append(animationModel?.geoQueryModel?.g?.get(0))
                                    .toString()

                                moveMarkerAnimation(
                                    driverGeoModel.key,
                                    animationModel, marker, currentLocation, from, to
                                )
                            } else {
                                //First location init
                                driverGeoModel.key?.let {
                                    Common.driverSubscribe
                                        .put(it, animationModel)
                                }
                            }

                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(requireView(), error.message, Snackbar.LENGTH_SHORT).show()
                }

            })
        }


    }

    private fun moveMarkerAnimation(
        key: String?,
        animationModel: AnimationModel,
        marker: Marker?,
        currentLocation: Location?,
        from: String,
        to: String
    ) {
        if (!animationModel.isRunning!!) {


            //Request API
            iGoogleApi?.getDirections(
                "driving",
                "less_driving",
                from, to,
                getString(R.string.google_maps_key)
            )
                ?.subscribeOn(io.reactivex.schedulers.Schedulers.io())
                ?.observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                ?.subscribe { it ->

                    try {
                        val jsonObj = JSONObject(it)
                        val jsonArray = jsonObj.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            //polylineList = Common.decodePoly(polyline)
                            animationModel.polylineList = Common
                                .decodePoly(polyline)
                        }
                        //Moving
                        /** handler = Handler()
                        index = -1
                        next = 1 **/
                        animationModel.index = -1
                        animationModel.next = 1

                        val runnable = object : Runnable {
                            override fun run() {
                                if (animationModel.polylineList!!.size > 1) {

                                    if (animationModel.index < animationModel.polylineList!!.size - 2) {
                                        animationModel.index++
                                        animationModel.next = animationModel.index + 1
                                        animationModel.start =
                                            animationModel.polylineList!![animationModel.index]
                                        animationModel.end =
                                            animationModel.polylineList!![animationModel.next]
                                    }
                                    val valueAnimator = ValueAnimator.ofInt(0, 1)
                                    valueAnimator.duration = 3000
                                    valueAnimator.interpolator = LinearInterpolator()
                                    valueAnimator.addUpdateListener {
                                        animationModel.v = it.animatedFraction

                                        animationModel.lat =
                                            animationModel.v * animationModel.end?.latitude!! + (1 - animationModel.v) * animationModel.start?.latitude!!
                                        animationModel.lng =
                                            animationModel.v * animationModel.end?.longitude!! + (1 - animationModel.v) * animationModel.start?.longitude!!
                                        val newPos = LatLng(animationModel.lat, animationModel.lng)
                                        marker!!.position = newPos
                                        marker.setAnchor(0.5f, 0.5f)
                                        marker.rotation =
                                            Common.getBearing(animationModel.start!!, newPos)

                                    }
                                    valueAnimator.start()
                                    if (animationModel.index < animationModel.polylineList?.size!! - 2)
                                        animationModel.handler!!.postDelayed(this, 1500)
                                    else if (animationModel.index < animationModel.polylineList!!.size - 1) {
                                        animationModel.isRunning = false
                                        Common.driverSubscribe.put(key!!, animationModel)
                                    }
                                }
                            }

                        }

                        animationModel.handler!!.postDelayed(runnable, 1500)


                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }?.let {
                    compositeDisposable?.add(
                        it
                    )
                }
        }
    }

}