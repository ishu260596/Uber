package com.masai.uber_rider.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.masai.uber_rider.R
import com.masai.uber_rider.databinding.FragmentAgeCheckBinding
import com.masai.uber_rider.ui.activities.RiderHomeActivity

class AgeCheckFragment : Fragment() {
    private var binding: FragmentAgeCheckBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAgeCheckBinding.inflate(layoutInflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /** To Change the color of some substrings **/
        val text = getString(R.string.checkpara)
        val spannable = SpannableString(text)

        /** To make clickable substrings **/
        var clickableSpan1: ClickableSpan? = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(context, "One", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.BLUE
                ds.isUnderlineText = false
            }
        }

        var clickableSpan2: ClickableSpan? = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(context, "One", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.BLUE
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(clickableSpan1, 73, 92, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(clickableSpan2, 112, 127, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding!!.TextView.text = spannable
        binding!!.TextView.movementMethod = LinkMovementMethod.getInstance()
        binding!!.btnNext.setOnClickListener {
            if (binding!!.checkBox.isChecked) {
                startActivity(Intent(context, RiderHomeActivity::class.java))
                activity?.finish()
            } else {
                Toast.makeText(
                    context, "You are not adult yet !",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}