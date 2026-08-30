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

    //播放器核心 Engine
    const val OBRTV_Engine = "OBRTV_Engine"
    const val OBRTV_Engine_AlreadyPlayingTargetItem = "OBRTV_Engine_AlreadyPlayingTargetItem"
    const val OBRTV_Engine_RetrieveFailed = "OBRTV_Engine_RetrieveFailed"
    const val OBRTV_Engine_SetItemSuccess = "OBRTV_Engine_SetItemSuccess"
    const val OBRTV_Engine_TypeNotSupport = "OBRTV_Engine_TypeNotSupport"  //不支持的媒体类型
    const val OBRTV_Engine_OffLine = "OBRTV_Engine_OffLine"   //播放器离线
    const val OBRTV_Engine_SoFrequent = "OBRTV_Engine_SoFrequent"  //设置过快



    //解码器 retriever
    const val retriever_type_not_support = "retriever_type_not_support"
    const val retriever_complete = "retriever_complete"
    const val retriever_error = "retriever_error"
    const val retriever_get_type_failed = "retriever_get_type_failed"



}