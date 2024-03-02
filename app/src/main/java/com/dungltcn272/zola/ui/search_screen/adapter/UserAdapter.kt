package com.dungltcn272.zola.ui.search_screen.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dungltcn272.zola.databinding.ItemContainerUserBinding
import com.dungltcn272.zola.model.User

class UserAdapter : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    var onItemClick : ((User) -> Unit)? = null

    private val diffUtil = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.email == newItem.email
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.email == newItem.email
        }
    }
    val differ = AsyncListDiffer(this, diffUtil)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = differ.currentList[position]
        holder.setData(user)
        holder.binding.root.setOnClickListener{
            onItemClick?.invoke(user)
        }
    }

    inner class UserViewHolder(val binding: ItemContainerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(user: User) {
            binding.imgProfile.setImageBitmap(getUserImage(user.image))
            binding.tvName.text = user.name
            binding.tvEmail.text = user.email
        }
    }

    private fun getUserImage(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}