package com.masai.uber_rider.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentTransaction
import com.masai.uber_rider.R
import com.masai.uber_rider.databinding.FragmentPromoCodeBinding

class PromoCodeFragment : Fragment() {
    private var binding: FragmentPromoCodeBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPromoCodeBinding.inflate(layoutInflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.btnForward.setOnClickListener {
            if (binding!!.etPromoCode.text.toString().isEmpty()) {
                binding!!.etPromoCode.error = "Provide a promo code if you have any!"
            } else {
                redirect()
            }
        }

        binding!!.tvSkip.setOnClickListener {
            redirect()
        }
    }

    private fun redirect() {
        val fragmentAge = AgeCheckFragment()
        val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
        transaction.replace(R.id.container3, fragmentAge)
        transaction.addToBackStack("FragmentAge")
        transaction.commit()
        return
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}