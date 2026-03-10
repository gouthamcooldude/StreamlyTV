package com.streamlytv.ui.channels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamlytv.R
import com.streamlytv.data.model.Channel
import com.streamlytv.data.model.EpgProgram

class ChannelAdapter(
    private val onChannelClick: (Channel) -> Unit,
    private val onFavoriteClick: (Channel) -> Unit
) : ListAdapter<ChannelItem, ChannelAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val logo: ImageView = view.findViewById(R.id.channelLogo)
        private val name: TextView = view.findViewById(R.id.channelName)
        private val nowPlaying: TextView = view.findViewById(R.id.nowPlaying)
        private val badge4K: TextView = view.findViewById(R.id.badge4K)
        private val badge51: TextView = view.findViewById(R.id.badge51)
        private val favoriteBtn: ImageButton = view.findViewById(R.id.btnFavorite)
        private val group: TextView = view.findViewById(R.id.channelGroup)

        fun bind(item: ChannelItem) {
            val channel = item.channel

            name.text = channel.name
            group.text = channel.group

            // EPG now playing
            if (item.currentProgram != null) {
                nowPlaying.text = item.currentProgram.title
                nowPlaying.visibility = View.VISIBLE
            } else {
                nowPlaying.visibility = View.GONE
            }

            // 4K badge
            badge4K.visibility = if (channel.is4K) View.VISIBLE else View.GONE

            // 5.1 badge
            badge51.visibility = if (channel.is51) View.VISIBLE else View.GONE

            // Logo
            if (channel.logo.isNotEmpty()) {
                Glide.with(logo.context)
                    .load(channel.logo)
                    .placeholder(R.drawable.ic_channel_placeholder)
                    .error(R.drawable.ic_channel_placeholder)
                    .into(logo)
            } else {
                logo.setImageResource(R.drawable.ic_channel_placeholder)
            }

            // Favorite button
            favoriteBtn.setImageResource(
                if (channel.isFavorite) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )

            favoriteBtn.setOnClickListener { onFavoriteClick(channel) }
            itemView.setOnClickListener { onChannelClick(channel) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChannelItem>() {
        override fun areItemsTheSame(oldItem: ChannelItem, newItem: ChannelItem) =
            oldItem.channel.id == newItem.channel.id

        override fun areContentsTheSame(oldItem: ChannelItem, newItem: ChannelItem) =
            oldItem == newItem
    }
}

data class ChannelItem(
    val channel: Channel,
    val currentProgram: EpgProgram? = null
)
