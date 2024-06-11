package com.fhernandezri.protegest.adapters

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.models.User
import com.google.firebase.storage.FirebaseStorage

class UserAdapter(private val users: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        val name: TextView = itemView.findViewById(R.id.name)
        val rank: TextView = itemView.findViewById(R.id.rank)
        val role: TextView = itemView.findViewById(R.id.role)
        val xp: TextView = itemView.findViewById(R.id.experience)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.name.text = user.name
        holder.rank.text = "Rango: ${user.rank}"
        holder.role.text = "Rol: ${user.role}"
        holder.xp.text = "${user.experience} XP"

        // Load profile pic
        val storageReference = FirebaseStorage.getInstance().getReference("/profilePics/${user.id}")
        storageReference.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(holder.profileImageView.context)
                .load(uri)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable?>?, isFirstResource: Boolean): Boolean {
                        Log.e("GLIDE", "Load failed", e)
                        // Load the default profile image when the loading of profile image fails
                        Glide.with(holder.profileImageView.context)
                            .load(R.drawable.default_profile)
                            .into(holder.profileImageView)
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: com.bumptech.glide.request.target.Target<Drawable?>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        Log.i("GLIDE", "Resource ready")
                        return false
                    }
                })
                .into(holder.profileImageView)
        }.addOnFailureListener { exception ->
            Log.e("FIREBASE", "Download URL failed", exception)
            // Load the default profile image when the download of profile image URL fails
            Glide.with(holder.profileImageView.context)
                .load(R.drawable.default_profile)
                .into(holder.profileImageView)
        }
    }

    override fun getItemCount() = users.size
}