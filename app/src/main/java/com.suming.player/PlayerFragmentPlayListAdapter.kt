package com.suming.player

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PlayerFragmentPlayListAdapter(
    private val context: Context,
    private val onDeleteClick: (Uri) -> Unit,
    private val onPlayClick: (Uri) -> Unit
):PagingDataAdapter<MediaItemForVideo, PlayerFragmentPlayListAdapter.ViewHolder>(diffCallback) {
    //条目比较器
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemForVideo>() {
            override fun areItemsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem == newItem
            }
        }
    }
    //ViewHolder
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val TouchPad: View = itemView.findViewById(R.id.TouchPad)
        val tvThumb: ImageView = itemView.findViewById(R.id.tvThumb)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvArtist: TextView = itemView.findViewById(R.id.tvArtist)
        val ButtonDelete: ImageView = itemView.findViewById(R.id.ButtonDelete)
        val ButtonPlay: ImageView = itemView.findViewById(R.id.ButtonPlay)
    }
    //协程作用域
    //private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //封面缓存
    private val CoverBitmapCache = LruCache<Int, Bitmap>(30 * 1024 * 1024)


    init {
        loadAllCoverFrame()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_list_adapter_items, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.tvName.text = item.name.substringBeforeLast(".")
        /*
        holder.tvArtist.text = item.artist.ifEmpty { "未知" }
        val frame = CoverBitmapCache.get(item.name.hashCode())
        if (frame == null) {generateCoverFrame(item, holder)}
        else{
            holder.tvThumb.setImageBitmap(frame)
        }
        //点击事件设定
        holder.TouchPad.setOnClickListener { onItemClick(item.uri) }
        holder.tvDuration.setOnClickListener { onDurationClick(item) }
        holder.tvOption.setOnClickListener {
            ToolVibrate().vibrate(context)
            //显示菜单
            val popup = PopupMenu(holder.itemView.context, holder.tvOption)
            popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
            val popup_update_cover = popup.menu.findItem(R.id.MenuAction_Repic)
            val popup_hide_text = popup.menu.findItem(R.id.MenuAction_Hide)
            val popup_onSmallCardPlay = popup.menu.findItem(R.id.MenuAction_onSmallCardPlay)
            val local_isHidden = item.isHidden
            popup_hide_text.title = if (local_isHidden) "取消隐藏" else "隐藏"
            popup.show()
            //注册点击
            popup_hide_text.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                onItemHideClick(item.uri, !local_isHidden)
                item.isHidden = !local_isHidden
                true
            }
            popup_update_cover.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                context.showCustomToast("进入视频后,可在更多选项面板更新封面", Toast.LENGTH_SHORT, 3)
                true
            }
            popup_onSmallCardPlay.setOnMenuItemClickListener {
                ToolVibrate().vibrate(context)
                onSmallCardPlay(item.uri, item.name)
                true
            }
        }
        holder.tvFormat.setOnClickListener { onFormatClick(item, item.format) }

         */
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        val item = getItem(position) ?: return

    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.bindingAdapterPosition
    }



    //Functions
    //更新指定位置的封面
    fun updateCoverForVideo(videoName: String) {
        //先检查新图是否存在
        val covers_path = File(context.filesDir, "miniature/cover")
        val cover_file = File(covers_path, "${videoName.hashCode()}.webp")
        if (cover_file.exists()) {
            CoverBitmapCache.remove(videoName.hashCode())
            val bitmap = BitmapFactory.decodeFile(cover_file.absolutePath)
            if (bitmap != null) {
                CoverBitmapCache.put(videoName.hashCode(), bitmap)
            }
        }
        //遍历列表并换图
        snapshot().forEachIndexed { index, mediaItem ->
            if (mediaItem?.name == videoName) {
                notifyItemChanged(index)
            }
        }
    }
    //内部Functions
    @SuppressLint("DefaultLocale")
    private fun FormatTime_numOnly(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        //不显示时
        return if (hours == 0L){
            String.format("%02d:%02d",  minutes, seconds)
        }
        //显示时
        else{
            String.format("%02d:%02d:%02d",  hours, minutes, seconds)
        }
    }
    //uri转绝对路径
    private fun getAbsoluteFilePath(context: Context, uri: Uri): String? {
        var absolutePath: String? = null

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val projection = arrayOf(MediaStore.Video.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    absolutePath = it.getString(columnIndex)
                }
            }
        }
        else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            absolutePath = uri.path
        }

        if (absolutePath != null && File(absolutePath).exists()) {
            return absolutePath
        }
        return null
    }
    //读取所有封面图
    private fun loadAllCoverFrame(){
        val covers_path = File(context.filesDir, "miniature/cover")
        covers_path.mkdirs()
        val files = covers_path.listFiles { file -> file.extension in listOf("webp") }
        files?.forEach { file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val key = (file.name.removeSuffix(".webp")).toInt()
            CoverBitmapCache.put(key, bitmap)
        }
    }



}