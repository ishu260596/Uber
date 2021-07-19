package com.masai.uber_rider.callback

import com.masai.uber_rider.utils.models.DriverGeoModel

interface FirebaseDriverInfoListener {
    fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel)
}