package com.dungltcn272.zola.ui.chat_screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
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
import com.dungltcn272.zola.utils.Constants
import com.dungltcn272.zola.utils.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var receiverUser: User
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore
    private lateinit var chatMessages: MutableList<ChatMessage>
    private lateinit var chatAdapter: ChatAdapter
    private var conversionId: String? = null
    private var isReceiverAvailable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadReceiverDetails()
        init()

        setListener()
        listenerMessages()
    }

    private fun loadReceiverDetails() {
        @Suppress("DEPRECATION")
        receiverUser = intent.getSerializableExtra(Constants.KEY_USER) as User
        binding.tvName.text = receiverUser.name
        val encodedImage = receiverUser.image
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imgProfileReceiver.setImageBitmap(bitmap)
    }

    private fun init() {
        preferenceManager = PreferenceManager(applicationContext)
        database = FirebaseFirestore.getInstance()
        chatMessages = mutableListOf()
        chatAdapter = ChatAdapter(
            getBitmapFromEncodedString(receiverUser.image),
            preferenceManager.getString(Constants.KEY_USER_ID)!!
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
    }

    private fun listenerMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(
                Constants.KEY_SENDER_ID,
                preferenceManager.getString(Constants.KEY_USER_ID)
            )
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
            .addSnapshotListener(eventListener)

        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
            .whereEqualTo(
                Constants.KEY_RECEIVER_ID,
                preferenceManager.getString(Constants.KEY_USER_ID)
            )
            .addSnapshotListener(eventListener)
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
                binding.rcvChat.scrollToPosition(chatMessages.size - 1)
            }
        }
        binding.progressBar.visibility = View.GONE
        if (conversionId == null) {
            checkForConversion()
        }

    }

    private fun sendMessage() {
        val message = hashMapOf<String, Any>()
        message[Constants.KEY_SENDER_ID] = preferenceManager.getString(Constants.KEY_USER_ID)!!
        message[Constants.KEY_RECEIVER_ID] = receiverUser.id
        message[Constants.KEY_MESSAGE] = binding.edtMessage.text.toString().trim()
        message[Constants.KEY_TIMESTAMP] = Date()
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message)
        if (conversionId != null) {
            updateConversion(binding.edtMessage.text.toString().trim())
        } else {
            val conversion = hashMapOf<String, Any>()
            conversion[Constants.KEY_SENDER_ID] =
                preferenceManager.getString(Constants.KEY_USER_ID)!!
            conversion[Constants.KEY_SENDER_NAME] =
                preferenceManager.getString(Constants.KEY_NAME)!!
            conversion[Constants.KEY_SENDER_IMAGE] =
                preferenceManager.getString(Constants.KEY_IMAGE)!!
            conversion[Constants.KEY_RECEIVER_ID] = receiverUser.id
            conversion[Constants.KEY_RECEIVER_IMAGE] = receiverUser.image
            conversion[Constants.KEY_RECEIVER_NAME] = receiverUser.name
            conversion[Constants.KEY_LAST_MESSAGE] = binding.edtMessage.text.toString().trim()
            conversion[Constants.KEY_TIMESTAMP] = Date()
            addConversion(conversion)
        }
        if (!isReceiverAvailable) {
            try {
                val tokens = JSONArray()
                tokens.put(receiverUser.token)
                val data = JSONObject()
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME))
                data.put(
                    Constants.KEY_FCM_TOKEN,
                    preferenceManager.getString(Constants.KEY_FCM_TOKEN)
                )
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
                                    showToast(error.getString("error"))
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

    private fun listenAvailabilityReceiver() {
        database.collection(Constants.KEY_COLLECTION_USER)
            .document(receiverUser.id)
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
                        receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN)!!
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


    private fun getBitmapFromEncodedString(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    private fun getReadableDatetime(date: Date): String {
        return SimpleDateFormat("dd 'Th' MM hh:mm", Locale.getDefault()).format(date)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun addConversion(conversion: HashMap<String, Any>) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .add(conversion)
            .addOnSuccessListener { documentReferences -> conversionId = documentReferences.id }
    }

    private fun updateConversion(message: String) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId!!)
            .update(Constants.KEY_LAST_MESSAGE, message, Constants.KEY_TIMESTAMP, Date())
    }

    private fun checkForConversion() {
        if (chatMessages.size > 0) {
            checkForConversionRemotely(
                preferenceManager.getString(Constants.KEY_USER_ID)!!,
                receiverUser.id
            )
            checkForConversionRemotely(
                receiverUser.id,
                preferenceManager.getString(Constants.KEY_USER_ID)!!
            )
        }
    }

    private fun checkForConversionRemotely(senderId: String, receiverId: String) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
            .get()
            .addOnCompleteListener(conversionOnCompleteListener)

    }

    private val conversionOnCompleteListener: OnCompleteListener<QuerySnapshot> =
        OnCompleteListener { task ->
            if (task.isSuccessful && task.result != null && task.result.documents.size > 0) {
                val documentSnapshot = task.result.documents[0]
                conversionId = documentSnapshot.id
            }
        }

    override fun onResume() {
        super.onResume()
        listenAvailabilityReceiver()
    }
}