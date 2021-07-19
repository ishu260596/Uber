package com.masai.uber_rider.remote

import com.masai.uber_rider.utils.models.FCMResponse
import com.masai.uber_rider.utils.models.FCMSendData
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ICFMService {

    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAADAJ0F9I:APA91bF0p93-c8Rqe-dCjmdtAD-dZ-CU0ZeQVJNXJK3IoZQosdoJw6Bfw8h268pR3vgmtKW3N5KUzfH8BMj-jlbJQ-5cytRhcSOHh9Amm-E1FsfNJbP5gMkQakbjbbZErM06kBSZSBDm"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData?): Observable<FCMResponse?>?
}