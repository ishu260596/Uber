package com.masai.uber_rider.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.masai.uber_rider.R
import com.masai.uber_rider.databinding.FragmentNameBinding
import com.masai.uber_rider.utils.KEY_PASSENGER_DISPLAY_NAME
import com.masai.uber_rider.utils.PreferenceHelper
import com.masai.uber_rider.utils.UserUtils
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType

class NameFragment : Fragment() {
    private var binding: FragmentNameBinding? = null

    private lateinit var mAuth: FirebaseAuth
    private lateinit var userDatabaseRef: DatabaseReference

    private lateinit var userId: String
    private lateinit var fName: String
    private lateinit var lName: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNameBinding.inflate(layoutInflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PreferenceHelper.getSharedPreferences(requireContext())
        mAuth = FirebaseAuth.getInstance()

        userId = mAuth.currentUser?.uid.toString()
        val user = mAuth.currentUser
        if (user != null) {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        AestheticDialog.Builder(
                            requireActivity(),
                            DialogStyle.TOASTER,
                            DialogType.ERROR
                        )
                            .setTitle("Failed")
                            .show()
                        return@OnCompleteListener
                    }
                    // Get new FCM registration token
                    val token = task.result
                    Log.d("Token", token.toString())
                    UserUtils.updateToken(requireContext(), token)
                })
        }
        userDatabaseRef = FirebaseDatabase.getInstance().reference

        binding!!.btnNext.setOnClickListener {
            validateCredentials()
        }
    }

    private fun validateCredentials() {
        fName = binding!!.etFirstName.text.toString()
        lName = binding!!.etLastName.text.toString()
        if (fName.isEmpty()) {
            binding!!.etFirstName.error = "Please provide first name"
            return
        }
        if (lName.isEmpty()) {
            binding!!.etLastName.error = "Please provide last name"
            return
        }

        if (fName.isNotBlank() && lName.isNotBlank()) {
            val name = "$fName $lName"
            PreferenceHelper.writeStringToPreference(KEY_PASSENGER_DISPLAY_NAME, name)
            Log.d("tag", name)
            saveIntoFirebase()
        }
    }

    private fun saveIntoFirebase() {

        userDatabaseRef.child("Riders").child(userId)
            .child("fname")
            .setValue(fName)
        userDatabaseRef.child("Riders").child(userId)
            .child("lname")
            .setValue(lName)
            .addOnSuccessListener {
                AestheticDialog.Builder(requireActivity(), DialogStyle.TOASTER, DialogType.SUCCESS)
                    .setTitle("Success")
                    .show()
                redirect()
            }
            .addOnFailureListener {
                AestheticDialog.Builder(requireActivity(), DialogStyle.TOASTER, DialogType.ERROR)
                    .setTitle("Failed")
                    .show()
            }
    }

    private fun redirect() {
        val promoCodeFragment = PromoCodeFragment()
        val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
        transaction.replace(R.id.container3, promoCodeFragment)
        transaction.addToBackStack("FragmentPromo")
        transaction.commit()
        return
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}