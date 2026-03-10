package com.streamlytv.ui.vod

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamlytv.R

class VodAdapter(
    private val onItemClick: (VodDisplayItem) -> Unit,
    private val onLikeClick: (VodDisplayItem) -> Unit,
    private val onDislikeClick: (VodDisplayItem) -> Unit
) : ListAdapter<VodDisplayItem, VodAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vod, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val poster: ImageView = view.findViewById(R.id.vodPoster)
        private val title: TextView = view.findViewById(R.id.vodTitle)
        private val badge4K: TextView = view.findViewById(R.id.badge4K)
        private val badgeHdr: TextView = view.findViewById(R.id.badgeHdr)
        private val badge51: TextView = view.findViewById(R.id.badge51)
        private val badgeCodec: TextView = view.findViewById(R.id.badgeCodec)
        private val badgeAudio: TextView = view.findViewById(R.id.badgeAudio)
        private val watchedDot: View = view.findViewById(R.id.watchedDot)
        private val resumeBar: ProgressBar = view.findViewById(R.id.resumeBar)
        private val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        private val btnDislike: ImageButton = view.findViewById(R.id.btnDislike)

        fun bind(item: VodDisplayItem) {
            title.text = item.vodItem.name

            // Poster
            Glide.with(poster.context)
                .load(item.vodItem.streamIcon)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .centerCrop()
                .into(poster)

            // 4K badge
            badge4K.visibility = if (item.is4K) View.VISIBLE else View.GONE

            // HDR badge
            if (item.isHdr && item.hdrLabel.isNotEmpty()) {
                badgeHdr.text = item.hdrLabel
                badgeHdr.visibility = View.VISIBLE
            } else if (item.isHdr) {
                badgeHdr.text = "HDR"
                badgeHdr.visibility = View.VISIBLE
            } else {
                badgeHdr.visibility = View.GONE
            }

            // 5.1 badge
            badge51.visibility = if (item.is51) View.VISIBLE else View.GONE

            // Video codec badge
            if (item.videoCodecLabel.isNotEmpty()) {
                badgeCodec.text = item.videoCodecLabel
                badgeCodec.visibility = View.VISIBLE
            } else {
                badgeCodec.visibility = View.GONE
            }

            // Audio label badge
            if (item.audioLabel.isNotEmpty()) {
                badgeAudio.text = item.audioLabel
                badgeAudio.visibility = View.VISIBLE
            } else {
                badgeAudio.visibility = View.GONE
            }

            // Watched dot
            watchedDot.visibility = if (item.isWatched) View.VISIBLE else View.GONE

            // Resume progress bar
            if (item.resumePercent in 1..99) {
                resumeBar.progress = item.resumePercent
                resumeBar.visibility = View.VISIBLE
            } else {
                resumeBar.visibility = View.GONE
            }

            // Like / Dislike buttons
            btnLike.setImageResource(
                if (item.isLiked) R.drawable.ic_thumb_up_filled
                else R.drawable.ic_thumb_up_outline
            )
            btnDislike.setImageResource(
                if (item.isDisliked) R.drawable.ic_thumb_down_filled
                else R.drawable.ic_thumb_down_outline
            )

            btnLike.setOnClickListener { onLikeClick(item) }
            btnDislike.setOnClickListener { onDislikeClick(item) }
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<VodDisplayItem>() {
        override fun areItemsTheSame(a: VodDisplayItem, b: VodDisplayItem) =
            a.vodItem.streamId == b.vodItem.streamId
        override fun areContentsTheSame(a: VodDisplayItem, b: VodDisplayItem) =
            a == b
    }
}
