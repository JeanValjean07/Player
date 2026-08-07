package com.suming.player.ActivityComponent.MainActivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import com.suming.player.DataPack.DataClassForStorage.MediaItemFullForAudio
import com.suming.player.FuncionalPack.Animations
import com.suming.player.FuncionalPack.ArtworkCapturer
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//@Suppress("unused")
class RecyclerAdapterMusic(
    private val context: Context,
    private val onItemClick: (Uri) -> Unit,
    private val onOptionsClick: (MediaItemFullForAudio, View) -> Unit,
): PagingDataAdapter<MediaItemFullForAudio, RecyclerAdapterMusic.ViewHolder>(DiffUtil) {
    companion object {
        //比较器
        val DiffUtil = object : DiffUtil.ItemCallback<MediaItemFullForAudio>() {
            override fun areItemsTheSame(oldItem: MediaItemFullForAudio, newItem: MediaItemFullForAudio): Boolean  {
                return oldItem.media_SPECIFIC_MediaType == newItem.media_SPECIFIC_MediaType
            }
            override fun areContentsTheSame(oldItem: MediaItemFullForAudio, newItem: MediaItemFullForAudio): Boolean {
                return oldItem == newItem
            }
        }
    }


    //viewHolder 1 - 普通音乐卡片
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //基本视图
        val itemHandle: View = itemView.findViewById(R.id.TouchPad)
        val itemFrame: ImageView = itemView.findViewById(R.id.auThumb)
        val itemName: TextView = itemView.findViewById(R.id.auName)
        val itemArtist: TextView = itemView.findViewById(R.id.auArtist)
        val ButtonOptions: CardView = itemView.findViewById(R.id.ButtonOptions)
        //可控制任务
        var itemFrameJob: Job? = null
        //标识
        var isAnimShowed: Boolean = false   //是否显示过动画
    }

    //协程
    private val coroutine_captureAlbum = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_loadArtwork = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutine_loadArtwork_in = CoroutineScope(Dispatchers.IO + SupervisorJob())




    //init(未使用)
    /*
    init {
    }
     */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_list_item_music, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        bindBasicMusicCard(holder, position)
    }




    //绑定基本音乐卡片
    private fun bindBasicMusicCard(holder: ViewHolder, position: Int){
        //consoleLog("bindBasicMusicCard: $position")
        val item = getItem(position) ?: return
        holder.itemName.text = item.file_name.substringBeforeLast(".")
        holder.itemArtist.text = if (item.media_artist == "<unknown>" || item.media_artist == "") { "未知艺术家" } else { item.media_artist }
        //加载专辑封面任务
        holder.itemFrameJob?.cancel()
        holder.itemFrameJob = coroutine_loadArtwork.launch { loadArtworkFrame(item, holder) }
        //点击事件设定
        holder.itemName.setOnClickListener{
            holder.itemName.isSelected = true
        }
        holder.itemHandle.setOnClickListener {
            onItemClick(item.content_uriString.toUri())
        }
        holder.ButtonOptions.setOnClickListener {
            onOptionsClick(item, it)
        }
    }


    //Long Thread Functions
    private fun loadArtworkFrame(item: MediaItemFullForAudio, holder: ViewHolder)   {
        //记录holder的tag
        val imageTag = item.media_api_NUM_ID.hashCode().toString()
        holder.itemFrame.tag = imageTag

        //取出目标缩略图文件
        coroutine_loadArtwork_in.launch {
            val Bitmap = ArtworkFrameManager.GET_ArtworkFrame_Bitmap(context, MediaType.Audio, item.media_api_NUM_ID)
            if (Bitmap != null){
                //推到ImageView
                withContext(Dispatchers.Main) {
                    if (holder.itemFrame.tag == imageTag) {
                        submitToImageView(holder,Bitmap)
                    }else{ Bitmap.recycle() }
                }
            }else{
                //获取专辑封面
                captureAlbumFrame(item, holder)
            }

        }
    }

    //推送到ImageView
    private fun submitToImageView(holder: ViewHolder,Bitmap : Bitmap){
        holder.itemFrame.setImageBitmap(Bitmap)
        if (!holder.isAnimShowed){
            holder.itemFrame.startAnimation(Animations.FadeIn)
            holder.isAnimShowed = true
        }
    }
    private fun submitToImageViewNoAnim(holder: ViewHolder,Bitmap : Bitmap){
        holder.itemFrame.setImageBitmap(Bitmap)
    }

    //生成缩略图
    private fun captureAlbumFrame(item: MediaItemFullForAudio, holder: ViewHolder){
        coroutine_captureAlbum.launch {
            //获取专辑封面(让ArtworkCapturer承担截图任务)
            //consoleLog("captureAlbumFrame: ${item.file_name} ${item.content_uriString}")
            var Bitmap = ArtworkCapturer.captureAlbumInMusic(
                context = context,
                uri = item.content_uriString.toUri(),
                needCompress = true,
            )

            //检查是否取图成功
            if (Bitmap == null){
                Bitmap = ArtworkCapturer.getDefaultAlbumFrame(context)
            }

            if (Bitmap == null){
                return@launch
            }

            //推送到ImageView
            withContext(Dispatchers.Main) { submitToImageViewNoAnim(holder,Bitmap) }

            //保存图片
            ArtworkFrameManager.SAVE_ArtworkFrame_Bitmap(context, MediaType.Audio, item.media_api_NUM_ID, Bitmap)

        }
    }



    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "RecyclerAdapterMusic: $msg")
        }
    }

}