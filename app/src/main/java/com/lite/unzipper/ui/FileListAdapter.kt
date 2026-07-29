package com.lite.unzipper.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lite.unzipper.databinding.ItemFileBinding

class FileListAdapter(private val onItemClick: (FileItem) -> Unit) :
    ListAdapter<FileItem, FileListAdapter.FileViewHolder>(DIFF) {

    private val pressInterpolator = AccelerateInterpolator(1.4f)
    private val releaseInterpolator = OvershootInterpolator(2.2f)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FileViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentAnimator: AnimatorSet? = null

        init {
            binding.cardRoot.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> animatePress()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animateRelease()
                }
                false
            }
            binding.cardRoot.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(getItem(pos))
            }
            binding.fileCheck.setOnCheckedChangeListener { _, _ ->
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(getItem(pos))
            }
        }

        private fun animatePress() {
            currentAnimator?.cancel()
            val set = AnimatorSet()
            set.playTogether(
                ObjectAnimator.ofFloat(binding.cardRoot, "scaleX", 0.95f).apply {
                    duration = 80; interpolator = pressInterpolator
                },
                ObjectAnimator.ofFloat(binding.cardRoot, "scaleY", 0.95f).apply {
                    duration = 80; interpolator = pressInterpolator
                },
                ObjectAnimator.ofFloat(binding.cardRoot, "translationZ", 2f).apply {
                    duration = 80; interpolator = pressInterpolator
                }
            )
            currentAnimator = set
            set.start()
        }

        private fun animateRelease() {
            currentAnimator?.cancel()
            val set = AnimatorSet()
            set.playTogether(
                ObjectAnimator.ofFloat(binding.cardRoot, "scaleX", 1f).apply {
                    duration = 320; interpolator = releaseInterpolator
                },
                ObjectAnimator.ofFloat(binding.cardRoot, "scaleY", 1f).apply {
                    duration = 320; interpolator = releaseInterpolator
                },
                ObjectAnimator.ofFloat(binding.cardRoot, "translationZ", 0f).apply {
                    duration = 320; interpolator = releaseInterpolator
                }
            )
            currentAnimator = set
            set.start()
        }

        fun bind(item: FileItem) {
            binding.fileName.text = item.name
            binding.fileMeta.text = item.formatMeta(itemView.context)
            binding.fileCheck.setOnCheckedChangeListener(null)
            binding.fileCheck.isChecked = item.isSelected
            binding.fileCheck.setOnCheckedChangeListener { _, _ -> onItemClick(item) }
            binding.lockIcon.visibility =
                if (item.isEncrypted) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FileItem>() {
            override fun areItemsTheSame(old: FileItem, new: FileItem) = old.uri == new.uri
            override fun areContentsTheSame(old: FileItem, new: FileItem) = old == new
        }
    }
}
