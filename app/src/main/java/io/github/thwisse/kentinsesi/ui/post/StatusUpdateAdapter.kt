package io.github.thwisse.kentinsesi.ui.post

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.thwisse.kentinsesi.data.model.StatusUpdate
import io.github.thwisse.kentinsesi.databinding.ItemStatusUpdateBinding
import io.github.thwisse.kentinsesi.util.loadAvatar
import io.github.thwisse.kentinsesi.util.toLocalizedTitle
import java.text.SimpleDateFormat
import java.util.Locale

class StatusUpdateAdapter : ListAdapter<StatusUpdate, StatusUpdateAdapter.ViewHolder>(StatusUpdateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatusUpdateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class ViewHolder(private val binding: ItemStatusUpdateBinding) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("EEEE, dd MMM, HH:mm", Locale.getDefault())

        fun bind(update: StatusUpdate, position: Int) {
            val context = binding.root.context
            // Avatar loading
            android.util.Log.d("StatusUpdateAdapter", "bind() - update.id=${update.id}, authorAvatarSeed='${update.authorAvatarSeed}'")
            binding.ivUpdateAuthorAvatar.loadAvatar(update.authorAvatarSeed)

            // Ok işareti - sadece ilk item'dan sonrakiler için göster
            binding.tvTimelineArrow.visibility = if (position == 0) android.view.View.GONE else android.view.View.VISIBLE

            // Durum badge'i
            val (badgeText, badgeColor) = when (update.status) {
                "new" -> context.getString(io.github.thwisse.kentinsesi.R.string.status_new_badge) to Color.parseColor("#2196F3")
                "in_progress" -> context.getString(io.github.thwisse.kentinsesi.R.string.status_in_progress_badge) to Color.parseColor("#FF9800")
                "resolved" -> context.getString(io.github.thwisse.kentinsesi.R.string.status_resolved_badge) to Color.parseColor("#4CAF50")
                else -> context.getString(io.github.thwisse.kentinsesi.R.string.status_unknown_badge) to Color.parseColor("#9E9E9E")
            }
            
            binding.tvStatusBadge.text = badgeText
            binding.tvStatusBadge.setBackgroundColor(badgeColor)
            
            // Tarih
            binding.tvDate.text = update.createdAt?.let { dateFormat.format(it) } ?: "-"
            
            // Yazar
            val usernameText = if (update.authorUsername.isNotBlank()) 
                "@${update.authorUsername}" 
            else 
                ""
            
            val authorName = if (usernameText.isNotBlank()) {
                "${update.authorFullName} ($usernameText)"
            } else {
                update.authorFullName
            }
            
            // Lokasyon ve title bilgisi (yorumlar gibi)
            val location = listOf(update.authorCity, update.authorDistrict)
                .filter { it.isNotBlank() }
                .joinToString("/")
            val title = update.authorTitle.toLocalizedTitle(context)
            
            val metaInfo = buildString {
                if (location.isNotBlank()) append(location)
                if (title.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(title)
                }
            }
            
            binding.tvAuthor.text = if (metaInfo.isNotBlank()) {
                "$authorName\n$metaInfo"
            } else {
                authorName
            }
            
            // Not
            binding.tvNote.text = update.note
        }
    }

    class StatusUpdateDiffCallback : DiffUtil.ItemCallback<StatusUpdate>() {
        override fun areItemsTheSame(oldItem: StatusUpdate, newItem: StatusUpdate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StatusUpdate, newItem: StatusUpdate): Boolean {
            return oldItem == newItem
        }
    }
}
