package com.suming.player.FuncPack_ListManager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import com.suming.player.databinding.AddonRecyclerviewFooterBinding

class ListBottonSloganViewHolder(parent: ViewGroup, retry: () -> Unit):RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.addon_recyclerview_footer, parent, false)
){

    private val binding = AddonRecyclerviewFooterBinding.bind(itemView)

    private val FooterText: TextView = binding.stateText



    fun bind(loadState: LoadState) {
        when (loadState) {
            is LoadState.Loading -> {
                FooterText.visibility = View.VISIBLE
                FooterText.text = "加载中"
            }
            is LoadState.Error -> {
                FooterText.visibility = View.VISIBLE
                FooterText.text = "加载出错"
            }
            is LoadState.NotLoading -> {
                FooterText.visibility = View.VISIBLE
                FooterText.text = "没有更多了"
            }

        }
    }



//
}

