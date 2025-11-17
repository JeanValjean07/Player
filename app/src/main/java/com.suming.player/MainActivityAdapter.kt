package com.suming.player

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
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
import coil.load
import data.MediaItemRepo
import data.MediaItemSetting
import data.MediaModel.MediaItem_video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivityAdapter(
    private val context: Context,
    private val onItemClick: (MediaItem_video) -> Unit,
    private val onDurationClick: (MediaItem_video) -> Unit,
    private val onOptionClick: (MediaItem_video) -> Unit,
    private val onItemHideClick: (String, Boolean) -> Unit
):PagingDataAdapter<MediaItem_video, MainActivityAdapter.ViewHolder>(diffCallback) {

    //比较器
    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<MediaItem_video>() {
            override fun areItemsTheSame(oldItem: MediaItem_video, newItem: MediaItem_video): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: MediaItem_video, newItem: MediaItem_video): Boolean {
                return oldItem == newItem
            }
        }
    }
    //ViewHolder
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val TouchPad: View = itemView.findViewById(R.id.TouchPad)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        val tvOption: CardView = itemView.findViewById(R.id.options)
    }

    //协程作用域
    private val coroutineScopeGenerateCover = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutineScopeSaveRoom = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val coroutineScopeReadRoom = CoroutineScope(Dispatchers.IO + SupervisorJob())





    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_main_adapter_items, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "QueryPermissionsNeeded")
    override fun onBindViewHolder(holder: ViewHolder, position: Int)  {
        val item = getItem(position) ?: return
        holder.tvName.text = item.name
        holder.tvDuration.text = format_time_for_show(item.durationMs)
        holder.tvThumb.load(item.Media_Cover_Path)
        //点击事件设定
        holder.TouchPad.setOnClickListener { onItemClick(item) }
        holder.tvDuration.setOnClickListener { onDurationClick(item) }
        holder.tvOption.setOnClickListener {
             val popup = PopupMenu(holder.itemView.context, holder.tvOption)
            popup.menuInflater.inflate(R.menu.activity_main_popup_options, popup.menu)
            //读数据库
            var itemHided = false
            coroutineScopeReadRoom.launch {
                val setting = MediaItemRepo.get(context).getSetting(item.name) ?: return@launch
                if (setting.PREFS_Hide){
                    itemHided = true
                    popup.menu.findItem(R.id.MenuAction_Hide).title = "取消隐藏"
                }else{
                    itemHided = false
                }
            }
            popup.setOnMenuItemClickListener { menu_item ->
                when(menu_item.itemId){
                    R.id.MenuAction_Repic -> {
                        context.showCustomToast( "进入视频后,在更多按钮面板可重新截取封面", Toast.LENGTH_SHORT,3)
                        true
                    }
                    R.id.MenuAction_Hide -> {
                        if (itemHided) {
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
            popup.show()
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        val item = getItem(position) ?: return

        coroutineScopeGenerateCover.launch(Dispatchers.IO) {
            //需要生成缩略图
            if (item.Media_Cover_Path == ""){

                val retriever = MediaMetadataRetriever()
                //核心逻辑:::
                try {
                    //生成封面缩略图
                    retriever.setDataSource(getAbsoluteFilePath(context, item.uri) ?: item.uri.toString())

                    //Log.d("SuMing", "生成缩略图: ${item.uri}")



                    val bitmap = retriever.getFrameAtTime(1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                    //生成成功
                    if (bitmap != null){
                        //创建目录
                        val saveCover = File(context.cacheDir, "Media/${item.name.hashCode()}/cover/cover.jpg")
                        saveCover.parentFile?.mkdirs()
                        //保存图片
                        saveCover.outputStream().use {
                            val success = bitmap.compress(Bitmap.CompressFormat.WEBP, 10, it)
                            if (!success) { bitmap.compress(Bitmap.CompressFormat.JPEG, 10, it) }
                        }
                        //刷新页面
                        withContext(Dispatchers.Main){
                            holder.tvThumb.setImageBitmap(bitmap)
                        }

                        //修改item中的缩略图链接
                        item.Media_Cover_Path = saveCover.path
                        //存入数据库
                        coroutineScopeSaveRoom.launch {


                            val newSetting = MediaItemSetting(MARK_FileName = item.name, SavePath_Cover = saveCover.path,)
                            MediaItemRepo.get(context).saveSetting(newSetting)



                           // MediaItemRepo.get(context).update_cover_path(item.name, saveCover.path)

                            //MediaItemRepo.get(context).preset_all_row_without_cover_path(item.name, saveCover.path)
                        }
                    }
                    //生成失败
                    else{
                        Toast.makeText(context, "存在无法生成缩略图的视频,请手动为其截取", Toast.LENGTH_SHORT).show()
                    }

                }
                catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }

            }
            //已有缩略图可直接绑定
            else{
                return@launch
                //交给BindViewHolder来承担
                /*
                Log.d("MediaReader", "${item.name}:可直接绑定已有缩略图")
                val BitmapExist = BitmapFactory.decodeFile(item.Media_Cover_Path!!)
                holder.tvThumb.setImageBitmap(BitmapExist)

                 */
            }
        }

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

}