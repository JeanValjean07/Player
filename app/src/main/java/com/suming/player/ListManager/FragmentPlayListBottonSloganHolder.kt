package com.suming.player.ListManager

import android.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView

/*
class FragmentPlayListBottonSloganHolder(
    parent: ViewGroup,
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.activity_player_fragment_play_list_footer, parent, false)
) {
    private val binding = ItemLoadingFooterBinding.bind(itemView)

    private val errorMsg: TextView = binding.errorMsg




    fun bind(loadState: LoadState) {
        when (loadState) {
            is LoadState.Loading -> {

                errorMsg.visibility = View.GONE
            }
            is LoadState.Error -> {

                errorMsg.visibility = View.VISIBLE
                errorMsg.text = loadState.error.localizedMessage
            }
            else -> {

                errorMsg.visibility = View.GONE
            }
        }
    }
}

 */