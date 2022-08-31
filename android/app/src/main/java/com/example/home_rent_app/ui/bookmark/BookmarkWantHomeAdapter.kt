package com.example.home_rent_app.ui.bookmark

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.home_rent_app.data.dto.WantedArticleBookmark
import com.example.home_rent_app.data.model.BookmarkRequest
import com.example.home_rent_app.databinding.ItemWanthomeBookmarkBinding
import com.example.home_rent_app.util.ItemIdSession
import com.example.home_rent_app.util.UserSession
import javax.inject.Inject

class BookmarkWantHomeAdapter @Inject constructor(
    private val viewModel: BookmarkViewModel,
    private val userSession: UserSession,
    private val itemIdSession: ItemIdSession
) : ListAdapter<WantedArticleBookmark, BookmarkWantHomeAdapter.BookmarkWantHomeViewHolder>(BookmarkAdapterDiffCallBack) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkWantHomeViewHolder {
        val binding =
            ItemWanthomeBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookmarkWantHomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkWantHomeViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class BookmarkWantHomeViewHolder(private val binding: ItemWanthomeBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(wantedArticleBookmark: WantedArticleBookmark) {
            binding.bookmarkWantedBookmark = wantedArticleBookmark
            binding.btnLike.isChecked = true
            binding.btnLike.setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked) deleteBookmark(wantedArticleBookmark)
            }
        }

        private fun deleteBookmark(wantedArticleBookmark: WantedArticleBookmark) {
            viewModel.deleteBookmark(BookmarkRequest(userSession.userId ?: 0, wantedArticleBookmark.id))
        }
    }
}

object BookmarkAdapterDiffCallBack : DiffUtil.ItemCallback<WantedArticleBookmark>() {
    override fun areItemsTheSame(oldItem: WantedArticleBookmark, newItem: WantedArticleBookmark): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: WantedArticleBookmark, newItem: WantedArticleBookmark): Boolean {
        return oldItem == newItem
    }
}
