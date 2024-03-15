package com.dungltcn272.zola.ui.profile_screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.dungltcn272.zola.base.BaseActivity
import com.dungltcn272.zola.databinding.ActivityProfileBinding
import com.dungltcn272.zola.model.User
import com.dungltcn272.zola.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var encodedImage: String? = null
    private lateinit var thisUser: User
    private var isCurrentUser = false
    private lateinit var database: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        database = FirebaseFirestore.getInstance()
        setContentView(binding.root)

        getInformationFromIntent()
        setListener()
    }

    private fun getInformationFromIntent() {
        @Suppress("DEPRECATION")
        thisUser = intent.getSerializableExtra(Constants.KEY_USER) as User
        isCurrentUser = intent.getBooleanExtra(Constants.KEY_IS_CURRENT_USER, false)
        encodedImage = thisUser.image
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imgProfile.setImageBitmap(bitmap)
        if (isCurrentUser) {
            binding.edtUsername.isEnabled = true
            binding.tvEmail.visibility = View.VISIBLE
            binding.btnUpdate.visibility = View.VISIBLE
        } else {
            binding.tvEmail.visibility = View.GONE
            binding.edtUsername.isEnabled = false
            binding.btnUpdate.visibility = View.GONE
        }
        binding.edtUsername.setText(thisUser.name)
        binding.tvEmail.text = thisUser.email
    }

    private fun setListener() {
        if (isCurrentUser) {
            binding.layoutImage.setOnClickListener {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pickImage.launch(intent)
            }
        }
        binding.btnUpdate.setOnClickListener {
            updateInformation()
        }
        binding.root.setOnClickListener {
            hideKeyboard()
        }
        binding.icBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun updateInformation() {
        val newName = binding.edtUsername.text.toString().trim()
        if (encodedImage != thisUser.image || newName != thisUser.name) {
            database.collection(Constants.KEY_COLLECTION_USER).document(thisUser.id)
                .update(Constants.KEY_IMAGE, encodedImage, Constants.KEY_NAME, newName)
                .addOnSuccessListener {
                    showToast("Update successfully")
                }.addOnFailureListener {
                    showToast("Update failed")
                }
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
                        encodedImage = encodeImage(bitmap)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
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