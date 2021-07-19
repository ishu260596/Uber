package com.masai.uber_rider.utils

import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.masai.uber_rider.remote.ICFMService
import com.masai.uber_rider.remote.RetrofitFCMClient
import com.masai.uber_rider.utils.models.DriverGeoModel
import com.masai.uber_rider.utils.models.FCMSendData
import com.masai.uber_rider.utils.models.TokenModel
import com.masai.uber_rider.utils.models.TokenModel2
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.StringBuilder

class UserUtils {
    companion object {
        fun updateToken(context: Context, token: String) {
            val tokenModel = TokenModel(token)


            FirebaseDatabase.getInstance().getReference(KEY_TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().currentUser?.uid.toString())
                .setValue(tokenModel)
                .addOnFailureListener {

                    AestheticDialog.Builder(
                        context as Activity,
                        DialogStyle.TOASTER,
                        DialogType.ERROR
                    )
                        .setTitle(it.message.toString())
                        .show()
                }
                .addOnSuccessListener {

                }
        }

        fun sendRequestToDriver(
            context: Context,
            mainLayout: RelativeLayout?,
            foundDriver: DriverGeoModel?,
            target: LatLng,
            startAddress: String,
            endAddress: String
        ) {
            val compositeDisposable = CompositeDisposable()
            val ifcmService = RetrofitFCMClient.getInstance()!!.create(ICFMService::class.java)
            //Get token
            FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(foundDriver!!.key!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val tokenModel = snapshot.getValue(TokenModel2::class.java)
                            val notificationData: MutableMap<String, String> = HashMap()
                            notificationData.put(Common.NOTI_TITLE, Common.REQUEST_DRIVER_TITLE)
                            notificationData.put(
                                Common.NOTI_BODY,
                                "Hi There MR. This message represent for Request Driver action"
                            )
                            FirebaseAuth.getInstance().currentUser?.uid?.let {
                                notificationData.put(
                                    Common.RIDER_KEY,
                                    it
                                )
                            }
                            notificationData[Common.PICKUP_LOCATION] = StringBuilder()
                                .append(target.latitude)
                                .append(",")
                                .append(target.longitude)
                                .toString()
                            notificationData[START_ADDRESS] = startAddress
                            notificationData[END_ADDRESS] = endAddress

                            val fcmSendData =
                                tokenModel!!.token?.let { FCMSendData(it, notificationData) }
                            compositeDisposable.add(
                                ifcmService.sendNotification(fcmSendData)!!
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ fcmResponse ->
                                        if (fcmResponse!!.success == 0) {
                                            compositeDisposable.clear()
                                            Snackbar.make(
                                                mainLayout!!,
                                                "Request Driver failed",
                                                Snackbar.LENGTH_LONG
                                            )
                                                .show()
                                        }
                                    }, { t: Throwable? ->
                                        compositeDisposable.clear()
                                        Snackbar.make(
                                            mainLayout!!,
                                            t!!.message!!,
                                            Snackbar.LENGTH_LONG
                                        )
                                            .show()
                                    })
                            )

                        } else {
                            Snackbar.make(mainLayout!!, "Token Not found", Snackbar.LENGTH_LONG)
                                .show()

                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Snackbar.make(mainLayout!!, error.message, Snackbar.LENGTH_LONG).show()
                    }

                })

        }
    }

}