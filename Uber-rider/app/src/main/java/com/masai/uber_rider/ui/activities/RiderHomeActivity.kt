package com.masai.uber_rider.ui.activities

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.cazaea.sweetalert.SweetAlertDialog
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.masai.uber_rider.R
import com.masai.uber_rider.databinding.ActivityHomeBinding
import com.masai.uber_rider.databinding.DialogCustomImageSelectionBinding
import com.masai.uber_rider.utils.KEY_PASSENGER_DISPLAY_NAME
import com.masai.uber_rider.utils.KEY_PASSENGER_PROFILE_URL
import com.masai.uber_rider.utils.PreferenceHelper
import com.masai.uber_rider.utils.UserUtils
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType
import de.hdodenhof.circleimageview.CircleImageView


class RiderHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var navController: NavController

    private lateinit var pDialog: SweetAlertDialog

    private lateinit var mAuth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var profilePicRef: StorageReference

    private lateinit var userId: String
    private var profileImage: String? = null
    private var url: String? = null
    private lateinit var name: String

    private lateinit var view: View
    private lateinit var tvName: TextView
    private lateinit var ivImage: CircleImageView

    companion object {
        private const val CAMERA = 1
        private const val GALLERY = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarHome.toolbar)

        initViews()

        navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.signOut) {
                val sDialog =
                    SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                sDialog.titleText = "Sign out"
                sDialog.contentText = "Are you sure!"
                sDialog.cancelText = "No"
                sDialog.confirmText = "Yes"
                sDialog.showCancelButton(true)
                sDialog.progressHelper.barColor = Color.parseColor("#CF7351")
                sDialog.setCancelable(true)
                sDialog.setCancelClickListener { sDialog.cancel(); }

                sDialog.setConfirmClickListener {
                    FirebaseAuth.getInstance().signOut()
                    redirect()
                }
                sDialog.show()
            }

            true
        }

        ivImage.setOnClickListener {
            customImageSelectionDialog()
        }
    }

    private fun initViews() {
        PreferenceHelper.getSharedPreferences(this)
        mAuth = FirebaseAuth.getInstance()
        userId = mAuth.currentUser?.uid.toString()
        val user = mAuth.currentUser
        if (user != null) {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.ERROR)
                            .setTitle("Failed")
                            .show()
                        return@OnCompleteListener
                    }
                    // Get new FCM registration token
                    val token = task.result
                    Log.d("Token", token.toString())
                    UserUtils.updateToken(this, token)
                })
        }
        databaseRef = FirebaseDatabase.getInstance().reference
        profilePicRef = FirebaseStorage.getInstance().reference

        drawerLayout = binding.drawerLayout
        navView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        view = navView.getHeaderView(0)
        tvName = view.findViewById<TextView>(R.id.tvNameHeader)
        ivImage = view.findViewById<CircleImageView>(R.id.profile_image_header)

        pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        pDialog.progressHelper.barColor = Color.parseColor("#CF7351")
        pDialog.titleText = "Please wait !"
        pDialog.setCancelable(false)

        name = PreferenceHelper.getStringFromPreference(KEY_PASSENGER_DISPLAY_NAME).toString()
        tvName.text = name
        profileImage =
            PreferenceHelper.getStringFromPreference(KEY_PASSENGER_PROFILE_URL).toString()
        url =
            PreferenceHelper.getStringFromPreference(KEY_PASSENGER_PROFILE_URL).toString()

        if (profileImage != null) {
            Glide.with(ivImage).load(profileImage).placeholder(R.drawable.ic_user_1)
                .into(ivImage)
        }
        if (url != null) {
            Glide.with(ivImage).load(url).placeholder(R.drawable.ic_user_1)
                .into(ivImage)
        }

    }

    private fun customImageSelectionDialog() {
        val dialog = Dialog(this)

        val binding: DialogCustomImageSelectionBinding =
            DialogCustomImageSelectionBinding.inflate(layoutInflater)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        dialog.setContentView(binding.root)
        binding.tvCamera.setOnClickListener {

            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            // Here after all the permission are granted launch the CAMERA to capture an image.
                            if (report.areAllPermissionsGranted()) {
                                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                startActivityForResult(intent, CAMERA)
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
            dialog.dismiss()
        }

        binding.tvGallery.setOnClickListener {
            Dexter.withContext(this)
                .withPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .withListener(object : PermissionListener {

                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                        // Here after all the permission are granted launch the gallery to select and image.
                        val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        startActivityForResult(galleryIntent, GALLERY)
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        AestheticDialog.Builder(
                            this@RiderHomeActivity,
                            DialogStyle.TOASTER,
                            DialogType.WARNING
                        )
                            .setTitle("Warning !")
                            .setMessage("You have denied the storage permission to select image.")
                            .show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()

            dialog.dismiss()
        }
        //Start the dialog and display it on screen.
        dialog.show()
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val filePath: StorageReference = profilePicRef.child("DriverProfile")
            .child("$userId.jpg")
        if (requestCode == CAMERA) {
            val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
            pDialog.progressHelper.barColor = Color.parseColor("#CF7351")
            pDialog.titleText = "Loading"
            pDialog.setCancelable(false)
            pDialog.show()
            data?.extras?.let {
                val thumbnail: Uri =
                    data.extras!!.get("data") as Uri // Bitmap from camera
                ivImage.setImageURI(thumbnail)
                if (thumbnail != null) {
                    val uploadTask: UploadTask = filePath.putFile(thumbnail)
                    uploadTask.addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot?> {
                        profilePicRef.child("DriverProfile")
                            .child("$userId.jpg").downloadUrl.addOnSuccessListener(
                                OnSuccessListener<Uri> { uri ->
                                    val url = uri.toString()
                                    profileImage = url.toString()
                                    saveToDatabase()
                                    pDialog.cancel()
//                                    AestheticDialog.Builder(
//                                        this,
//                                        DialogStyle.TOASTER,
//                                        DialogType.SUCCESS
//                                    )
//                                        .setTitle("Uploaded Successfully")
//                                        .show()
                                })
                    })

                }

            }
        } else if (requestCode == GALLERY) {

            val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
            pDialog.progressHelper.barColor = Color.parseColor("#CF7351")
            pDialog.titleText = "Loading"
            pDialog.setCancelable(false)
            pDialog.show()
            data?.let {
                // Here we will get the select image URI.
                val thumbnail: Uri? = data.data
                ivImage.setImageURI(thumbnail)
                if (thumbnail != null) {
                    val uploadTask: UploadTask = filePath.putFile(thumbnail)
                    uploadTask.addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot?> {
                        profilePicRef.child("DriverProfile")
                            .child("$userId.jpg").downloadUrl.addOnSuccessListener(
                                OnSuccessListener<Uri> { uri ->
                                    val url = uri.toString()
                                    profileImage = url.toString()
                                    saveToDatabase()
                                    pDialog.cancel()
//                                    AestheticDialog.Builder(
//                                        this,
//                                        DialogStyle.TOASTER,
//                                        DialogType.SUCCESS
//                                    )
//                                        .setTitle("Uploaded Successfully")
//                                        .show()
                                })
                    })

                }
            }

        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("tag", "Cancelled")
        }

    }

    private fun saveToDatabase() {
        PreferenceHelper.writeStringToPreference(KEY_PASSENGER_PROFILE_URL, url)
        databaseRef.child("Riders").child(userId)
            .child("profileurl")
            .setValue(profileImage)
            .addOnSuccessListener {
//                AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.SUCCESS)
//                    .setTitle("Success")
//                    .show()
            }
            .addOnFailureListener {
//                AestheticDialog.Builder(this, DialogStyle.TOASTER, DialogType.ERROR)
//                    .setTitle("Failed")
//                    .show()
            }


        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.hasChild("profileurl")) {
                        profileImage = dataSnapshot.child("profileurl").value.toString()
                        Glide.with(ivImage).load(profileImage).placeholder(R.drawable.ic_user_1)
                            .into(ivImage)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }

        databaseRef.addValueEventListener(postListener)

    }


    private fun redirect() {
        val intent = Intent(
            this,
            SplashActivity::class.java
        )
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}