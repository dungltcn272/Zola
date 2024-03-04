package com.dungltcn272.zola.firebase

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dungltcn272.zola.R
import com.dungltcn272.zola.utils.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Token $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Token mm $message")

        val notificationMessage = message.data[Constants.KEY_MESSAGE] ?: ""
        val notificationName = message.data[Constants.KEY_NAME] ?: ""

        showNotification(notificationName, notificationMessage)
    }

    private fun showNotification(title: String, message: String) {
        val notificationId = Random.nextInt()
        val channelId = "chat_message"

        val builder = NotificationCompat.Builder(this, channelId)
        builder.setSmallIcon(R.drawable.ic_notifications)
        builder.setContentTitle(title)
        builder.setContentText(message)
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Chat Message"
            val channelDescription = "This notification channel is used for chat message notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            channel.description = channelDescription
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationManagerCompat = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }


        notificationManagerCompat.notify(notificationId, builder.build())

        Log.d("FCM", "Notification sent with ID: $notificationId")
    }
}
