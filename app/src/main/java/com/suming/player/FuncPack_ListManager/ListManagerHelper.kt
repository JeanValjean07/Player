package com.suming.player.FuncPack_ListManager

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import com.suming.player.DataPack.DataClass.MediaItemFullForAudio
import com.suming.player.DataPack.DataClass.MediaItemFullForVideo

@Suppress("unused")
object ListManagerHelper {
    //context
    private lateinit var context: Application
    fun setContext(context: Context){
        //检查是不是applicationContext
        if (context is Application) {
            this.context = context
        }else{
            consoleLog("PlayerListManager.setContext error")
        }
    }
    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "ListManagerHelper: $msg")
        }
    }



    //列表管理器设置
    private var Paradox_List: SharedPreferences? = null
    const val Paradox_List_Name = "Paradox_List_Name"
    private fun initListSetting(){
        if (Paradox_List != null) return
        Paradox_List = context.getSharedPreferences(Paradox_List_Name, MODE_PRIVATE)
    }
    //循环模式
    private var PREFS_LoopMode = ""
    const val PREFS_LoopMode_Key = "PREFS_LoopMode"
    const val LOOP_MODE_OFF = "OFF"
    const val LOOP_MODE_ONE = "ONE"
    const val LOOP_MODE_ALL = "ALL"
    @SuppressLint("StaticFieldLeak")
    fun setLoopMode(mode: String, context: Context) {
        initListSetting()

        //刷新缓存并落盘
        PREFS_LoopMode  = mode
        Paradox_List?.edit{ putString(PREFS_LoopMode_Key, PREFS_LoopMode ).apply() }
    }
    fun getLoopMode(context: Context): String{
        initListSetting()


        PREFS_LoopMode  = Paradox_List?.getString(PREFS_LoopMode_Key, "OFF") ?: "OFF"
        if (PREFS_LoopMode != "OFF" && PREFS_LoopMode != "ONE" && PREFS_LoopMode != "ALL"){ PREFS_LoopMode = "OFF" }

        return PREFS_LoopMode
    }


    //列表字段
    const val ListMark_Null = ""
    const val ListMark_UseLast = "ListMark_UseLast"
    const val ListMark_Custom = "ListMark_Custom"
    const val ListMark_History = "ListMark_History"
    const val ListMark_Video = "ListMark_Video"
    const val ListMark_Audio = "ListMark_Audio"



    //当前正显示的列表
    private var state_currentShowingListMark = ListMark_Null
    fun TURNTO_List(targetList: String){
        //记录到当前正显示的列表
        state_currentShowingListMark = targetList
        //保存到上一次显示的列表记录
        SET_STE_LastShowingListMark(targetList)
    }
    //默认显示的列表
    private var PRFR_AcquiesceShowingPage = ListMark_Null
    const val PRFR_KEYNAME_AcquiesceShowingPage_Key = "PRFR_KEYNAME_AcquiesceShowingPage"
    fun GET_PRFR_AcquiesceShowingPage(): String{
        initListSetting()

        //仅在无缓存时读取
        if (PRFR_AcquiesceShowingPage == ListMark_Null){
            PRFR_AcquiesceShowingPage = Paradox_List?.getString(PRFR_KEYNAME_AcquiesceShowingPage_Key, ListMark_Null) ?: ListMark_Null
            //检查并置入默认值
            if (PRFR_AcquiesceShowingPage == ListMark_Null){
                //设置useLast页签为默认值
                Paradox_List?.edit { putString(PRFR_KEYNAME_AcquiesceShowingPage_Key, ListMark_UseLast) }
                PRFR_AcquiesceShowingPage = ListMark_UseLast
            }
        }


        return PRFR_AcquiesceShowingPage
    }
    fun SET_PRFR_AcquiesceShowingPage(page: String): Boolean{
        initListSetting()

        //过滤无效值
        if (page != ListMark_Custom && page != ListMark_UseLast && page != ListMark_Video && page != ListMark_Audio && page != ListMark_History){
            return false
        }

        //刷新缓存并落盘
        PRFR_AcquiesceShowingPage = page
        Paradox_List?.edit { putString(PRFR_KEYNAME_AcquiesceShowingPage_Key, page) }

        return true
    }
    //上一次显示的列表
    private var state_LastShowingListMark = ListMark_Null
    private const val state_LastShowingListMark_KeyName = "state_LastShowingListMark_KeyName"
    fun GET_STE_LastShowingListMark(): String{
        initListSetting()

        //仅在无缓存时读取
        if (state_LastShowingListMark == ListMark_Null){

            state_LastShowingListMark = Paradox_List?.getString(state_LastShowingListMark_KeyName, ListMark_Null) ?: ListMark_Null
            //检查并置入默认值
            if (state_LastShowingListMark == ListMark_Null){

                //设置useLast页签为默认值
                Paradox_List?.edit { putString(state_LastShowingListMark_KeyName, ListMark_UseLast) }
                state_LastShowingListMark = ListMark_UseLast
            }
        }

        return state_LastShowingListMark
    }
    private fun SET_STE_LastShowingListMark(page: String): Boolean{
        initListSetting()

        //过滤无效值
        if (page != ListMark_Custom && page != ListMark_UseLast && page != ListMark_Video && page != ListMark_Audio && page != ListMark_History){
            return false
        }

        //刷新缓存并落盘
        state_LastShowingListMark = page
        Paradox_List?.edit { putString(state_LastShowingListMark_KeyName, page) }

        return true
    }











    //自定义列表内容
    var ListContent_CustomList = mutableListOf<customListItem>()
    //已播列表内容
    var ListContent_HistoryList = mutableListOf<historyListItem>()


    //自定义列表
    data class customListItem(
            var uriNumOnly: Long,
            var title: String,
            var uri: Uri,
            var type: String,
        )
    //向自定义列表中插入一项
    fun customList_insertItem(item: customListItem){
        /*
        val item = getMediaInfo(uriString, context)
        if (item == MiniMediaItemForList.EMPTY) return

        if (ListContent_CustomList.any { it.uriNumOnly == item.uriNumOnly }) return
        ListContent_CustomList.add(item)

         */

    }
    //从自定义列表中删除一项
    fun customList_deleteItem(item: customListItem){
        //customList.removeAll { it.uriNumOnly == item.uriNumOnly }

    }
    //整表替换自定义列表
    fun customList_replaceFully(newList: List<customListItem>) {
        ListContent_CustomList.clear()
        ListContent_CustomList.addAll(newList)
    }
    //清空自定义列表
    fun customList_clearAll(){
        ListContent_CustomList.clear()
    }

    //已播列表
    data class historyListItem(
        var uriNumOnly: Long,
        var title: String,
        var uri: Uri,
        var type: String,
    )
    //向已播列表中插入一项
    fun historyList_insertItem(item: historyListItem){
        /*
        val item = getMediaInfo(uriString, context)
        if (item == MiniMediaItemForList.EMPTY) return

        if (ListContent_CustomList.any { it.uriNumOnly == item.uriNumOnly }) return
        ListContent_CustomList.add(item)

         */

    }
    //从已播列表中删除一项
    fun historyList_deleteItem(item: historyListItem){
        //customList.removeAll { it.uriNumOnly == item.uriNumOnly }

    }
    //整表替换已播列表
    fun historyList_replaceFully(newList: List<historyListItem>) {
        ListContent_HistoryList.clear()
        ListContent_HistoryList.addAll(newList)
    }
    //清空已播列表
    fun historyList_clearAll(){
        ListContent_HistoryList.clear()
    }


    //列表Fragment间通信字段
    const val event_key_general = "event_key_general"
    const val event_key_extra = "event_key_extra"
    const val fragment_request_key_custom = "fragment_request_key_custom"
    const val fragment_request_key_custom_reverse = "fragment_request_key_custom_reverse"
    const val fragment_request_key_history = "fragment_request_key_history"
    const val fragment_request_key_history_reverse = "fragment_request_key_history_reverse"
    const val fragment_request_key_video = "fragment_request_key_video"
    const val fragment_request_key_video_reverse = "fragment_request_key_video_reverse"
    const val fragment_request_key_audio = "fragment_request_key_audio"
    const val fragment_request_key_audio_reverse = "fragment_request_key_audio_reverse"

    //通信事件
    //通用通信事件(几个页面用同一个字段) event_detail_general_
    const val event_detail_general_update_currentPlayingList_icon = "event_detail_general_update_currentPlayingList_icon"
    const val event_detail_general_goto_list_top = "event_detail_general_goto_list_top"
    const val event_detail_general_update_list_state = "event_detail_general_update_list_state"
    const val event_detail_general_media_item_update = "event_detail_general_media_item_update"
    const val event_detail_general_media_state_update = "event_detail_general_media_state_update"
    //特殊通信事件(每个页面用不同的字段) event_detail_



    //Adaptor Payload字段 payload_event_
    const val payload_event_item_update = "payload_event_item_update"
    const val payload_event_item_state_update = "payload_event_item_state_update"
    const val payload_event_item_clear_playing_mark = "payload_event_item_clear_playing_mark"





    //显示字段
    const val string_already_set_playing_list = "已设为当前播放列表"
    const val string_set_as_playing_list = "设为当前播放列表"






    //播放接力管理
    //当前正在播放的列表
    private var state_currentPlayingListMark = ListMark_Video  //TODO
    fun SET_STE_CurrentPlayingListMark(targetList: String): Boolean{
        initListSetting()
        //设置当前正在播放的列表
        when(targetList){
            ListMark_Custom -> {
                state_currentPlayingListMark = targetList
                return true
            }
            ListMark_History -> {
                state_currentPlayingListMark = targetList
                return true
            }
            ListMark_Video -> {
                state_currentPlayingListMark = targetList
                return true
            }
            ListMark_Audio -> {
                state_currentPlayingListMark = targetList
                return true
            }
            //设置失败
            else -> {
                return false
            }
        }

    }
    fun GET_STE_CurrentPlayingListMark(): String{

        return state_currentPlayingListMark
    }




    //获取下一个或上一个媒体项
    fun getMediaItemByOrder(flag_prev_or_next: String, current_uri_num: Long): Triple<String, Uri, String>    {


        return Triple("", Uri.EMPTY, "")
    }


    //查询某个特定uri是否存在于当前播放列表中
    fun getMediaItemByUri(uriNumOnly: Long): Boolean {
        return when(state_currentPlayingListMark){
            ListMark_Custom -> {
                ListContent_CustomList.any { it.uriNumOnly == uriNumOnly }
            }
            ListMark_History -> {
                //ListContent_HistoryList.any { it.media_api_id == uriNumOnly }
                false
            }
            else -> {
                false
            }
        }
    }




    //向实时视频列表和音乐列表传入内容
    fun InfuseLiveVideoList(newList: List<MediaItemFullForVideo>){

    }
    fun InfuseLiveMusicList(newList: List<MediaItemFullForAudio>){

    }








}