package com.dungltcn272.zola.model

import java.util.Date

class ChatMessage {
    var senderId : String =""
    var receiverId : String = ""
    var message : String =""
    var datetime: String=""
    var dateObject : Date? = null
    var conversationId : String = ""
    var conversationName : String = ""
    var conversationImage : String = ""
}