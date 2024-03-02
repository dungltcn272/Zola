package com.dungltcn272.zola.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dungltcn272.zola.utils.Constants
import com.dungltcn272.zola.utils.PreferenceManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

open class BaseActivity : AppCompatActivity() {
    private lateinit var documentRef : DocumentReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferenceManager =  PreferenceManager(applicationContext)
        val database = FirebaseFirestore.getInstance()
        documentRef = database.collection(Constants.KEY_COLLECTION_USER)
            .document(preferenceManager.getString(Constants.KEY_USER_ID)!!)
    }

    override fun onPause() {
        super.onPause()
        documentRef.update(Constants.KEY_AVAILABILITY, 0)
    }

    override fun onResume() {
        super.onResume()
        documentRef.update(Constants.KEY_AVAILABILITY, 1)
    }
}