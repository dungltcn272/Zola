package com.dungltcn272.zola.ui.search_screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.dungltcn272.zola.base.BaseActivity
import com.dungltcn272.zola.databinding.ActivitySearchBinding
import com.dungltcn272.zola.model.User
import com.dungltcn272.zola.ui.chat_screen.ChatActivity
import com.dungltcn272.zola.ui.search_screen.adapter.UserAdapter
import com.dungltcn272.zola.utils.Constants
import com.dungltcn272.zola.utils.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchActivity : BaseActivity() {
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore
    private lateinit var binding: ActivitySearchBinding
    private lateinit var userAdapter : UserAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(applicationContext)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseFirestore.getInstance()
        userAdapter =UserAdapter()

        setListener()
        getUsers()

    }

    private fun setListener() {
        binding.imgCancel.setOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed()
        }
        searchEventListener()

        userAdapter.onItemClick = {user ->
            val intent = Intent(applicationContext, ChatActivity::class.java)
            intent.putExtra(Constants.KEY_USER, user)
            startActivity(intent)
            finish()
        }

        binding.root.setOnClickListener {
            hideKeyboard()
        }
    }

    private fun searchEventListener() {
        var searchJob: Job? = null

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()

                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    searchWithKeyword(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        }

        binding.edtSearch.addTextChangedListener(textWatcher)
    }

    private fun searchWithKeyword(keyword: String) {
        if (keyword.isEmpty()){
            getUsers()
            return
        }
        loading(true)
        database.collection(Constants.KEY_COLLECTION_USER)
            .get()
            .addOnSuccessListener { querySnapshot ->
                loading(false)
                val currentUserId = preferenceManager.getString(Constants.KEY_USER_ID)
                val users = mutableListOf<User>()
                for (queryDocumentSnapshot in querySnapshot) {
                    if(currentUserId == queryDocumentSnapshot.id){
                        continue
                    }
                    if(queryDocumentSnapshot.getString(Constants.KEY_NAME)?.contains(keyword) == true){
                        val user = User().apply {
                            name = queryDocumentSnapshot.getString(Constants.KEY_NAME)!!
                            email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL)!!
                            image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE)!!
                            queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN)?.let {
                                token = it
                            }
                            id = queryDocumentSnapshot.id
                        }
                        users.add(user)
                    }

                }
                if (users.size > 0) {
                    userAdapter.differ.submitList(users)
                    binding.rcvUser.adapter = userAdapter
                    binding.rcvUser.visibility = View.VISIBLE
                } else {
                    showErrorMessage()
                    binding.rcvUser.visibility = View.GONE
                }

            }
            .addOnFailureListener { exception ->
                loading(false)
                Log.e("OK", "Error getting documents: $exception")
            }
    }

    private fun getUsers() {
        loading(true)
        database.collection(Constants.KEY_COLLECTION_USER)
            .get()
            .addOnCompleteListener { task ->
                loading(false)
                val currentUserId = preferenceManager.getString(Constants.KEY_USER_ID)

                if (task.isSuccessful && task.result != null) {
                    val users = mutableListOf<User>()
                    for (queryDocumentSnapshot in task.result) {
                        if (currentUserId == queryDocumentSnapshot.id) {
                            continue
                        }

                        val user = User()
                        user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME)!!
                        user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL)!!
                        user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE)!!
                        queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN)?.let {
                            user.token = it
                        }
                        user.id = queryDocumentSnapshot.id
                        users.add(user)
                    }
                    if (users.size > 0) {
                        userAdapter.differ.submitList(users)
                        binding.rcvUser.adapter = userAdapter
                        binding.rcvUser.visibility = View.VISIBLE
                    } else {
                        showErrorMessage()
                    }
                } else {
                    showErrorMessage()
                }
            }

    }

    private fun showErrorMessage() {
        binding.tvError.text = String.format("%s", "No user available")
        binding.tvError.visibility = View.VISIBLE
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rcvUser.visibility = View.GONE
            binding.tvError.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.rcvUser.visibility = View.VISIBLE
            binding.tvError.visibility = View.GONE
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }


}