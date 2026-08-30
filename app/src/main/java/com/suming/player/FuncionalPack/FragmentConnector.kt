package com.suming.player.FuncionalPack

object FragmentConnector {


    //Fragment标签合集
    const val fragment_tag_more_button = "fragment_tag_more_button"   //更多选项面板
    const val fragment_tag_equalizer = "fragment_tag_equalizer"  //均衡器面板
    const val fragment_tag_media_info = "fragment_tag_media_info"  //媒体信息面板
    const val fragment_tag_play_list = "fragment_tag_play_list"  //播放列表面板
    const val fragment_tag_video_store_setting = "fragment_tag_video_store_setting"  //视频库设置面板
    const val fragment_tag_music_store_setting = "fragment_tag_music_store_setting"  //音乐库设置面板




    //所有Fragment公用一个receive_key
    const val request_key = "request_key"
    const val receive_key = "receive_key"
    //额外参数key
    const val extra_key = "extra_key"

    //通用的Fragment开启与关闭事件
    const val fragment_event_close = "fragment_event_close"
    const val fragment_event_open = "fragment_event_open"

    //更多操作面板
    const val fragment_request_key_more_button = "fragment_request_key_more_button"  //Activity -> Fragment
    const val fragment_request_key_more_button_reverse = "fragment_request_key_more_button_reverse" //Fragment -> Activity
    //更多操作面板具体事件
    const val fragment_more_button_capture_frame = "fragment_more_button_capture_frame"  //截屏
    const val fragment_more_button_back_to_start = "fragment_more_button_back_to_start"  //回到视频起始
    const val fragment_more_button_start_play_list = "fragment_more_button_start_play_list"  //打开播放列表
    const val fragment_more_button_extract_frame = "fragment_more_button_extract_frame"  //截取全部帧
    const val fragment_more_button_switch_ori_listener = "fragment_more_button_switch_ori_listener"  //开启/关闭方向监听器
    const val fragment_more_button_bind_play_view = "fragment_more_button_bind_play_view"  //重新绑定播放器视图
    const val fragment_more_button_unlock_brightness_control = "fragment_more_button_unlock_brightness_control"  //解除亮度控制
    const val fragment_more_button_clear_miniature = "fragment_more_button_clear_miniature"  //清除当前进度条缩略图
    const val fragment_more_button_update_cover_frame = "fragment_more_button_update_cover_frame"  //更新封面
    const val update_cover_frame_use_current_frame = "use_current_frame"  //更新封面-截取视频当前帧
    const val update_cover_frame_use_default_frame = "use_default_frame"  //更新封面-使用默认封面
    const val update_cover_frame_pick_local_frame = "pick_local_frame"  //更新封面-选择本地图片
    const val fragment_more_button_start_pip_window = "fragment_more_button_start_pip_window"  //开启小窗模式
    const val fragment_more_button_open_video_info = "fragment_more_button_open_video_info"  //打开媒体信息面板
    const val fragment_more_button_open_equalizer = "fragment_more_button_open_equalizer"  //打开均衡器面板
    const val fragment_more_button_sys_share_video = "fragment_more_button_sys_share_video"  //使用系统分享面板
    const val fragment_more_button_exit_right_now = "fragment_more_button_exit_right_now"  //立即退出
    const val fragment_more_button_delete_custom_cover = "fragment_more_button_delete_custom_cover"  //删除自定义封面图
    const val fragment_more_button_update_keep_screen_on = "fragment_more_button_update_keep_screen_on"  //刷新屏幕常亮状态
    const val fragment_more_button_updated_seek_mode = "fragment_more_button_updated_seek_mode"  //SeekMode刷新了
    const val fragment_more_button_updated_link_scroll = "fragment_more_button_updated_link_scroll"  //LinkScroll刷新了
    const val fragment_more_button_updated_tap_jump = "fragment_more_button_updated_tap_jump"  //TapJump刷新了



    //播放列表面板
    const val fragment_request_key_play_list = "fragment_request_key_play_list" //Activity -> Fragment
    const val fragment_request_key_play_list_reverse = "fragment_request_key_play_list_reverse" //Fragment -> Activity
    //播放列表面板具体事件
    const val fragment_play_list_require_refresh = "fragment_play_list_require_refresh"


    //均衡器面板
    const val fragment_request_key_equalizer = "fragment_request_key_equalizer" //Activity -> Fragment
    const val fragment_request_key_equalizer_reverse = "fragment_request_key_equalizer_reverse" //Fragment -> Activity
    //均衡器面板具体事件



    //媒体信息面板
    const val fragment_request_key_media_info = "fragment_request_key_media_info" //Activity -> Fragment
    const val fragment_request_key_media_info_reverse = "fragment_request_key_media_info_reverse" //Fragment -> Activity
    //媒体信息面板具体事件



    //媒体库面板request_key
    const val fragment_request_key_video_store_setting = "fragment_request_key_video_store_setting"  //独立key
    const val fragment_request_key_music_store_setting = "fragment_request_key_music_store_setting"  //独立key
    //媒体库面板事件(视频库和音乐库公用事件)
    const val fragment_media_store_setting_require_recyclerview_refresh = "fragment_media_store_setting_require_recyclerview_refresh"  //请求列表刷新
    const val fragment_media_store_setting_require_mediastore_api_refresh = "fragment_media_store_setting_require_mediastore_api_refresh"  //请求媒体库API刷新(重读媒体文件)










}