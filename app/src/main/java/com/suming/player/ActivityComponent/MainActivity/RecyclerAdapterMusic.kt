package com.suming.player.ActivityComponent.MainActivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.suming.player.R
import com.suming.player.DataPack.MediaModel.MediaItemForMusic
import com.suming.player.DataPack.MediaModel.MediaItemForVideo
import com.suming.player.FuncionalPack.ArtworkCapturer
import com.suming.player.FuncionalPack.ArtworkFrameManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

//@Suppress("unused")
class RecyclerAdapterMusic(
    private val context: Context,
    private val onItemClick: (Uri) -> Unit,
    private val onOptionsClick: (MediaItemForMusic, View) -> Unit,
): PagingDataAdapter<MediaItemForMusic, RecyclerAdapterMusic.ViewHolder>(DiffUtil) {
    companion object {
        //条目比较器
        val DiffUtil = object : DiffUtil.ItemCallback<MediaItemForMusic>() {
            override fun areItemsTheSame(oldItem: MediaItemForMusic, newItem: MediaItemForMusic): Boolean  {
                return oldItem.uriNumOnly == newItem.uriNumOnly
            }
            override fun areContentsTheSame(oldItem: MediaItemForMusic, newItem: MediaItemForMusic): Boolean {
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




    init {
        consoleLog("init: 哎呀，骇亖我力")
    }

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
        consoleLog("bindBasicMusicCard: $position")
        val item = getItem(position) ?: return
        holder.itemName.text = item.filename.substringBeforeLast(".")
        holder.itemArtist.text = if (item.artist == "<unknown>" || item.artist == "") { "未知艺术家" } else { item.artist }
        //加载专辑封面任务
        holder.itemFrameJob?.cancel()
        holder.itemFrameJob = coroutine_loadArtwork.launch { loadArtworkFrame(item, holder) }
        //点击事件设定
        holder.itemName.setOnClickListener{
            holder.itemName.isSelected = true
        }
        holder.itemHandle.setOnClickListener {
            onItemClick(item.uriString.toUri())
        }
        holder.ButtonOptions.setOnClickListener {
            onOptionsClick(item, it)
        }
    }


    //Long Thread Functions
    private fun loadArtworkFrame(item: MediaItemForMusic, holder: ViewHolder)   {
        //记录holder的tag
        val imageTag = item.uriNumOnly.hashCode().toString()
        holder.itemFrame.tag = imageTag

        //取出目标缩略图文件
        coroutine_loadArtwork_in.launch {

            val Bitmap = ArtworkFrameManager.get_Artwork_Frame_Bitmap(context, ArtworkFrameManager.artwork_type_audio, item.uriNumOnly)
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
            holder.itemFrame.startAnimation(FadeInAnimation)
            holder.isAnimShowed = true
        }
    }

    //生成缩略图
    private fun captureAlbumFrame(item: MediaItemForMusic, holder: ViewHolder){
        coroutine_captureAlbum.launch {
            //获取专辑封面(让ArtworkCapturer承担截图任务)
            var Bitmap = ArtworkCapturer.captureAlbumInMusic(
                context = context,
                uri = item.uriString.toUri(),
                needCompress = true,
            )

            //检查是否取图成功
            if (Bitmap == null){
                consoleLog("获取专辑封面失败: 开始获取默认图 uriNumOnly=${item.uriNumOnly}")
                Bitmap = ArtworkCapturer.getDefaultAlbumFrame(context)
            }else{
                consoleLog("获取专辑封面成功: uriNumOnly=${item.uriNumOnly}")
            }

            if (Bitmap == null){
                consoleLog("默认专辑封面获取失败")
                return@launch
            }

            //推送到ImageView
            withContext(Dispatchers.Main) {
                submitToImageView(holder,Bitmap)
            }

            //保存图片(让ArtworkFrameManager承担保存任务)
            ArtworkFrameManager.save_Artwork_Frame_Bitmap(
                context,
                ArtworkFrameManager.artwork_type_audio,
                item.uriNumOnly,
                Bitmap
            )

        }
    }


    //加载动画
    private var FadeInAnimation: AlphaAnimation = AlphaAnimation(0.0f, 1.0f).apply { duration = 250 }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "RecyclerAdapterMusic: $msg")
        }
    }

}