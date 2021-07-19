package com.masai.uber_rider.utils.models

import android.os.Handler
import com.google.android.gms.maps.model.LatLng

class AnimationModel(
    var isRunning: Boolean? = null,
    val geoQueryModel: GeoQueryModel? = null

) {
    //moving marker
     var polylineList: java.util.ArrayList<LatLng?>? = null
     var handler: Handler? = null
     var index: Int = 0
     var next: Int = 0
     var start: LatLng? = null
     var end: LatLng? = null
     var v: Float = 0.0f
     var lat: Double = 0.0
     var lng: Double = 0.0

    init {
        handler = Handler()
    }
}


