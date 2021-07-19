package com.masai.uber_rider.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.masai.uber_rider.R
import kotlinx.android.synthetic.main.activity_receipt.*

class ReceiptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)
        btnReceipt.setOnClickListener {
            startActivity(Intent(this, PaymentGateWayActivity::class.java))
        }
    }
}