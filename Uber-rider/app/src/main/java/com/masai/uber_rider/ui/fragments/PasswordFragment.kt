package com.masai.uber_rider.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentTransaction
import com.cazaea.sweetalert.SweetAlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.masai.uber_rider.R
import com.masai.uber_rider.databinding.FragmentPasswordBinding
import com.masai.uber_rider.ui.activities.RiderHomeActivity
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType


class PasswordFragment : Fragment() {
    private var binding: FragmentPasswordBinding? = null

    private lateinit var mAuth: FirebaseAuth
    private lateinit var userDatabaseRef: DatabaseReference

    private lateinit var userId: String
    private lateinit var password: String
    private lateinit var mobile: String

    private lateinit var pDialog: SweetAlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPasswordBinding.inflate(layoutInflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()

        userId = mAuth.currentUser?.uid.toString()
        userDatabaseRef = FirebaseDatabase.getInstance().reference

        pDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        pDialog.progressHelper.barColor = Color.parseColor("#CF7351")
        pDialog.titleText = "Please wait !"
        pDialog.setCancelable(false)


        val bundle = arguments
        if (bundle != null) {
            mobile = bundle.getString("mobile").toString()
        }

        binding!!.btnNextPassword.setOnClickListener {
            password = binding!!.etPassword.text.toString()
            if (password.length > 6) {
                saveIntoFirebase()
            } else {
                binding!!.etPassword.error = "password length must be greater than 6"
            }
        }
    }

    private fun saveIntoFirebase() {
        pDialog.show()
        val hashMap: HashMap<String, String> = HashMap<String, String>()
        hashMap["userId"] = userId
        hashMap["mobile"] = mobile
        hashMap["password"] = password
        userDatabaseRef.child("Riders").child(userId).setValue(hashMap)
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
        pDialog.cancel()
        val fragmentName = NameFragment()
        val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
        transaction.replace(R.id.container3, fragmentName)
        transaction.addToBackStack("FragmentName")
        transaction.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}