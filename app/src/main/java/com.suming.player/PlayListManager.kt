package com.suming.player

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.WorkerThread
import data.MediaModel.MediaItem_video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class PlayListManager private constructor(context: Context) {
    // 单例模式
    companion object {
        private const val PREFS_NAME = "PlayListPreferences"
        private const val KEY_CURRENT_PLAYLIST = "CurrentPlayList"
        private const val KEY_PLAYING_INDEX = "PlayingIndex"
        private const val KEY_PLAY_MODE = "PlayMode"
        private const val KEY_HISTORY_LIST = "HistoryList"

        @Volatile
        private var INSTANCE: PlayListManager? = null

        fun getInstance(context: Context): PlayListManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayListManager(context).also { INSTANCE = it }
            }
        }
    }

    //播放模式枚举
    enum class PlayMode { SEQUENCE, RANDOM, LOOP }
    //SEQUENCE 顺序播放
    //RANDOM 随机播放
    //LOOP 循环播放

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = preferences.edit()

    //当前播放列表
    private var currentPlayList: MutableList<MediaItem_video> = mutableListOf()
    //当前播放索引
    private var currentPlayingIndex: Int = -1
    //当前播放模式
    private var currentPlayMode: PlayMode = PlayMode.SEQUENCE
    //历史播放列表
    private var historyList: MutableList<Long> = mutableListOf()
    //随机播放:随机数生成器
    private val random = Random()

    init {
        // 初始化时加载保存的播放列表
        loadPlayList()
    }

    suspend fun updatePlayList(videos: List<MediaItem_video>) {
        currentPlayList.clear()
        currentPlayList.addAll(videos)
        currentPlayingIndex = -1
        savePlayList()
    }

    suspend fun updatePlayList_MediaStore(videos: List<MediaItem_video>) {
        currentPlayList.clear()
        currentPlayList.addAll(videos)
        currentPlayingIndex = -1
        savePlayList_MediaStore()
    }


    suspend fun addToPlayList(video: MediaItem_video) {
        if (!currentPlayList.any { it.id == video.id }) {
            currentPlayList.add(video)
            savePlayList()
        }
    }


    suspend fun removeFromPlayList(videoId: Long) {
        val index = currentPlayList.indexOfFirst { it.id == videoId }
        if (index != -1) {
            currentPlayList.removeAt(index)
            if (currentPlayingIndex == index) {
                currentPlayingIndex = -1
            } else if (currentPlayingIndex > index) {
                currentPlayingIndex--
            }
            savePlayList()
        }
    }


    suspend fun setCurrentPlayingVideo(videoId: Long): Boolean {
        val index = currentPlayList.indexOfFirst { it.id == videoId }
        if (index != -1) {
            currentPlayingIndex = index
            // 记录到历史列表
            recordHistory(videoId)
            savePlayList()
            return true
        }
        return false
    }


    fun getCurrentPlayingVideo(): MediaItem_video? {
        return if (currentPlayingIndex >= 0 && currentPlayingIndex < currentPlayList.size) {
            currentPlayList[currentPlayingIndex]
        } else {
            null
        }
    }

    fun getNextVideo(): MediaItem_video? {
        if (currentPlayList.isEmpty()) return null

        return when (currentPlayMode) {
            PlayMode.SEQUENCE -> {
                currentPlayingIndex++
                if (currentPlayingIndex >= currentPlayList.size) {
                    currentPlayingIndex = 0 // 播放到末尾后回到开头
                }
                currentPlayList[currentPlayingIndex]
            }
            PlayMode.RANDOM -> {
                // 随机选择一个不同的视频
                if (currentPlayList.size == 1) {
                    currentPlayList[0]
                } else {
                    var nextIndex: Int
                    do {
                        nextIndex = random.nextInt(currentPlayList.size)
                    } while (nextIndex == currentPlayingIndex)
                    currentPlayingIndex = nextIndex
                    currentPlayList[currentPlayingIndex]
                }
            }
            PlayMode.LOOP -> {
                // 单曲循环
                if (currentPlayingIndex >= 0) {
                    currentPlayList[currentPlayingIndex]
                } else {
                    currentPlayingIndex = 0
                    currentPlayList[0]
                }
            }
        }
    }

    fun getPreviousVideo(): MediaItem_video? {
        if (currentPlayList.isEmpty()) return null

        return when (currentPlayMode) {
            PlayMode.SEQUENCE, PlayMode.LOOP -> {
                currentPlayingIndex--
                if (currentPlayingIndex < 0) {
                    currentPlayingIndex = currentPlayList.size - 1
                }
                currentPlayList[currentPlayingIndex]
            }
            PlayMode.RANDOM -> {
                // 随机选择一个不同的视频
                if (currentPlayList.size == 1) {
                    currentPlayList[0]
                } else {
                    var prevIndex: Int
                    do {
                        prevIndex = random.nextInt(currentPlayList.size)
                    } while (prevIndex == currentPlayingIndex)
                    currentPlayingIndex = prevIndex
                    currentPlayList[prevIndex]
                }
            }
        }
    }

    fun setPlayMode(mode: PlayMode) {
        currentPlayMode = mode
        editor.putInt(KEY_PLAY_MODE, mode.ordinal)
        editor.apply()
    }

    fun getPlayMode(): PlayMode {
        return currentPlayMode
    }

    fun getCurrentPlayList(): List<MediaItem_video> {
        return currentPlayList.toList()
    }

    suspend fun clearPlayList() {
        currentPlayList.clear()
        currentPlayingIndex = -1
        savePlayList()
    }

    private fun recordHistory(videoId: Long) {
        // 先移除已存在的相同ID
        historyList.remove(videoId)
        // 添加到列表开头
        historyList.add(0, videoId)
        // 限制历史记录数量
        if (historyList.size > 100) {
            historyList = historyList.subList(0, 100)
        }
        // 保存历史记录
        saveHistory()
    }

    fun getHistoryList(): List<Long> {
        return historyList.toList()
    }


    @WorkerThread
    private suspend fun savePlayList() {
        withContext(Dispatchers.IO) {
            // 这里简化了存储逻辑，实际应用中可能需要更复杂的序列化方式
            // 例如使用Gson将对象转换为JSON字符串
            val playListString = currentPlayList.joinToString(separator = ",") { it.id.toString() }
            editor.putString(KEY_CURRENT_PLAYLIST, playListString)
            editor.putInt(KEY_PLAYING_INDEX, currentPlayingIndex)
            editor.apply()
        }
    }
    @WorkerThread
    private suspend fun savePlayList_MediaStore() {
        withContext(Dispatchers.IO) {
            val playListString = currentPlayList.joinToString(separator = ",") { it.id.toString() }
            editor.putString(KEY_CURRENT_PLAYLIST, playListString)
            editor.putInt(KEY_PLAYING_INDEX, currentPlayingIndex)
            editor.apply()
            //Log.d("SuMing", "savePlayList_MediaStore: $playListString")
        }

    }


    @WorkerThread
    private fun loadPlayList() {
        // 注意：这里简化了加载逻辑，实际应用中需要从数据库或其他地方获取完整的MediaItem_video对象
        // 这里只加载了ID列表，需要额外的逻辑来获取完整的视频信息
        val playListString = preferences.getString(KEY_CURRENT_PLAYLIST, "")
        currentPlayingIndex = preferences.getInt(KEY_PLAYING_INDEX, -1)
        currentPlayMode = PlayMode.entries.toTypedArray()[preferences.getInt(KEY_PLAY_MODE, PlayMode.SEQUENCE.ordinal)]

        // 加载历史记录
        val historyString = preferences.getString(KEY_HISTORY_LIST, "")
        if (!historyString.isNullOrEmpty()) {
            historyList = historyString.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
        }
    }

    private fun saveHistory() {
        val historyString = historyList.joinToString(",")
        editor.putString(KEY_HISTORY_LIST, historyString)
        editor.apply()
    }
}
