package com.masai.uber_rider.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.masai.uber_rider.databinding.ActivityGetStartedBinding


class GetStartedActivity : AppCompatActivity() {
    private var binding: ActivityGetStartedBinding? = null
    private lateinit var mAuth: FirebaseAuth

    /** companion object {
    private const val LOGIN_REQUEST_CODE = 7171
    }
    lateinit var provider: List<AuthUI.IdpConfig>
    lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var database: FirebaseDatabase
    private lateinit var riderInfoRef: DatabaseReference

    private fun displaySplashScreen() {
    Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
    .subscribe {
    mAuth.addAuthStateListener(listener)
    }
    }
     **/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        val view = binding!!.root
        setContentView(view)
        binding!!.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    /**private fun init() {
    firebaseAuth = FirebaseAuth.getInstance()
    database = FirebaseDatabase.getInstance()
    riderInfoRef = database.getReference(Common.RIDER_INFO_REFERENCE)
    provider = Arrays.asList(
    AuthUI.IdpConfig.PhoneBuilder().build(),
    AuthUI.IdpConfig.GoogleBuilder().build()
    )
    listener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
    val user = myFirebaseAuth.currentUser
    if (user != null) {
    checkUserFromFirebase()
    } else {
    showLoginLayout()
    }
    }
    }**/

    /**   private fun showLoginLayout() {
    val authMethodPickerLayout = AuthMethodPickerLayout
    .Builder(R.layout.layout_sign_in)
    .setPhoneButtonId(R.id.btn_phone_sign_in)
    .setGoogleButtonId(R.id.btn_google_sign_in)
    .build()
    startActivityForResult(
    AuthUI.getInstance()
    .createSignInIntentBuilder()
    .setAuthMethodPickerLayout(authMethodPickerLayout)
    .setTheme(R.style.LoginTheme)
    .setAvailableProviders(provider)
    .setIsSmartLockEnabled(false)
    .build(), LOGIN_REQUEST_CODE
    )
    }  **/

    /**  private fun checkUserFromFirebase() {
    riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
    .addListenerForSingleValueEvent(object : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) {
    if (snapshot.exists()) {
    val model = snapshot.getValue(RiderModel::class.java)
    gotToHomeActivity(model)
    } else {
    showRegisterLayout()
    }
    }

    override fun onCancelled(error: DatabaseError) {
    Toast.makeText(this@GetStartedActivity, error.message, Toast.LENGTH_LONG).show()
    }

    })
    }  **/

    /**  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == LOGIN_REQUEST_CODE) {
    val response = IdpResponse.fromResultIntent(data)
    if (resultCode == Activity.RESULT_OK) {
    val user = FirebaseAuth.getInstance().currentUser
    } else {
    Toast.makeText(
    this@GetStartedActivity, response!!.error!!.message,
    Toast.LENGTH_LONG
    ).show()
    }
    }

    }  **/

    /** private fun showRegisterLayout() {
    val builder = AlertDialog
    .Builder(this, R.style.DialogTheme)
    val itemView = LayoutInflater
    .from(this).inflate(R.layout.layout_register, null)

    val edit_first_name = itemView.findViewById<View>(R.id.edt_first_name) as EditText
    val edit_last_name = itemView.findViewById<View>(R.id.edt_last_name) as EditText
    val edit_phone_number = itemView.findViewById<View>(R.id.edt_phone_number) as EditText
    val btn_continue = itemView.findViewById<View>(R.id.btn_register) as Button

    //set Data
    if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
    !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
    )
    edit_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

    //View
    builder.setView(itemView)
    val dialog = builder.create()
    dialog.show()

    //Event
    btn_continue.setOnClickListener {
    if (TextUtils.isDigitsOnly(edit_first_name.text.toString())) {
    Toast.makeText(
    this@GetStartedActivity, "Please enter First Name",
    Toast.LENGTH_SHORT
    ).show()
    return@setOnClickListener
    } else if (TextUtils.isDigitsOnly(edit_last_name.text.toString())) {
    Toast.makeText(
    this@GetStartedActivity, "Please enter Last Name",
    Toast.LENGTH_SHORT
    ).show()
    return@setOnClickListener
    } else if (TextUtils.isDigitsOnly(edit_phone_number.text.toString())) {
    Toast.makeText(
    this@GetStartedActivity, "Please enter Phone Number",
    Toast.LENGTH_SHORT
    ).show()
    return@setOnClickListener
    } else {
    val model = RiderModel()
    model.firstName = edit_first_name.text.toString()
    model.lastName = edit_last_name.text.toString()
    model.phoneNumber = edit_phone_number.text.toString()
    riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
    .setValue(model)
    .addOnFailureListener { e ->
    Toast.makeText(
    this@GetStartedActivity, ""+e.message,
    Toast.LENGTH_SHORT
    ).show()
    dialog.dismiss()

    }
    .addOnSuccessListener {
    Toast.makeText(
    this@GetStartedActivity, "Register Successfully",
    Toast.LENGTH_SHORT
    ).show()
    dialog.dismiss()
    gotToHomeActivity(model)
    }
    }
    }


    }  **/

    /**  private fun gotToHomeActivity(model: RiderModel?) {
    Common.currentRider = model
    startActivity(Intent(this,RiderHomeActivity::class.java))
    finish()
    } **/

    /**override fun onStop() {
    if (firebaseAuth != null && listener != null) {
    firebaseAuth.removeAuthStateListener(listener)
    }
    super.onStop()
    } **/

    /**override fun onStart() {
    super.onStart()
    displaySplashScreen()
    }**/

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}