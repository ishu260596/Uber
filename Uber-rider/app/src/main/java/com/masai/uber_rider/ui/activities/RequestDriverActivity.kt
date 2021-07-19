package com.masai.uber_rider.ui.activities

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.directions.route.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ui.IconGenerator
import com.masai.uber_rider.R
import com.masai.uber_rider.databinding.ActivityRequestDriverBinding
import com.masai.uber_rider.remote.ApiClient
import com.masai.uber_rider.remote.IGoogleApi
import com.masai.uber_rider.remote.NetworkRetrofit
import com.masai.uber_rider.remote.RetrofitClient
import com.masai.uber_rider.remote.models.LegsItem
import com.masai.uber_rider.remote.models.Result
import com.masai.uber_rider.remote.models.RoutesItem
import com.masai.uber_rider.utils.Common
import com.masai.uber_rider.utils.EventBus.SelectedPlaceEvent
import com.masai.uber_rider.utils.LatLngConvertor
import com.masai.uber_rider.utils.UserUtils
import com.masai.uber_rider.utils.models.DriverGeoModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_request_driver.*
import kotlinx.android.synthetic.main.layout_find_yourdriver.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.lang.StringBuilder
import retrofit2.Call
import retrofit2.Response

class RequestDriverActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleApiClient.OnConnectionFailedListener,
    RoutingListener {

    private lateinit var foundDriver: DriverGeoModel

    //effect
    var lastUserCircle: Circle? = null
    val duration = 1000
    var lastPlusAnimator: ValueAnimator? = null
    var animator: ValueAnimator? = null
    private val DESIRED_NUM_SPIN = 5
    private val PER_SPINTIME = 40

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityRequestDriverBinding

    //polyline object
    private var polylines: ArrayList<Polyline>? = null

    private var selectPlaceEvent: SelectedPlaceEvent? = null

    //Routes
    private var compositeDisposable: CompositeDisposable? = null
    private var iGoogleApi: IGoogleApi? = null
    private var apiClient: ApiClient? = null
    private var blackPolyline: Polyline? = null
    private var greyPolyline: Polyline? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var polylineList: ArrayList<LatLng?>? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

    //views
    private var btnConfirmUber: Button? = null
    private var btnConfirmPickup: Button? = null
    private var confirm_uber_pickup: CardView? = null
    private var confirm_uber_layout: CardView? = null
    private var confirm_driver_layout: CardView? = null
    private var tvPickUpAddress: TextView? = null
    private var tvOrigin: TextView? = null

    private lateinit var eAddress: String
    private var sAddress = "Hanuman Mandir Mattan Sidh 177001"
    private lateinit var eeAddress: String
    private lateinit var ssAddress: String
    private lateinit var dDistance: String
    private lateinit var dDuration: String



    override fun onStart() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        super.onStart()
    }

    override fun onStop() {
        compositeDisposable?.clear()
        if (!EventBus.getDefault().hasSubscriberForEvent(SelectedPlaceEvent::class.java)) {
            EventBus.getDefault().removeStickyEvent(SelectedPlaceEvent::class.java)
            EventBus.getDefault().unregister(this)
        }
        super.onStop()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectPlaceEvent(event: SelectedPlaceEvent) {
        selectPlaceEvent = event
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRequestDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intViews()

        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun intViews() {
        iGoogleApi = RetrofitClient.getInstanceK()!!.create(IGoogleApi::class.java)
        apiClient = NetworkRetrofit.getInstanceRe()!!.create(ApiClient::class.java)

        btnConfirmUber = findViewById(R.id.btnConfirmUber)
        btnConfirmPickup = findViewById(R.id.btnConfirmPickUp)

        confirm_uber_layout = findViewById<View>(R.id.confirm_uber_layout) as CardView
        confirm_uber_pickup = findViewById<View>(R.id.confirm_uber_pickup) as CardView
        confirm_driver_layout = findViewById<View>(R.id.confirm_uber_layout_driver) as CardView
//        filMaps = findViewById<View>(R.id.filMaps) as View

        btnConfirmUber!!.setOnClickListener {
            confirm_uber_pickup!!.visibility = View.VISIBLE
            confirm_uber_layout!!.visibility = View.GONE

            setDataPicker()
        }

        btnConfirmPickup!!.setOnClickListener {
            if (mMap == null) return@setOnClickListener
            if (selectPlaceEvent == null) return@setOnClickListener

//            mMap.clear()

            //tilt
            val cameraPosition = CameraPosition.Builder().target(selectPlaceEvent!!.origin)
                .tilt(45f)
                .zoom(16f)
                .build()
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

            //start animation
            addMarkerWithPlusAnimation()

        }

    }

    private fun addMarkerWithPlusAnimation() {
        confirm_uber_pickup!!.visibility = View.GONE
//        filMaps?.visibility = View.VISIBLE
        confirm_driver_layout?.visibility = View.VISIBLE

        originMarker = mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker())
                .position(selectPlaceEvent!!.origin)
        )

        addPlusatingEffect(selectPlaceEvent!!.origin)
    }

    private fun addPlusatingEffect(origin: LatLng) {
        if (lastPlusAnimator != null) lastPlusAnimator!!.cancel()

        if (lastUserCircle != null) lastUserCircle!!.center = origin
        lastPlusAnimator = Common.valueAnimator(duration,
            object : ValueAnimator.AnimatorUpdateListener {
                override fun onAnimationUpdate(animation: ValueAnimator?) {
                    if (lastUserCircle != null) {
                        lastUserCircle!!.radius = animation?.animatedValue.toString().toDouble()

                    } else {
                        lastUserCircle = mMap.addCircle(
                            CircleOptions()
                                .center(origin)
                                .radius(animation?.animatedValue.toString().toDouble())
                                .strokeColor(Color.WHITE)
                                .fillColor(
                                    ContextCompat.getColor(
                                        this@RequestDriverActivity,
                                        R.color.grey
                                    )
                                )
                        )

                    }
                }

//                if (lastUserCircle != null) lastUserCircle.radius = animation
            })

        //startRotation
        startMapCameraSpiningAnimation(mMap.cameraPosition.target)

    }

    @SuppressLint("Recycle")
    private fun startMapCameraSpiningAnimation(target: LatLng) {
        if (animator != null) {
            animator!!.cancel()
        }
        animator = ValueAnimator.ofFloat(0f, (DESIRED_NUM_SPIN * 360).toFloat())
        animator!!.duration = (DESIRED_NUM_SPIN * PER_SPINTIME * 1000).toLong()
        animator!!.interpolator = LinearInterpolator()
        animator!!.startDelay = (100)
        animator!!.addUpdateListener {
            val newBearingValue = it.animatedValue as Float
            mMap.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(target)
                        .zoom(16f)
                        .tilt(45f)
                        .bearing(newBearingValue)
                        .build()
                )
            )
        }
        animator!!.start()
        findNearbyDriver(target, ssAddress, eeAddress)

    }

    private fun findNearbyDriver(target: LatLng, startAddress: String, endAddress: String) {
        if (Common.driversFound.size > 0) {
            var min = 0f
            foundDriver = Common   //Default found driver is first driver
                .driversFound[Common.driversFound.keys.iterator().next()]!!
            val currentRiderLocation = Location("")
            currentRiderLocation.latitude = target!!.latitude
            currentRiderLocation.longitude = target!!.longitude
            for (key in Common.driversFound.keys) {
                val driverLocation = Location("")
                driverLocation.latitude = Common.driversFound[key]!!.geoLocation!!.latitude
                driverLocation.longitude = Common.driversFound[key]!!.geoLocation!!.longitude

                //first init min value and found driver if first driver in list
                if (min == 0f) {
                    min = driverLocation.distanceTo(currentRiderLocation)
                    foundDriver = Common.driversFound[key]!!
                } else if (driverLocation.distanceTo(currentRiderLocation) < min) {
                    min = driverLocation.distanceTo(currentRiderLocation)
                    foundDriver = Common.driversFound[key]!!
                }
//                Snackbar.make(
//                    main_layout, StringBuilder("Found driver: ")
//                        .append(foundDriver!!.driverInfoModel!!.mobile),
//                    Snackbar.LENGTH_LONG
//                ).show()
//                UserUtils.sendRequestToDriver(
//                    this@RequestDriverActivity,
//                    main_layout,
//                    foundDriver,
//                    target,
//                    ssAddress, endAddress
//                )

                UserUtils.sendRequestToDriver(
                    this@RequestDriverActivity,
                    main_layout,
                    foundDriver,
                    target,
                    selectPlaceEvent?.originString!!,
                    selectPlaceEvent?.destinationString!!
                )
            }
        } else {
            Snackbar.make(
                main_layout, "Drivers Not Found",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun setDataPicker() {
//        tvPickUpAddress = findViewById(R.id.tvPickUpAddress)
//        tvPickUpAddress!!.text = ssAddress
        mMap.clear()
        addPickUpMarker()
    }

    private fun addPickUpMarker() {
        val view = layoutInflater.inflate(R.layout.pickup_infor_window, null)
        val generator = IconGenerator(this)
        generator.setContentView(view)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        val icon = generator.makeIcon()

        originMarker = mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectPlaceEvent!!.origin)
        )

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMinZoomPreference(6.0f)
        mMap.setMaxZoomPreference(14.0f)

        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                selectPlaceEvent!!.origin, 18f
            )
        )
        mMap.animateCamera(CameraUpdateFactory.zoomIn())
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10f), 2000, null)

        val cameraPosition = CameraPosition.Builder()
            .target(selectPlaceEvent?.origin) // Sets the center of the map to Mountain View
            .zoom(17f)            // Sets the zoom
            .bearing(90f)         // Sets the orientation of the camera to east
            .tilt(30f)            // Sets the tilt of the camera to 30 degrees
            .build()              // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        drawPath2(selectPlaceEvent!!)
//
//        findRoutes(selectPlaceEvent!!)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.setOnMyLocationClickListener {
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    selectPlaceEvent?.origin, 18f
                )
            )
            true
        }

        //layout button
        val locationBtn = (findViewById<View>("1".toInt())!!.parent!! as View)
            .findViewById<View>("2".toInt())

        val params = locationBtn.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        params.bottomMargin = 250

        mMap.uiSettings.isZoomControlsEnabled = true

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_maps_style)
            )
            if (!success) {
                Snackbar.make(
                    mapFragment.requireView(),
                    "Load Map style failed",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Snackbar.make(
                mapFragment.requireView(),
                e.message.toString(),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun findRoutes(
        selectPlaceEvent: SelectedPlaceEvent,
    ) {

        val routing = Routing.Builder()
            .travelMode(AbstractRouting.TravelMode.DRIVING)
            .withListener(this)
            .alternativeRoutes(true)
            .waypoints(selectPlaceEvent.origin, selectPlaceEvent.destination)
            .key("AIzaSyBrgPQhpR6cePo-zHYSxNfEwQY6MqNI74w") //also define your api key here.
            .build()
        routing.execute()
    }

    @SuppressLint("CheckResult")
    private fun drawPath(selectPlaceEvent: SelectedPlaceEvent) {

        //Request API
        iGoogleApi?.getDirections(
            "driving",
            "less_driving",
            selectPlaceEvent.originString,
            selectPlaceEvent.destinationString,
            getString(R.string.google_maps_key)
        )
            ?.subscribeOn(io.reactivex.schedulers.Schedulers.io())
            ?.observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
            ?.subscribe { it ->

                try {
                    val jsonObj = JSONObject(it!!)
                    val jsonArray = jsonObj.getJSONArray("routes")

                    for (i in 0 until jsonArray.length()) {
                        val route = jsonArray.getJSONObject(i)
                        val poly = route.getJSONObject("overview_polyline")
                        val polyline = poly.getString("points")
                        polylineList = Common.decodePoly(polyline)
                    }
                    polylineOptions = PolylineOptions()
                    polylineOptions!!.color(Color.GRAY)
                    polylineOptions!!.width(12f)
                    polylineOptions!!.jointType(JointType.ROUND)
                    polylineOptions!!.startCap(SquareCap())
                    polylineOptions!!.addAll(polylineList!!)
                    greyPolyline = mMap.addPolyline(polylineOptions!!)

                    blackPolylineOptions = PolylineOptions()
                    blackPolylineOptions!!.color(Color.BLACK)
                    blackPolylineOptions!!.width(12f)
                    blackPolylineOptions!!.jointType(JointType.ROUND)
                    blackPolylineOptions!!.startCap(SquareCap())
                    blackPolylineOptions!!.addAll(polylineList!!)
                    blackPolyline = mMap.addPolyline(blackPolylineOptions!!)

                    //Animator
                    val valueAnimator = ValueAnimator.ofInt(0, 100)
                    valueAnimator.duration = 1100
                    valueAnimator.repeatCount = ValueAnimator.INFINITE
                    valueAnimator.interpolator = LinearInterpolator()
                    valueAnimator.addUpdateListener { value ->
                        val points = greyPolyline!!.points
                        val percentageValue = value.animatedValue.toString().toInt()
                        val size = points.size
                        val newPoints = (size * (percentageValue / 100.0f)).toInt()
                        val p = points.subList(0, newPoints)
                        blackPolyline!!.points = p
                    }
                    valueAnimator.start()

                    val latLngBound = LatLngBounds.Builder().include(selectPlaceEvent.origin)
                        .include(selectPlaceEvent.destination).build()

                    //add car icon for origin

                    val objects = jsonArray.getJSONObject(0)
                    val legs = objects.getJSONArray("legs")
                    val legsObject = legs.getJSONObject(0)

                    val time = legsObject.getJSONObject("duration")
                    val duration = time.getString("text")

                    val startAddress = legsObject.getString("start_address")
                    sAddress = startAddress.toString()
                    val endAddress = legsObject.getString("end_address")
                    eAddress = endAddress.toString()

                    addOriginMarker(duration, startAddress)
                    addDestinationMarker(duration, endAddress)

                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom - 1))

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }?.let {
                compositeDisposable?.add(
                    it
                )
            }
    }

    private fun addDestinationMarker(duration: String, endAddress: String) {
        val view = layoutInflater.inflate(R.layout.destination_infor_window, null)
        val tvDestination = view.findViewById<View>(R.id.tvDestinationMap) as TextView
        tvDestination.text = Common.formatAddress(endAddress).toString()

        val generator = IconGenerator(this)
        generator.setContentView(view)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        val icon = generator.makeIcon()

        destinationMarker = mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectPlaceEvent!!.destination)
        )

    }

    private fun addOriginMarker(duration: String, startAddress: String) {
        val view = layoutInflater.inflate(R.layout.origin_infor_window, null)
        val tvTime = view.findViewById<View>(R.id.tvTime) as TextView
        tvOrigin = view.findViewById<View>(R.id.tvOrigin) as TextView

        tvTime.text = Common.formatDuration(dDuration).toString()
        tvOrigin!!.text = Common.formatAddress(startAddress).toString()

        val generator = IconGenerator(this)
        generator.setContentView(view)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        val icon = generator.makeIcon()

        originMarker = mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectPlaceEvent!!.origin)
        )

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        findRoutes(selectPlaceEvent!!)
    }

    override fun onRoutingFailure(p0: RouteException?) {
        val parentLayout = findViewById<View>(android.R.id.content)
        val snackbar: Snackbar = Snackbar.make(parentLayout, p0.toString(), Snackbar.LENGTH_LONG)
        snackbar.show()
        findRoutes(selectPlaceEvent!!)
    }

    override fun onRoutingStart() {
    }

    override fun onRoutingSuccess(p0: java.util.ArrayList<Route>?, p1: Int) {
        val center = CameraUpdateFactory.newLatLng(selectPlaceEvent!!.origin)
        val zoom = CameraUpdateFactory.zoomTo(16f)
        polylines?.clear()
        val polyOptions = PolylineOptions()
        var polylineStartLatLng: LatLng? = null
        var polylineEndLatLng: LatLng? = null
        polylines = ArrayList()
        //add route(s) to the map using polyline
        for (i in 0 until p0!!.size) {
            if (i == p1) {
                polyOptions.color(Color.GRAY)
                polyOptions.width(12f)
                polyOptions.addAll(p0.get(p1).getPoints())
                val polyline = mMap.addPolyline(polyOptions)
                polylineStartLatLng = polyline.points[0]
                val k = polyline.points.size
                polylineEndLatLng = polyline.points[k - 1]
                (polylines as ArrayList<Polyline>).add(polyline)
            } else {
            }
        }
        //Add Marker on route starting position
        //Add Marker on route starting position
        val view = layoutInflater.inflate(R.layout.origin_infor_window, null)
        val tvTime = view.findViewById<View>(R.id.tvTime) as TextView
        tvOrigin = view.findViewById<View>(R.id.tvOrigin) as TextView
        tvTime.text = Common.formatDuration(dDuration.toString()).toString()
        tvOrigin!!.text = ssAddress

        val generator = IconGenerator(this)
        generator.setContentView(view)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        val icon = generator.makeIcon()

        originMarker = mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectPlaceEvent!!.origin)
        )

        /** val startMarker = MarkerOptions()
        startMarker.position(polylineStartLatLng!!)
        startMarker.title("My Location")
        mMap.addMarker(startMarker) **/

        //Add Marker on route ending position
        //Add Marker on route ending position
        /**val view1 = layoutInflater.inflate(R.layout.destination_infor_window, null)
        val tvDestinaftion = view1.findViewById<View>(R.id.tvDestinationMap) as TextView
        tvDestinaftion.text=eeAddress

        val generator1 = IconGenerator(this)
        generator.setContentView(view1)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        val icon1 = generator1.makeIcon()

        destinationMarker = mMap.addMarker(
        MarkerOptions()
        .icon(BitmapDescriptorFactory.fromBitmap(icon1))
        .position(selectPlaceEvent!!.destination)
        ) **/

        val endMarker = MarkerOptions()
        endMarker.position(polylineEndLatLng!!)
        endMarker.title("Destination")
        endMarker.icon(
            getBitmapDescriptorFromVector(
                this,
                R.drawable.ic__375372_logo_uber_icon__1_
            )
        )
        mMap.addMarker(endMarker)
    }

    fun getBitmapDescriptorFromVector(
        context: Context,
        vectorDrawableResourceId: Int
    ): BitmapDescriptor? {

        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onRoutingCancelled() {
        findRoutes(selectPlaceEvent!!)
    }

    private fun drawPath2(selectPlaceEvent: SelectedPlaceEvent) {

        val apiClient = NetworkRetrofit.getInstanceRe()?.create(ApiClient::class.java)

        apiClient?.getDirections(
            selectPlaceEvent.originString,
            selectPlaceEvent.destinationString,
            getString(R.string.google_maps_key)
        )?.enqueue(object : retrofit2.Callback<Result> {

            override fun onResponse(call: Call<Result?>, response: Response<Result?>) {
                if (response.isSuccessful) {
                    val result: Result? = response.body()
                    val routes: List<RoutesItem?>? = result?.routes
                    val legs: List<LegsItem?>? = routes?.get(0)?.legs
                    ssAddress = legs?.get(0)?.startAddress.toString()
                    eeAddress = legs?.get(0)?.endAddress.toString()
                    dDistance = legs?.get(0)?.distance?.text.toString()
                    dDuration = legs?.get(0)?.duration?.text.toString()
                    val polyline = legs?.get(0)?.steps?.get(0)?.polyline.toString()
                    polylineList = Common.decodePoly(polyline)
                    Log.d("tag", ssAddress)
                    Log.d("tag", eeAddress)
                    Log.d("tag", dDistance)
                    Log.d("tag", dDuration)

//                    UserUtils.sendRequestToDriver(
//                        this@RequestDriverActivity,
//                        main_layout,
//                        foundDriver,
//                        LatLngConvertor.getDestination(eeAddress),
//                        ssAddress, eeAddress
//                    )
//                    findRoutes(selectPlaceEvent!!, ssAddress, eeAddress, dDistance, dDuration)

                    findRoutes(selectPlaceEvent!!)

                    /** polylineOptions = PolylineOptions()
                    polylineOptions!!.color(Color.GRAY)
                    polylineOptions!!.width(12f)
                    polylineOptions!!.jointType(JointType.ROUND)
                    polylineOptions!!.addAll(polylineList!!)
                    greyPolyline = mMap.addPolyline(polylineOptions!!)

                    blackPolylineOptions = PolylineOptions()
                    blackPolylineOptions!!.color(Color.BLACK)
                    blackPolylineOptions!!.width(12f)
                    blackPolylineOptions!!.jointType(JointType.ROUND)
                    blackPolylineOptions!!.addAll(polylineList!!)
                    blackPolyline = mMap.addPolyline(blackPolylineOptions!!)

                    //Animator
                    val valueAnimator = ValueAnimator.ofInt(0, 100)
                    valueAnimator.duration = 1100
                    valueAnimator.repeatCount = ValueAnimator.INFINITE
                    valueAnimator.interpolator = LinearInterpolator()
                    valueAnimator.addUpdateListener { value ->
                    val points = greyPolyline!!.points
                    val percentageValue = value.animatedValue.toString().toInt()
                    val size = points.size
                    val newPoints = (size * (percentageValue / 100.0f)).toInt()
                    val p = points.subList(0, newPoints)
                    blackPolyline!!.points = p
                    }
                    valueAnimator.start()

                    val latLngBound = LatLngBounds.Builder().include(selectPlaceEvent.origin)
                    .include(selectPlaceEvent.destination).build()

                    addOriginMarker(dDuration, ssAddress)
                    addDestinationMarker(dDuration, eeAddress)

                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom - 1)) **/
                }

            }

            override fun onFailure(call: Call<Result?>, t: Throwable) {
                Log.d("tag", "Exception")
            }

        })
    }

    override fun onDestroy() {
        if (animator != null) animator!!.end()
        super.onDestroy()
    }
}