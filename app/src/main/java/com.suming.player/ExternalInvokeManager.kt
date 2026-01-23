package com.suming.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi

class ExternalInvokeManager : AppCompatActivity(){


    //启动来源标记 < 1 = ACTION_SEND/ACTION_VIEW 丨 2 = pending 丨 3 = 其他 >
    private var source = 0


    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        val uri = ExtractMediaUri(intent)

        when(source){
            1 -> {
                if (uri == Uri.EMPTY){

                    showCustomToast("链接无效", Toast.LENGTH_SHORT, 3)

                }else{

                    //确认启动
                    val playPageType = SettingsRequestCenter.get_PREFS_PlayPageType(this)
                    when(playPageType){
                        0 -> {
                            //构建intent
                            val intent = Intent(this, PlayerActivityOro::class.java).apply { putExtra("uri", uri) }
                                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

                            startActivity(intent)

                        }
                        1 -> {
                            //构建intent
                            val intent = Intent(this, PlayerActivityNeo::class.java).apply { putExtra("uri", uri) }
                                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

                            startActivity(intent)
                        }
                    }

                }

            }
            2 -> {

                //确认启动
                val playPageType = SettingsRequestCenter.get_PREFS_PlayPageType(this)
                when(playPageType){
                    0 -> {
                        //构建intent
                        val intent = Intent(this, PlayerActivityOro::class.java).apply { putExtra("uri", Uri.EMPTY) }
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

                        startActivity(intent)

                    }
                    1 -> {
                        //构建intent
                        val intent = Intent(this, PlayerActivityNeo::class.java).apply { putExtra("uri", Uri.EMPTY) }
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

                        startActivity(intent)
                    }
                }

            }
            else -> {

                showCustomToast("输入不合法", Toast.LENGTH_SHORT, 3)

            }


        }




        finish()

    }


    //检查intent并返回uri信息丨Triple<A: 1=获取链接成功 2=获取失败丨B: 0=从originIntent 1=从pendingIntent获取丨C:Uri>
    private fun ExtractMediaUri(intent: Intent): Uri {
        when (intent.action) {
            //系统面板：分享
            Intent.ACTION_SEND -> {
                source = 1

                val intentUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)

                return intentUri ?: Uri.EMPTY
            }
            //系统面板：选择其他应用打开
            Intent.ACTION_VIEW -> {

                source = 1

                val intentUri = intent.data

                return intentUri ?: Uri.EMPTY
            }
            //正常打开
            else -> {
                var intentUri = IntentCompat.getParcelableExtra(intent, "uri", Uri::class.java)?: Uri.EMPTY
                if (intent != Uri.EMPTY){

                    source = 3

                    return intentUri
                }
                if (intent.getStringExtra("IntentSource") == "FromPendingIntent"){

                    source = 2

                    val intentUriString = intent.getStringExtra("MediaInfo_MediaUri") ?: ""

                    if (intentUriString == ""){

                        return Uri.EMPTY

                    }else{
                        intentUri = intentUriString.toUri()

                        return intentUri
                    }

                }else{

                    source = 3

                    return Uri.EMPTY

                }

            }
        }
    }




}