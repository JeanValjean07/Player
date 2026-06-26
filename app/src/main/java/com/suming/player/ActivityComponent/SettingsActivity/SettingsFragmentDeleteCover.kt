package com.suming.player.ActivityComponent.SettingsActivity

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.util.UnstableApi
import com.suming.player.R
import com.suming.player.AddonTools.ToolVibrate

@UnstableApi
@Suppress("unused")
class SettingsFragmentDeleteCover: DialogFragment() {

    companion object {
        fun newInstance(): SettingsFragmentDeleteCover = SettingsFragmentDeleteCover().apply { arguments =
            bundleOf()
        }
    }

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){

            //横屏时隐藏状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { view, insets -> WindowInsetsCompat.CONSUMED }

                /*
                dialog?.window?.decorView?.post { dialog?.window?.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } }

                 */

                //三星专用:显示到挖空区域
                dialog?.window?.attributes?.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                dialog?.window?.decorView?.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }

            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOutHorizontal)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOut)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            if(context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
                val decorView: View = dialog?.window?.decorView ?: return
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.activity_settings_delete_cover_item, container, false)

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //按钮：退出
        val ButtonExit = view.findViewById<ImageButton>(R.id.buttonExit)
        ButtonExit.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            dismiss()
        }
        //按钮：点击空白区域退出
        val topArea = view.findViewById<View>(R.id.out_area)
        topArea.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            dismiss()
        }
        val ButtonCancel = view.findViewById<CardView>(R.id.ButtonCancel)
        ButtonCancel.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            dismiss()
        }

        //按钮：删除所有封面
        val ButtonDeleteAll = view.findViewById<CardView>(R.id.ButtonDeleteAll)
        ButtonDeleteAll.setOnClickListener {
            ToolVibrate().vibrate(requireContext())

            val result = bundleOf("KEY" to "DeleteAllCover")
            setFragmentResult("FROM_FRAGMENT_DELETE_COVER", result)

            dismiss()
        }
        val ButtonDeleteVideoCover = view.findViewById<CardView>(R.id.ButtonDeleteVideoCover)
        ButtonDeleteVideoCover.setOnClickListener {
            ToolVibrate().vibrate(requireContext())

            val result = bundleOf("KEY" to "DeleteVideoCover")
            setFragmentResult("FROM_FRAGMENT_DELETE_COVER", result)

            dismiss()
        }
        val ButtonDeleteMusicCover = view.findViewById<CardView>(R.id.ButtonDeleteMusicCover)
        ButtonDeleteMusicCover.setOnClickListener {
            ToolVibrate().vibrate(requireContext())

            val result = bundleOf("KEY" to "DeleteMusicCover")
            setFragmentResult("FROM_FRAGMENT_DELETE_COVER", result)

            dismiss()
        }



        //监听返回手势(DialogFragment)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismiss()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    //onViewCreated END
    }

}