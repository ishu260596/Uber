package com.masai.uber_rider.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.masai.uber_rider.R
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType
import kotlinx.android.synthetic.main.activity_payment_gate_way.*

class PaymentGateWayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_gate_way)
        btnGooglePay.setOnClickListener {
            AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.SUCCESS)
                .setTitle("Paid")
                .show()
            startActivity(Intent(this, RiderHomeActivity::class.java))
        }
    }
}