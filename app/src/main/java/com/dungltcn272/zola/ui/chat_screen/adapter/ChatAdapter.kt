package com.dungltcn272.zola.ui.chat_screen.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dungltcn272.zola.databinding.ItemContainerReceivedMessageBinding
import com.dungltcn272.zola.databinding.ItemContainerSentMessageBinding
import com.dungltcn272.zola.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ChatAdapter(val otherUserProfileImage: Bitmap, private val currentUserId: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (differ.currentList[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_SENT) {
            return SentMessageViewHolder(
                ItemContainerSentMessageBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        } else {
            return ReceiverMessageViewHolder(
                ItemContainerReceivedMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            (holder as SentMessageViewHolder).setData(position)
        } else {
            (holder as ReceiverMessageViewHolder).setData(position)
        }
    }

    private val diffUtil = object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.dateObject == newItem.dateObject
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.dateObject == newItem.dateObject
        }
    }
    val differ = AsyncListDiffer(this, diffUtil)

    inner class SentMessageViewHolder(private val binding: ItemContainerSentMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(position: Int) {
            val chatMessage = differ.currentList[position]
            binding.tvMessage.text = chatMessage.message
            if (formatDateToString(chatMessage.dateObject!!).isEmpty()){
                binding.tvDatetime.visibility = View.GONE
            }else{
                binding.tvDatetime.text = formatDateToString(chatMessage.dateObject!!)
                if (position < differ.currentList.size - 1) {
                    val seconds =
                        TimeUnit.MILLISECONDS.toSeconds(differ.currentList[position + 1].dateObject!!.time - chatMessage.dateObject!!.time)
                    if(seconds < 60){
                        binding.tvDatetime.visibility = View.GONE
                    }
                }else{
                    binding.tvDatetime.visibility = View.VISIBLE
                }
            }
        }
    }

    inner class ReceiverMessageViewHolder(private val binding: ItemContainerReceivedMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(position: Int) {
            val chatMessage = differ.currentList[position]
            binding.tvMessage.text = chatMessage.message
            binding.imgProfileReceiver.setImageBitmap(otherUserProfileImage)
            if (formatDateToString(chatMessage.dateObject!!).isEmpty()){
                binding.tvDatetime.visibility = View.GONE
            }else{
                binding.tvDatetime.text = formatDateToString(chatMessage.dateObject!!)
                if (position < differ.currentList.size - 1) {
                    val seconds =
                        TimeUnit.MILLISECONDS.toSeconds(differ.currentList[position + 1].dateObject!!.time - chatMessage.dateObject!!.time)
                    if(seconds < 60){
                        binding.tvDatetime.visibility = View.GONE
                    }
                }else{
                    binding.tvDatetime.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun formatDateToString(date: Date): String {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - date.time

        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff)
        val hours = TimeUnit.MILLISECONDS.toHours(timeDiff)
        val days = TimeUnit.MILLISECONDS.toDays(timeDiff)

        return when {
            seconds < 60 -> {
                // less than 1 minutes
                String.format("")
            }

            minutes < 60 -> {
                // less than 1 hour
                String.format("$minutes minutes")
            }

            hours < 24 -> {
                // less than 1 day
                String.format("$hours hours")
            }

            days < 30 -> {
                // less than 1 month
                String.format("$days days")
            }

            else -> {
                // more than 1 month
                SimpleDateFormat("dd 'Th' MM HH:mm", Locale.getDefault()).format(date)
            }
        }
    }


}