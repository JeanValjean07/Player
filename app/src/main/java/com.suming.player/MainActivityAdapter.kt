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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import data.DataBaseMediaStore.MediaStoreRepo
import data.MediaModel.MediaItemForVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivityAdapter(
    private val context: Context,
    private val onItemClick: (MediaItemForVideo) -> Unit,
    private val onDurationClick: (MediaItemForVideo) -> Unit,
    private val onOptionClick: (MediaItemForVideo) -> Unit,
    private val onItemHideClick: (String, Boolean) -> Unit
):PagingDataAdapter<MediaItemForVideo, MainActivityAdapter.ViewHolder>(diffCallback) {
    //比较器
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<MediaItemForVideo>() {
            override fun areItemsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MediaItemForVideo, newItem: MediaItemForVideo): Boolean {
                return oldItem == newItem
            }
        }
    }
    //ViewHolder
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val TouchPad: View = itemView.findViewById(R.id.TouchPad)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvFormat: TextView = itemView.findViewById(R.id.tvFormat)
        val tvThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        val tvOption: CardView = itemView.findViewById(R.id.options)
    }
    //协程作用域
    private val coroutineScopeGenerateCover = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutineScopeSaveRoom = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutineScopeReadRoom = CoroutineScope(Dispatchers.IO + SupervisorJob())
    //图片池
    private val CoverBitmapCache = LruCache<Int, Bitmap>(30 * 1024 * 1024)


    init {
        loadAllCoverFrame()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_adapter_items, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.tvName.text = item.name
        holder.tvDuration.text = format_time_for_show(item.durationMs)
        holder.tvFormat.text = item.format.ifEmpty { "未知" }
        val frame = CoverBitmapCache.get(item.name.hashCode())
        if (frame == null) {generateCoverFrame(item, holder)}
        else{ holder.tvThumb.setImageBitmap(frame) }
        //点击事件设定
        holder.TouchPad.setOnClickListener { onItemClick(item) }
        holder.tvDuration.setOnClickListener { onDurationClick(item) }
        holder.tvOption.setOnClickListener {
            val popup = PopupMenu(holder.itemView.context, holder.tvOption)
            popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
            popup.show()
            val popup_hide_text = popup.menu.findItem(R.id.MenuAction_Hide)
            coroutineScopeReadRoom.launch {
                val isHidden = MediaStoreRepo.get(context).getHideStatus((item.uri.toString().removePrefix("content://media/external/video/media/"))) ?: false

                withContext(Dispatchers.Main){
                    popup_hide_text.title = if (isHidden) "取消隐藏" else "隐藏"
                    popup.setOnMenuItemClickListener { menu_item ->
                        when(menu_item.itemId){
                            R.id.MenuAction_Repic -> {
                                context.showCustomToast( "进入视频后,在更多按钮面板可重新截取封面", Toast.LENGTH_SHORT,3)
                                true
                            }
                            R.id.MenuAction_Hide -> {
                                if (!isHidden) {
                                    context.showCustomToast( "已取消隐藏,刷新后生效", Toast.LENGTH_SHORT,3)
                                    //interface
                                    onItemHideClick(item.name, false)
                                }else{
                                    context.showCustomToast( "仅能在本APP中隐藏,刷新后生效", Toast.LENGTH_SHORT,3)
                                    //interface
                                    onItemHideClick(item.name, true)
                                }
                                true
                            }
                            else -> true
                        }
                    }
                }
            }

        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        val item = getItem(position) ?: return

    }


    //Functions
    @SuppressLint("DefaultLocale")
    private fun format_time_for_show(milliseconds: Long): String {
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
    //生成缩略图
    private fun generateCoverFrame(item: MediaItemForVideo, holder: ViewHolder){
        coroutineScopeGenerateCover.launch(Dispatchers.IO){
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(getAbsoluteFilePath(context, item.uri) ?: item.uri.toString())
                val bitmap = retriever.getFrameAtTime(1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                //生成成功
                if (bitmap != null){
                    //创建目录
                    val covers_path = File(context.filesDir, "miniature/cover")
                    if (!covers_path.exists()) {
                        covers_path.mkdirs()
                    }
                    //保存图片
                    val cover_item_file = File(covers_path, "${item.name.hashCode()}.webp")
                    cover_item_file.outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 10, it)
                        //缓存
                        val key = item.name.hashCode()
                        CoverBitmapCache.put(key, bitmap)
                    }
                    //刷新页面
                    withContext(Dispatchers.Main){
                        holder.tvThumb.setImageBitmap(bitmap)
                    }
                }
                //生成失败
                else{
                    Toast.makeText(context, "存在无法生成缩略图的视频,请手动为其截取", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
            finally {
                retriever.release()
            }
        }
    }


}