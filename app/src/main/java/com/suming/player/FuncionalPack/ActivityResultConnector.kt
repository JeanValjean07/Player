package com.suming.player.FuncionalPack

object ActivityResultConnector {

    //隐私页面标签
    const val ARAPI_Privacy = "ARAPI_Privacy"
    //隐私页面具体事件码
    const val ARAPI_Privacy_continue_without_storage_permission = "ARAPI_Privacy_continue_without_storage_permission"
    const val ARAPI_Privacy_continue_with_success_permit = "ARAPI_Privacy_continue_with_success_permit"


    //其他返回码 OBRTV = object return value
    //媒体检查器 MediaChecker MCK
    const val OBRTV_MCK = "OBRTV_MCK"
    const val OBRTV_MCK_FileNotExist = "OBRTV_MCK_FileNotExist"
    const val OBRTV_MCK_FileExist = "OBRTV_MCK_FileExist"

    //播放器核心 PlayerCore PC
    const val OBRTV_PC = "OBRTV_PC"
    const val OBRTV_PC_PlaySuccess = "OBRTV_PC_PlaySuccess"
    const val OBRTV_PC_PlayFailed = "OBRTV_PC_PlayFailed"



}