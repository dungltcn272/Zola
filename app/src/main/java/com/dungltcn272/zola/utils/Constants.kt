package com.dungltcn272.zola.utils

object Constants {
    const val KEY_COLLECTION_USER ="users"
    const val KEY_NAME ="name"
    const val KEY_EMAIL = "email"
    const val KEY_PASSWORD = "password"
    const val KEY_PREFERENCE_NAME = "zolaPreference"
    const val KEY_IS_SIGNED_IN = "isSignIn"
    const val KEY_USER_ID = "userId"
    const val KEY_IMAGE = "image"
    const val KEY_FCM_TOKEN = "fcmToken"
    const val KEY_USER = "user"
    const val KEY_COLLECTION_CHAT = "chat"
    const val KEY_SENDER_ID = "senderId"
    const val KEY_RECEIVER_ID = "receiverId"
    const val KEY_RECEIVER_SEEN = "receiverSeen"
    const val KEY_MESSAGE = "message"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_COLLECTION_CONVERSATIONS = "conversations"
    const val KEY_SENDER_NAME = "senderName"
    const val KEY_RECEIVER_NAME = "receiverName"
    const val KEY_SENDER_IMAGE = "senderImage"
    const val KEY_RECEIVER_IMAGE= "receiverImage"
    const val KEY_LAST_MESSAGE = "lastMessage"
    const val KEY_USERS_ID_ARRAY = "usersIdArray"
    const val KEY_AVAILABILITY = "availability"
    private const val REMOTE_MSG_AUTHORIZATION ="Authorization"
    private const val REMOTE_MSG_CONTENT_TYPE ="Content-Type"
    const val REMOTE_MSG_DATA ="data"
    const val REMOTE_MSG_REGISTRATION_IDS ="registration_ids"
    const val KEY_IS_CURRENT_USER ="isCurrentUser"


    private var remoteMsgHeader : HashMap<String, String>? =null
    fun getRemoteMsgHeaders() : HashMap<String, String> {
        if (remoteMsgHeader == null){
            remoteMsgHeader = hashMapOf()
            remoteMsgHeader!![REMOTE_MSG_AUTHORIZATION] = "key=AAAAVWEtTzA:APA91bEsrjgK33C1r06275fTDrAtS7osA3n3zERuplmt_acpx7-6LYqBDatc4OO0ZaJ6-B35HsKJtAI3nofuzbqeduD_5v543RnU8TnCKHNOqh54id8SpXuspGYMPtN_zvKMzN4bIyDP"
            remoteMsgHeader!![REMOTE_MSG_CONTENT_TYPE]="application/json"
        }
        return remoteMsgHeader!!
    }
}