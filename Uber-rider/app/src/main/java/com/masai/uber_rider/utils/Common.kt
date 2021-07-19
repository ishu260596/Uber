package com.masai.uber_rider.utils

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.masai.uber_rider.R
import com.masai.uber_rider.utils.models.AnimationModel
import com.masai.uber_rider.utils.models.DriverGeoModel
import com.masai.uber_rider.utils.models.RiderModel
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet


object Common {
    val PICKUP_LOCATION: String = "PickupLocation"
    val REQUEST_DRIVER_TITLE: String = "RequestDriver"
    val RIDER_KEY: String = "RiderKey"
    val NOTI_BODY = "body"
    val NOTI_TITLE = "title"
    const val RIDER_INFO_REFERENCE: String = "Riders"
    val TOKEN_REFERENCE: String = "Token"

    val markerList: MutableMap<String, Marker> = HashMap<String, Marker>()
    val DRIVER_INFO_REFERENCE: String = "Drivers"
    val driversFound: MutableMap<String, DriverGeoModel> = HashMap<String, DriverGeoModel>()
    val DRIVERS_LOCATION_REFERENCES: String = "DriverLocation" // same as driver app

    var currentRider: RiderModel? = null
    val driverSubscribe: MutableMap<String, AnimationModel> =
        HashMap<String, AnimationModel>()


    fun showNotification(
        context: Context,
        id: Int,
        title: String?,
        body: String?,
        intent: Intent?
    ) {

        var pendingIntent: PendingIntent? = null
//
//            if (intent != null) {
        pendingIntent = PendingIntent.getActivity(
            context,
            id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val NOTIFICATION_CHANNEL_ID = "ishu.masai.school"
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel: NotificationChannel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Uber", NotificationManager.IMPORTANCE_HIGH
                )
            notificationChannel.description = "Hi there"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = Color.YELLOW

            notificationManager.createNotificationChannel(notificationChannel)

        }
        val builder: NotificationCompat.Builder = NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_ID)

        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_baseline_directions_car)
            .setLargeIcon(
                BitmapFactory
                    .decodeResource(
                        context.resources,
                        R.drawable.ic_baseline_directions_car
                    )
            )

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        val notification = builder.build()

        notificationManager.notify(id, notification)

//            }
    }

    fun buildName(name: String?): String {
        return StringBuilder(name!!).toString()
    }

    //DECODE POLY
    fun decodePoly(encoded: String): java.util.ArrayList<LatLng?> {
        val poly = java.util.ArrayList<LatLng?>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }

    //GET BEARING
    fun getBearing(begin: LatLng, end: LatLng): Float {
        //You can copy this function by link at description
        val lat = Math.abs(begin.latitude - end.latitude)
        val lng = Math.abs(begin.longitude - end.longitude)
        if (begin.latitude < end.latitude && begin.longitude < end.longitude) return Math.toDegrees(
            Math.atan(lng / lat)
        )
            .toFloat() else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) return (90 - Math.toDegrees(
            Math.atan(lng / lat)
        ) + 90).toFloat() else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude) return (Math.toDegrees(
            Math.atan(lng / lat)
        ) + 180).toFloat() else if (begin.latitude < end.latitude && begin.longitude >= end.longitude) return (90 - Math.toDegrees(
            Math.atan(lng / lat)
        ) + 270).toFloat()
        return (-1).toFloat()
    }

    fun setWelcomeMessage(tvWelcome: TextView?) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (hour in 1..12) {
            tvWelcome?.text = StringBuffer("Good morning.").toString()
        } else if (hour in 13..17) {
            tvWelcome?.text = StringBuffer("Good afternoon.").toString()
        } else {
            tvWelcome?.text = StringBuffer("Good evening.").toString()
        }
    }

    fun formatDuration(duration: String): CharSequence? {

        if (duration.contains("mins")) {
            return duration.substring(0, duration.length - 1)//Remove letter s
        } else {
            return duration
        }

    }

    fun formatAddress(startAddress: String): CharSequence? {
        val firstIndexComms = startAddress.indexOf(",")
        return startAddress.substring(0, firstIndexComms)
    }

    fun valueAnimator(
        duration: Int,
        listner: ValueAnimator.AnimatorUpdateListener?
    ): ValueAnimator {
        val va = ValueAnimator.ofFloat(0f, 100f)
        va.duration = duration.toLong()
        va.addUpdateListener(listner)
        va.repeatCount = ValueAnimator.INFINITE
        va.repeatMode = ValueAnimator.RESTART
        va.start()
        return va
    }
}