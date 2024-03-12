package com.dungltcn272.zola.ui.chat_screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.dungltcn272.zola.base.BaseActivity
import com.dungltcn272.zola.databinding.ActivityChatBinding
import com.dungltcn272.zola.model.ChatMessage
import com.dungltcn272.zola.model.User
import com.dungltcn272.zola.network.ApiClient
import com.dungltcn272.zola.network.ApiService
import com.dungltcn272.zola.ui.chat_screen.adapter.ChatAdapter
import com.dungltcn272.zola.ui.profile_screen.ProfileActivity
import com.dungltcn272.zola.utils.Constants
import com.dungltcn272.zola.utils.PreferenceManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects

class ChatActivity : BaseActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var otherUser: User
    private lateinit var currentUser: User
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore
    private lateinit var chatMessages: MutableList<ChatMessage>
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var usersIdArray: MutableList<String>
    private var conversationId: String? = null
    private lateinit var listenRegistration : ListenerRegistration
    private var isReceiverAvailable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadDetails()
        init()

        setListener()
        listenerMessages()
    }

    private fun loadDetails() {
        @Suppress("DEPRECATION")
        otherUser = intent.getSerializableExtra(Constants.KEY_USER) as User
        binding.tvName.text = otherUser.name
        val encodedImage = otherUser.image
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imgProfileReceiver.setImageBitmap(bitmap)
        preferenceManager = PreferenceManager(applicationContext)
        currentUser = User().apply {
            id = preferenceManager.getString(Constants.KEY_USER_ID)!!
            name = preferenceManager.getString(Constants.KEY_NAME)!!
            image = preferenceManager.getString(Constants.KEY_IMAGE)!!
        }
        usersIdArray = mutableListOf(currentUser.id, otherUser.id)
        usersIdArray.sort()
    }

    private fun init() {
        database = FirebaseFirestore.getInstance()
        chatMessages = mutableListOf()
        chatAdapter = ChatAdapter(
            getBitmapFromEncodedString(otherUser.image),
            currentUser.id
        )
        binding.rcvChat.adapter = chatAdapter
    }

    private fun setListener() {
        binding.icBack.setOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed()
        }
        binding.btnSend.setOnClickListener {
            if (binding.edtMessage.text.toString().isNotEmpty()) {
                sendMessage()
            }
        }
        binding.root.setOnClickListener {
            hideKeyboard()
        }
        binding.rcvChat.setOnClickListener {
            hideKeyboard()
        }
        binding.icInfo.setOnClickListener {
            val intent = Intent(applicationContext, ProfileActivity::class.java)
            intent.putExtra(Constants.KEY_USER, otherUser)
            intent.putExtra(Constants.KEY_IS_CURRENT_USER, false)
            startActivity(intent)
        }
        binding.layoutImage.setOnClickListener{
            val intent = Intent(applicationContext, ProfileActivity::class.java)
            intent.putExtra(Constants.KEY_USER, otherUser)
            intent.putExtra(Constants.KEY_IS_CURRENT_USER, false)
            startActivity(intent)
        }
    }

    private fun listenerMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, currentUser.id)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, otherUser.id)
            .addSnapshotListener(eventListener)
        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, otherUser.id)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, currentUser.id)
            .addSnapshotListener(eventListener)
        listenRegistration = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_USERS_ID_ARRAY, usersIdArray)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null && !value.isEmpty) {
                    val document = value.documents[0]
                    if (document.getString(Constants.KEY_SENDER_ID) == otherUser.id
                        && document.getBoolean(Constants.KEY_RECEIVER_SEEN) == false
                    ) {
                        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                            .document(document.id).update(Constants.KEY_RECEIVER_SEEN, true)
                    }
                }
            }
    }

    private val eventListener: EventListener<QuerySnapshot> = EventListener { value, error ->

        if (error != null) {
            return@EventListener
        }
        if (value != null) {
            for (documentChange in value.documentChanges) {
                if (documentChange.type == DocumentChange.Type.ADDED) {
                    val chatMessage = ChatMessage()
                    chatMessage.senderId =
                        documentChange.document.getString(Constants.KEY_SENDER_ID)!!
                    chatMessage.receiverId =
                        documentChange.document.getString(Constants.KEY_RECEIVER_ID)!!
                    chatMessage.message = documentChange.document.getString(Constants.KEY_MESSAGE)!!
                    chatMessage.datetime =
                        getReadableDatetime(documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!)
                    chatMessage.dateObject =
                        documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                    chatMessages.add(chatMessage)
                }
            }
            chatMessages.sortBy {
                it.dateObject
            }
            chatAdapter.differ.submitList(chatMessages)
            if (chatMessages.size > 0) {
                chatAdapter.notifyItemRangeChanged(chatMessages.size, chatMessages.size)
                binding.rcvChat.smoothScrollToPosition(chatMessages.size - 1)
            }
        }
        if (conversationId == null) {
            checkForConversation()
        }
        binding.progressBar.visibility = View.GONE
    }

    private fun sendMessage() {
        val message = hashMapOf<String, Any>()
        message[Constants.KEY_SENDER_ID] = currentUser.id
        message[Constants.KEY_RECEIVER_ID] = otherUser.id
        message[Constants.KEY_MESSAGE] = binding.edtMessage.text.toString().trim()
        message[Constants.KEY_TIMESTAMP] = Date()

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message)
        Log.d("OK", "sendMessage: $conversationId")
        if (conversationId != null) {
            updateConversation(binding.edtMessage.text.toString().trim())
        } else {
            val conversion = hashMapOf<String, Any>()
            conversion[Constants.KEY_SENDER_ID] = currentUser.id
            conversion[Constants.KEY_SENDER_NAME] = currentUser.name
            conversion[Constants.KEY_SENDER_IMAGE] = currentUser.image
            conversion[Constants.KEY_RECEIVER_ID] = otherUser.id
            conversion[Constants.KEY_RECEIVER_IMAGE] = otherUser.image
            conversion[Constants.KEY_RECEIVER_NAME] = otherUser.name
            conversion[Constants.KEY_RECEIVER_SEEN] = false
            conversion[Constants.KEY_LAST_MESSAGE] = binding.edtMessage.text.toString().trim()
            conversion[Constants.KEY_TIMESTAMP] = Date()
            conversion[Constants.KEY_USERS_ID_ARRAY] = usersIdArray
            addConversation(conversion)
        }
        if (!isReceiverAvailable) {
            try {
                val tokens = JSONArray()
                tokens.put(otherUser.token)
                val data = JSONObject()
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME))
                data.put(Constants.KEY_MESSAGE, binding.edtMessage.text.toString().trim())

                val body = JSONObject()
                body.put(Constants.REMOTE_MSG_DATA, data)
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens)
                sendNotification(body.toString())
            } catch (e: Exception) {
                showToast(e.message.toString())
            }
        }
        binding.edtMessage.text = null

    }

    private fun updateConversation(message: String) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId!!)
            .update(
                Constants.KEY_LAST_MESSAGE,
                message,
                Constants.KEY_TIMESTAMP,
                Date(),
                Constants.KEY_RECEIVER_SEEN,
                false,
                Constants.KEY_SENDER_ID,
                currentUser.id,
                Constants.KEY_RECEIVER_ID,
                otherUser.id,
                Constants.KEY_SENDER_IMAGE,
                currentUser.image,
                Constants.KEY_RECEIVER_IMAGE,
                otherUser.image,
                Constants.KEY_SENDER_NAME,
                currentUser.name,
                Constants.KEY_RECEIVER_NAME,
                otherUser.name
            )
    }

    private fun addConversation(conversion: HashMap<String, Any>) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).add(conversion)
            .addOnSuccessListener { documentReferences ->
                conversationId = documentReferences.id
            }
    }

    private fun checkForConversation() {
        if (chatMessages.size > 0) {
            checkForConversationRemotely(
                usersIdArray
            )
        }
    }

    private fun checkForConversationRemotely(usersIdArray: MutableList<String>) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_USERS_ID_ARRAY, usersIdArray)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result.size() == 0) {
                        showToast("Can't read messages!!!")
                    } else {
                        val document = task.result.documents[0]
                        conversationId = document.id
                    }
                } else {
                    showToast("None conversation")
                }

            }
            .addOnFailureListener {
                showToast(it.message.toString())
            }

    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun sendNotification(messageBody: String) {
        ApiClient.getClient().create(ApiService::class.java)
            .sendMessage(Constants.getRemoteMsgHeaders(), messageBody)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        try {
                            if (response.body() != null) {
                                val responseJson = JSONObject(response.body()!!)
                                val results: JSONArray = responseJson.getJSONArray("results")
                                if (responseJson.getInt("failure") == 1) {
                                    val error = results[0] as JSONObject
                                    showToast("BUG" + error.getString("error"))
                                    return
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                        showToast("Notification sent successfully")
                    } else {
                        showToast("Error : " + response.code())
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    showToast(t.message.toString())
                }

            })
    }

    private fun getBitmapFromEncodedString(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    private fun getReadableDatetime(date: Date): String {
        return SimpleDateFormat("dd 'Th' MM hh:mm", Locale.getDefault()).format(date)
    }

    override fun onResume() {
        super.onResume()
        listenAvailabilityReceiver()
    }

    private fun listenAvailabilityReceiver() {
        database.collection(Constants.KEY_COLLECTION_USER).document(otherUser.id)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null) {
                    if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                        val availability =
                            Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABILITY)!!)
                                .toInt()
                        isReceiverAvailable = availability == 1
                    }
                    if (value.getString(Constants.KEY_FCM_TOKEN) != null) {
                        otherUser.token = value.getString(Constants.KEY_FCM_TOKEN)!!
                    }
                }
                if (isReceiverAvailable) {
                    binding.imgStatus.visibility = View.VISIBLE
                    binding.tvOnline.visibility = View.VISIBLE
                } else {
                    binding.imgStatus.visibility = View.INVISIBLE
                    binding.tvOnline.visibility = View.GONE
                }

            }

    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        listenRegistration.remove()
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}