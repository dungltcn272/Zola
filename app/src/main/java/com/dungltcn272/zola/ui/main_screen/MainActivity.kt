package com.dungltcn272.zola.ui.main_screen

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import com.dungltcn272.zola.base.BaseActivity
import com.dungltcn272.zola.databinding.ActivityMainBinding
import com.dungltcn272.zola.model.ChatMessage
import com.dungltcn272.zola.model.User
import com.dungltcn272.zola.ui.authentication_screen.AuthenticationActivity
import com.dungltcn272.zola.ui.chat_screen.ChatActivity
import com.dungltcn272.zola.ui.main_screen.adapter.RecentConversationAdapter
import com.dungltcn272.zola.ui.search_screen.SearchActivity
import com.dungltcn272.zola.utils.Constants
import com.dungltcn272.zola.utils.PreferenceManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var conversationAdapter: RecentConversationAdapter
    private lateinit var conversations : MutableList<ChatMessage>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        loadUserDetails()
        getToken()
        setListener()
        listenerConversation()
    }

    private fun init() {
        preferenceManager = PreferenceManager(applicationContext)
        database = FirebaseFirestore.getInstance()
        conversations = mutableListOf()
        conversationAdapter = RecentConversationAdapter(preferenceManager.getString(Constants.KEY_USER_ID)!!)
        binding.rcvRecentConversation.adapter = conversationAdapter
        conversationAdapter.onItemClick = { chatMessage ->
            val user = User()
            user.image = chatMessage.conversationImage
            user.name = chatMessage.conversationName
            if(chatMessage.senderId == preferenceManager.getString(Constants.KEY_USER_ID)){
                user.id = chatMessage.receiverId
            }else {
                user.id = chatMessage.senderId
            }
            val intent = Intent(applicationContext, ChatActivity::class.java)
            intent.putExtra(Constants.KEY_USER, user)
            startActivity(intent)
        }
    }

    private fun setListener() {
        binding.imgSignOut.setOnClickListener {
            signOut()
        }
        binding.layoutSearch.setOnClickListener {
            goToSearchScreen()
        }
    }

    private fun goToSearchScreen() {
        val intent = Intent(applicationContext, SearchActivity::class.java)
        startActivity(intent)
    }

    private fun listenerConversation(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener)
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener)
    }

    private val eventListener: EventListener<QuerySnapshot> = EventListener { value, error ->
        if (error != null) {
            return@EventListener
        }
        if (value != null) {
            for (documentChange in value.documentChanges) {
                if (documentChange.type == DocumentChange.Type.ADDED) {
                    val senderId = documentChange.document.getString(Constants.KEY_SENDER_ID)
                    val receiverId = documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                    val chatMessage = ChatMessage()
                    chatMessage.receiverId = receiverId!!
                    chatMessage.senderId = senderId!!
                    if (preferenceManager.getString(Constants.KEY_USER_ID) == senderId) {
                        chatMessage.conversationImage =
                            documentChange.document.getString(Constants.KEY_RECEIVER_IMAGE)!!
                        chatMessage.conversationName =
                            documentChange.document.getString(Constants.KEY_RECEIVER_NAME)!!
                        chatMessage.conversationId =
                            documentChange.document.getString(Constants.KEY_RECEIVER_ID)!!
                    } else {
                        chatMessage.conversationImage =
                            documentChange.document.getString(Constants.KEY_SENDER_IMAGE)!!
                        chatMessage.conversationName =
                            documentChange.document.getString(Constants.KEY_SENDER_NAME)!!
                        chatMessage.conversationId =
                            documentChange.document.getString(Constants.KEY_SENDER_ID)!!
                    }
                    chatMessage.message =
                        documentChange.document.getString(Constants.KEY_LAST_MESSAGE)!!
                    chatMessage.dateObject =
                        documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                    conversations.add(chatMessage)
                } else if (documentChange.type == DocumentChange.Type.MODIFIED) {
                    for (conversation in conversations) {
                        val senderId = documentChange.document.getString(Constants.KEY_SENDER_ID)
                        val receiverId =
                            documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                        if (conversation.senderId == senderId && conversation.receiverId == receiverId) {
                            conversation.message =
                                documentChange.document.getString(Constants.KEY_LAST_MESSAGE)!!
                            conversation.dateObject =
                                documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                            break
                        }
                    }
                }
            }
            conversations.sortByDescending { it.dateObject }
            conversationAdapter.differ.submitList(conversations.toList())
            conversationAdapter.notifyDataSetChanged()
            if(conversations.size >0){
                binding.rcvRecentConversation.scrollToPosition(0)
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun signOut() {
        showToast("Signing out ...")
        // delete token
        val documentReference = database.collection(Constants.KEY_COLLECTION_USER)
            .document(preferenceManager.getString(Constants.KEY_USER_ID).toString())
        val updates = hashMapOf<String, Any>()
        updates[Constants.KEY_FCM_TOKEN] = FieldValue.delete()

        documentReference.update(updates)
            .addOnSuccessListener {
                showToast("Sign out success!!!")
            }
            .addOnFailureListener {
                showToast("Unable to sign out!!!")
            }

        preferenceManager.clear()
        val intent = Intent(applicationContext, AuthenticationActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loadUserDetails() {
        val encodedImage = preferenceManager.getString(Constants.KEY_IMAGE).toString()
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imgProfile.setImageBitmap(bitmap)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.d("Update", "getToken: $it")
            updateToken(it)
        }.addOnFailureListener {
            Log.e("Error", "getToken: $it")
        }
    }

    private fun updateToken(token: String) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token)
        val documentReference = database.collection(Constants.KEY_COLLECTION_USER).document(
            preferenceManager.getString(Constants.KEY_USER_ID)!!
        )
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
            .addOnSuccessListener { showToast("Token update successfully") }
            .addOnFailureListener { showToast("Unable to update token") }
    }

}