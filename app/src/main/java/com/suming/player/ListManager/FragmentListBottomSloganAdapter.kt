package com.suming.player.ListManager

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter

class FragmentListBottomSloganAdapter(private val retry: () -> Unit):LoadStateAdapter<FragmentListBottonSloganViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState):FragmentListBottonSloganViewHolder{

        return FragmentListBottonSloganViewHolder(parent, retry)
    }

    override fun onBindViewHolder(holder: FragmentListBottonSloganViewHolder, loadState: LoadState){
        holder.bind(loadState)
    }

    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {

        return loadState is LoadState.Loading || loadState is LoadState.Error ||
             (loadState is LoadState.NotLoading && loadState.endOfPaginationReached)
    }



}

