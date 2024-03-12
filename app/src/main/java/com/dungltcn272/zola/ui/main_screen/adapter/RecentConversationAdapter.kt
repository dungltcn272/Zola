package com.dungltcn272.zola.ui.main_screen.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dungltcn272.zola.databinding.ItemContainerRecentConversationBinding
import com.dungltcn272.zola.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


class RecentConversationAdapter(private val currentUserId : String) :
    RecyclerView.Adapter<RecentConversationAdapter.ConversationViewHolder>() {


    var onItemClick: ((ChatMessage) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        return ConversationViewHolder(
            ItemContainerRecentConversationBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    private val diffUtil = object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.conversationId == newItem.conversationId
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.message == newItem.message &&
                    oldItem.dateObject == newItem.dateObject &&
                    oldItem.senderId == newItem.senderId && oldItem.receiverId == newItem.receiverId
        }
    }
    val differ = AsyncListDiffer(this, diffUtil)


    override fun getItemCount(): Int {
        return differ.currentList.size
    }


    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val chatMessage = differ.currentList[position]
        holder.setData(chatMessage)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(chatMessage)
        }
    }

    private fun getConversionImage(encodeImage: String): Bitmap {
        val bytes = Base64.decode(encodeImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    inner class ConversationViewHolder(private val binding: ItemContainerRecentConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(chatMessage: ChatMessage) {
            binding.imgProfile.setImageBitmap(getConversionImage(chatMessage.conversationImage))
            binding.tvName.text = chatMessage.conversationName
            if (chatMessage.senderId == currentUserId){
                binding.tvRecentMessage.text = String.format("You: ${chatMessage.message}")
            }else{
                binding.tvRecentMessage.text = chatMessage.message
                if (chatMessage.receiverSeen){
                    binding.seenStatus.visibility = View.INVISIBLE
                    binding.tvRecentMessage.setTypeface(null, Typeface.NORMAL)
                }else{
                    binding.seenStatus.visibility = View.VISIBLE
                    binding.tvRecentMessage.setTypeface(null, Typeface.BOLD)
                }
            }
            binding.tvTime.text = formatDateToString(chatMessage.dateObject!!)

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
                String.format("recent")
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