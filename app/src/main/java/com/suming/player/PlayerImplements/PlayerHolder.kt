package com.suming.player.PlayerImplements

import android.content.Context
import android.util.Log

object PlayerHolder {

    //空字段
    private val Undefined = ""
    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerInstanceHolder: $msg")
        }
    }




    //当前持有的播放器引擎
    private var _player: PlayerInterface? = null




    //当前正在运行的播放器引擎类型
    private var current_engine_type: String = Undefined




    //
    fun get_ins_refresh(context: Context): PlayerInterface {
        //读取当前选择的引擎类型
        val engine_type = EngineSettings.GET_PREFS_Engine_Type()

        //目标引擎变化时,释放旧的
        if (_player != null && engine_type != current_engine_type) {
            _player?.release()
            _player = null
        }


        //创建新的
        return _player ?: PlayerFactory.create(
            context.applicationContext,
            engine_type
        ).also {
            _player = it
            current_engine_type = engine_type
        }
    }





}