package com.masai.uber_rider.ui.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.masai.uber_rider.R
import com.masai.uber_rider.ui.fragments.FragmentEnterMobileNo

class MainActivity : AppCompatActivity() {
    private lateinit var fragmentManager: FragmentManager

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        launchFragment()
    }

    private fun launchFragment() {
        val fragmentEnterMobileNo = FragmentEnterMobileNo()
        fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragmentEnterMobileNo)
        transaction.addToBackStack("FragmentEnterMobileNo")
        transaction.commit()
    }
}