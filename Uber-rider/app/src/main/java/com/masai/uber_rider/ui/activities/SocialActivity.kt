package com.masai.uber_rider.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cazaea.sweetalert.SweetAlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import com.masai.uber_rider.databinding.ActivitySocialBinding
import com.masai.uber_rider.utils.*
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType
import java.util.*


class SocialActivity : AppCompatActivity() {
    private val SIGN_IN_CODE = 10
    private var binding: ActivitySocialBinding? = null

    //googleAuth
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userDatabaseRef: DatabaseReference
    private lateinit var googleSignInOptions: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var userId: String
    private lateinit var name: String
    private lateinit var profileUrl: String
    private lateinit var email: String
    private lateinit var uId: String

    private lateinit var pDialog: SweetAlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        PreferenceHelper.getSharedPreferences(this)
        initializeSignin()
    }

    private fun initializeSignin() {
        googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken("51580770258-ik294mt3tio5euva4h3pshmcecfbfa9v.apps.googleusercontent.com")
            .requestId()
            .requestProfile()
            .build()

        mAuth = FirebaseAuth.getInstance()
        userDatabaseRef = FirebaseDatabase.getInstance().reference
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        pDialog.progressHelper.barColor = Color.parseColor("#CF7351")
        pDialog.titleText = "Please wait !"
        pDialog.setCancelable(false)

        binding!!.btnGoogle.setOnClickListener {
            val intent: Intent = googleSignInClient.signInIntent
            startActivityForResult(intent, SIGN_IN_CODE)
            pDialog.show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_CODE) {
            val task: Task<GoogleSignInAccount>? = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                val account: GoogleSignInAccount? = task?.getResult(ApiException::class.java)
                processFirebaseLoginStep(account?.idToken, task);
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

        }
    }

    private fun processFirebaseLoginStep(idToken: String?, task1: Task<GoogleSignInAccount>?) {
        val authCredential = GoogleAuthProvider.getCredential(idToken, null)

        mAuth.signInWithCredential(authCredential)
            .addOnCompleteListener(this,
                OnCompleteListener<AuthResult?> { task ->
                    if (task.isSuccessful) {
                        val user: FirebaseUser? = mAuth.currentUser
                        userId = user?.uid.toString()
                        Toast.makeText(this, userId, Toast.LENGTH_SHORT).show()
                        handleSignInResult(task1)
                    }
                })
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>?) {
        try {
            if (task!!.isSuccessful) {
                val account = task.getResult(ApiException::class.java)
                name = account.displayName.toString()
                email = account.email.toString()
                uId = account.id.toString()
                profileUrl = account.photoUrl.toString()
                PreferenceHelper.writeBooleanToPreference(KEY_LOGIN_WITH_OAUTH, true)
                Toast.makeText(this, "Welcome ${account.displayName}", Toast.LENGTH_SHORT)
                    .show()
                updatePreference()
                saveUser()
            } else {
                AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.ERROR)
                    .setTitle("Failed")
                    .show()
            }
        } catch (e: Exception) {

        }
    }

    private fun updatePreference() {
        PreferenceHelper.writeBooleanToPreference(KEY_PASSENGER_LOGGED_IN, true)
        PreferenceHelper.writeStringToPreference(KEY_PASSENGER_GOOGLE_ID, uId)
        PreferenceHelper.writeStringToPreference(KEY_PASSENGER_DISPLAY_NAME, name)
        PreferenceHelper.writeStringToPreference(KEY_PASSENGER_USER_GOOGLE_GMAIL, email)
        PreferenceHelper.writeStringToPreference(KEY_PASSENGER_PROFILE_URL, profileUrl)
    }

    private fun saveUser() {
        val hashMap: HashMap<String, String> = HashMap<String, String>()
        hashMap["name"] = name
        hashMap["email"] = email
        hashMap["userId"] = userId
        hashMap["uId"] = uId
        hashMap["profileUrl"] = profileUrl
        userDatabaseRef.child("Riders").child(userId).setValue(hashMap)
            .addOnSuccessListener {
                AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.SUCCESS)
                    .setTitle("Completed")
                    .show()
                redirect()
            }
            .addOnFailureListener {
                AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.ERROR)
                    .setTitle("Failed")
                    .show()
            }

    }

    private fun redirect() {
        pDialog.cancel()
        val intent = Intent(this, RiderHomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}