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
import com.dungltcn272.zola.ui.profile_screen.ProfileActivity
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
    private lateinit var currentUser: User
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var conversationAdapter: RecentConversationAdapter
    private var encodedImage : String? = null
    private lateinit var conversations: MutableList<ChatMessage>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        database = FirebaseFirestore.getInstance()
        preferenceManager = PreferenceManager(applicationContext)
        setContentView(binding.root)
        loadUserDetails()
        init()
        getToken()
        setListener()
        listenerConversation()
    }

    private fun loadUserDetails() {
        currentUser = User().apply {
            name = preferenceManager.getString(Constants.KEY_NAME)!!
            id = preferenceManager.getString(Constants.KEY_USER_ID)!!
            image = preferenceManager.getString(Constants.KEY_IMAGE)!!
            email = preferenceManager.getString(Constants.KEY_EMAIL)!!
        }
        encodedImage = currentUser.image
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imgProfile.setImageBitmap(bitmap)
        database.collection(Constants.KEY_COLLECTION_USER).document(currentUser.id).addSnapshotListener { value, error ->
            if (error != null){
                return@addSnapshotListener
            }
            if (value != null){
                preferenceManager.putString(Constants.KEY_NAME, value.getString(Constants.KEY_NAME)!!)
                preferenceManager.putString(Constants.KEY_IMAGE, encodedImage!!)
            }
        }
    }

    private fun init() {
        conversations = mutableListOf()
        conversationAdapter = RecentConversationAdapter(currentUser.id)
        binding.rcvRecentConversation.adapter = conversationAdapter
        conversationAdapter.onItemClick = { chatMessage ->
            val user = User()
            user.image = chatMessage.conversationImage
            user.name = chatMessage.conversationName
            if (chatMessage.senderId == currentUser.id) {
                user.id = chatMessage.receiverId
            } else {
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
        binding.imgProfile.setOnClickListener {
            val intent = Intent(applicationContext, ProfileActivity::class.java)
            intent.putExtra(Constants.KEY_USER, currentUser)
            intent.putExtra(Constants.KEY_IS_CURRENT_USER, true)
            startActivity(intent)
        }
    }

    private fun goToSearchScreen() {
        val intent = Intent(applicationContext, SearchActivity::class.java)
        startActivity(intent)
    }

    private fun listenerConversation() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereArrayContains(Constants.KEY_USERS_ID_ARRAY, currentUser.id)
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
                    chatMessage.conversationId = documentChange.document.id
                    chatMessage.receiverSeen =
                        documentChange.document.getBoolean(Constants.KEY_RECEIVER_SEEN)!!
                    if (currentUser.id == senderId) {
                        chatMessage.conversationImage =
                            documentChange.document.getString(Constants.KEY_RECEIVER_IMAGE)!!
                        chatMessage.conversationName =
                            documentChange.document.getString(Constants.KEY_RECEIVER_NAME)!!
                    } else {
                        chatMessage.conversationImage =
                            documentChange.document.getString(Constants.KEY_SENDER_IMAGE)!!
                        chatMessage.conversationName =
                            documentChange.document.getString(Constants.KEY_SENDER_NAME)!!
                    }
                    chatMessage.usersIdArray = mutableListOf(senderId, receiverId)
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
                        if (conversation.usersIdArray?.containsAll(
                                mutableListOf(
                                    senderId, receiverId
                                )
                            ) == true
                        ) {
                            conversation.senderId = senderId!!
                            conversation.receiverId = receiverId!!
                            conversation.message =
                                documentChange.document.getString(Constants.KEY_LAST_MESSAGE)!!
                            conversation.dateObject =
                                documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                            conversation.receiverSeen =
                                documentChange.document.getBoolean(Constants.KEY_RECEIVER_SEEN)!!
                            break
                        }
                    }
                }
            }
            conversations.sortByDescending { it.dateObject }
            conversationAdapter.differ.submitList(conversations.toList())
            conversationAdapter.notifyDataSetChanged()
            if (conversations.size > 0) {
                binding.rcvRecentConversation.scrollToPosition(0)
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun signOut() {
        showToast("Signing out ...")
        // delete token
        val documentReference =
            database.collection(Constants.KEY_COLLECTION_USER).document(currentUser.id)
        val updates = hashMapOf<String, Any>()
        updates[Constants.KEY_FCM_TOKEN] = FieldValue.delete()

        documentReference.update(updates).addOnSuccessListener {
            showToast("Sign out success!!!")
        }.addOnFailureListener {
            showToast("Unable to sign out!!!")
        }

        preferenceManager.clear()
        val intent = Intent(applicationContext, AuthenticationActivity::class.java)
        startActivity(intent)
        finish()
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
            currentUser.id
        )
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
            .addOnSuccessListener { showToast("Token update successfully") }
            .addOnFailureListener { showToast("Unable to update token") }
    }

}