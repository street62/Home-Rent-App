package com.example.home_rent_app.ui.findroom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.home_rent_app.R
import com.example.home_rent_app.data.model.Article
import com.example.home_rent_app.databinding.ItemHomeListBinding
import com.example.home_rent_app.util.logger

class HomeListAdapter(
    private val goToDetail: (Int) -> Unit,
    private val addBookmark: (Int) -> Unit,
    private val deleteBookmark: (Int) -> Unit
    ): ListAdapter<Article, HomeListAdapter.HomeListViewHolder>(TempDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeListViewHolder {
        return HomeListViewHolder(ItemHomeListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: HomeListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HomeListViewHolder(private val binding: ItemHomeListBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(item: Article) {
            item.houseImages.forEach {
                logger("HomeListViewHolder : $it")
            }
            binding.item = item
            val viewPagerAdapter = HomeThumbnailAdapter()
            binding.vpThumbNailList.adapter = viewPagerAdapter
//            item.houseImages
            viewPagerAdapter.submitList(listOf("${R.drawable.default_profile_image}", "${R.drawable.ic_arrow_back}"))
            setOnHomeClick(item)
        }

        private fun setOnHomeClick(item: Article) {
            itemView.setOnClickListener {
                goToDetail(item.id)
            }

            binding.cbRecommend.setOnClickListener {
                if(binding.cbRecommend.isChecked) {
                    addBookmark(item.id)
                } else {
                    deleteBookmark(item.id)
                }
                item.bookmarked = !item.bookmarked
            }
        }

    }

    private object TempDiffUtil: DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Article, newItem: Article) =
            oldItem == newItem

    }

}