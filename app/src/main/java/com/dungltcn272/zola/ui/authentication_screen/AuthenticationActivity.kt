package com.dungltcn272.zola.ui.authentication_screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.dungltcn272.zola.R
import com.dungltcn272.zola.databinding.ActivityAuthenticationBinding
import com.dungltcn272.zola.ui.main_screen.MainActivity
import com.dungltcn272.zola.utils.Constants
import com.dungltcn272.zola.utils.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthenticationBinding
    private var encodedImage: String? = null
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(applicationContext)
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseFirestore.getInstance()
        setListener()
    }

    private fun setListener() {
        binding.tvGoToSignUp.setOnClickListener {
            goToNextScreenFlipper()
        }
        binding.tvGoToSignIn.setOnClickListener {
            goToPrevScreenFlipper()
        }
        binding.btnSignIn.setOnClickListener {
            hideKeyboard()
            if (isValidSignInDetail()) {
                signIn()
            }
        }
        binding.btnSignUp.setOnClickListener {
            hideKeyboard()
            if (isValidSignUpDetail()) {
                signUp()
            }
        }
        binding.layoutImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickImage.launch(intent)
        }
        binding.layout.setOnClickListener {
            hideKeyboard()
        }
    }

    private fun signIn() {
        loading(true)
        val email = binding.edtEmailSignIn.text.toString().trim()
        val password = binding.edtPasswordSignIn.text.toString().trim()
        database.collection(Constants.KEY_COLLECTION_USER)
            .whereEqualTo(Constants.KEY_EMAIL, email)
            .whereEqualTo(Constants.KEY_PASSWORD, password)
            .get()
            .addOnCompleteListener { task ->
                loading(false)
                if (task.isSuccessful && task.result != null && task.result.documents.size > 0) {
                    val documentSnapshot = task.result.documents[0]
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                    preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.id)
                    preferenceManager.putString(
                        Constants.KEY_NAME,
                        documentSnapshot.getString(Constants.KEY_NAME)!!
                    )
                    preferenceManager.putString(
                        Constants.KEY_IMAGE,
                        documentSnapshot.getString(Constants.KEY_IMAGE)!!
                    )
                    preferenceManager.putString(
                        Constants.KEY_EMAIL,
                        documentSnapshot.getString(Constants.KEY_EMAIL)!!
                    )
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    loading(false)
                    showToast("Unable to sign in")
                }
            }
            .addOnFailureListener {
                showToast(it.message.toString())
            }

    }

    private fun signUp() {
        loading(true)
        val user = hashMapOf<String, Any>()
        val name = binding.edtUsernameSignUp.text.toString().trim()
        val email = binding.edtEmailSignUp.text.toString().trim()
        val password = binding.edtPasswordSignUp.text.toString().trim()
        user[Constants.KEY_NAME] = name
        user[Constants.KEY_EMAIL] = email
        user[Constants.KEY_PASSWORD] = password
        user[Constants.KEY_IMAGE] = encodedImage.toString()

        database.collection(Constants.KEY_COLLECTION_USER)
            .whereEqualTo(Constants.KEY_EMAIL, email)
            .get()
            .addOnCompleteListener { task ->
                loading(false)
                if (task.isSuccessful) {
                    if (task.result != null) {
                        if (task.result.isEmpty) {
                            addUserToDatabase(user)
                        } else {
                            showToast("Email is already exits!!!")
                        }
                    }
                } else {
                    showToast(task.exception.toString())
                }

            }.addOnFailureListener {
                loading(false)
                Log.e("SignUp", it.message.toString())
                showToast(it.message.toString())
            }

    }

    private fun addUserToDatabase(user: HashMap<String, Any>) {
        database.collection(Constants.KEY_COLLECTION_USER)
            .add(user)
            .addOnSuccessListener {
                loading(false)
                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                preferenceManager.putString(Constants.KEY_USER_ID, it.id)
                preferenceManager.putString(Constants.KEY_NAME, user[Constants.KEY_NAME].toString())
                preferenceManager.putString(
                    Constants.KEY_EMAIL,
                    user[Constants.KEY_EMAIL].toString()
                )
                preferenceManager.putString(Constants.KEY_IMAGE, encodedImage.toString())
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                loading(false)
                Log.e("SignUp", it.message.toString())
                showToast(it.message.toString())
            }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)

        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)

    }

    private val pickImage: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val imageUri = result.data?.data
                    try {
                        val inputStream = contentResolver.openInputStream(imageUri!!)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.imgProfile.setImageBitmap(bitmap)
                        binding.tvAddImage.visibility = View.GONE
                        encodedImage = encodeImage(bitmap)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }

        }

    private fun isValidSignInDetail(): Boolean {
        return if (binding.edtEmailSignIn.text.toString().isEmpty()) {
            showToast("Enter email")
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.edtEmailSignIn.text.toString())
                .matches()
        ) {
            showToast("Email invalidate")
            false
        } else if (binding.edtPasswordSignIn.text.toString().isEmpty()) {
            showToast("Enter password")
            false
        } else {
            true
        }
    }


    private fun isValidSignUpDetail(): Boolean {
        if (encodedImage == null) {
            showToast("Select profile image")
            return false
        } else if (binding.edtUsernameSignUp.text.toString().isEmpty()) {
            showToast("Enter name")
            return false
        } else if (binding.edtEmailSignUp.text.toString().isEmpty()) {
            showToast("Enter email")
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.edtEmailSignUp.text.toString())
                .matches()
        ) {
            showToast("Email invalidate")
            return false
        } else if (binding.edtPasswordSignUp.text.toString().isEmpty()) {
            showToast("Enter password")
            return false
        } else if (binding.edtConfirmPasswordSignUp.text.toString().isEmpty()) {
            showToast("Enter confirm password")
            return false
        } else if (binding.edtPasswordSignUp.text.toString() != binding.edtConfirmPasswordSignUp.text.toString()) {
            showToast("Password with confirm password must equal")
            return false
        } else {
            return true
        }
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnSignUp.visibility = View.INVISIBLE
            binding.btnSignIn.visibility = View.INVISIBLE
            binding.progressBarSignUp.visibility = View.VISIBLE
            binding.progressBarSignIn.visibility = View.VISIBLE
        } else {
            binding.btnSignUp.visibility = View.VISIBLE
            binding.btnSignIn.visibility = View.VISIBLE
            binding.progressBarSignUp.visibility = View.INVISIBLE
            binding.progressBarSignIn.visibility = View.INVISIBLE
        }
    }


    private fun goToNextScreenFlipper() {
        binding.flipper.apply {
            setInAnimation(applicationContext, R.anim.slide_in_right)
            setOutAnimation(applicationContext, R.anim.slide_out_left)
            showNext()
        }
    }

    private fun goToPrevScreenFlipper() {
        binding.flipper.apply {
            setInAnimation(applicationContext, R.anim.slide_in_left)
            setOutAnimation(applicationContext, R.anim.slide_out_right)
            showPrevious()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

}