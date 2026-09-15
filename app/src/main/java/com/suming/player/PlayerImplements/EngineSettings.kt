package com.suming.player.PlayerImplements

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

object EngineSettings {

    //context
    private lateinit var context: Application
    fun setContext(context: Context){
        //检查是不是applicationContext
        if (context is Application) {

            this.context = context
        }
    }

    //空字段
    private val Undefined = ""
    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "EngineSettings: $msg")
        }
    }


    //Core
    //核心键值对清单
    private var Pandora_Engine_Core: SharedPreferences ?= null
    const val Pandora_Engine_Core_Name = "Pandora_Engine_Core"
    private fun open_pandora_Core(context: Context){
        if (Pandora_Engine_Core == null){
            Pandora_Engine_Core = context.getSharedPreferences(Pandora_Engine_Core_Name, 0)
        }
    }


    //当前选择的引擎类型
    private var engine_type: String = Undefined
    const val engine_type_Name = "engine_type"
    fun GET_PREFS_Engine_Type():String{
        open_pandora_Core(context)

        if (engine_type == Undefined){
            engine_type = Pandora_Engine_Core?.getString(engine_type_Name ,Undefined) ?: Undefined
            if (engine_type == Undefined){
                //
                engine_type = EngineType.Engine_ExoPlayer
                Pandora_Engine_Core?.edit { putString(engine_type_Name, EngineType.Engine_ExoPlayer) }

            }
        }

        return engine_type
    }
    fun SET_PREFS_Engine_Type(type:String){
        open_pandora_Core(context)

        engine_type = EngineType.Engine_ExoPlayer
        Pandora_Engine_Core?.edit { putString(engine_type_Name, EngineType.Engine_ExoPlayer) }
    }

























}