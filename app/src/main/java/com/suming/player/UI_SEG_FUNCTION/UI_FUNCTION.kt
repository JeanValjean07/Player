package com.suming.player.UI_SEG_FUNCTION

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class UI_FUNCTION {

    //实现振动
    private fun vibrate(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ToolVibrate.vibrate()
        }
    }


    //v1_v1: 页面风格v1，检查参数v1
    //约定onResult返回值：1=太大了 2=太小了 3=符合要求  onConfirm返回值：true=弹窗可以消失了 false=继续显示
    @SuppressLint("InflateParams")
    fun show_number_input_Dialog_v1_v1(
        context: Context,
        ui_title: String, ui_description: String, ui_hint: String,ui_button_text: String,
        rule_num_min: Int, rule_num_max: Int, ban_num: List<Int> = emptyList(), pass_num: List<Int> = emptyList(),
        onConfirm: (Int) -> Boolean, onResult: (Int) -> Unit
    ) {
        val dialog = Dialog(context).apply { window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) }
        val dialogView = LayoutInflater.from(context).inflate(R.layout.base_dialog_input_v1_value_single, null)
        dialog.setContentView(dialogView)

        val titleView: TextView = dialogView.findViewById(R.id.dialog_title)
        val descView: TextView = dialogView.findViewById(R.id.dialog_description)
        val editText: EditText = dialogView.findViewById(R.id.dialog_input)
        val button: Button = dialogView.findViewById(R.id.dialog_button)
        titleView.text = ui_title
        descView.text = ui_description
        editText.hint = ui_hint
        button.text = ui_button_text


        button.setOnClickListener {

        }

        dialog.show()
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        CoroutineScope(Dispatchers.Main).launch {
            delay(300.milliseconds)
            editText.requestFocus()
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }



    //v1_v2: 页面风格v1，检查参数v2
    @SuppressLint("InflateParams")
    fun show_number_input_Dialog_v1_v2(
        context: Context,
        ui_title: String, ui_description: String, ui_hint: String, ui_button_text: String,
        //调用方自己的规则
        validate: (Int) -> ValidationResult,
        //校验通过后的副作用
        onValid: (Int) -> Unit,
        //校验不通过后的副作用
        onReject: (String) -> Unit,
    ){
        //创建弹窗
        val dialog = Dialog(context).apply { window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable()) }
        val dialogView = LayoutInflater.from(context).inflate(R.layout.base_dialog_input_v1_value_single, null)
        dialog.setContentView(dialogView)
        //修改视图
        val titleView: TextView = dialogView.findViewById(R.id.dialog_title)
        val descView: TextView = dialogView.findViewById(R.id.dialog_description)
        val editText: EditText = dialogView.findViewById(R.id.dialog_input)
        val button: Button = dialogView.findViewById(R.id.dialog_button)
        titleView.text = ui_title
        descView.text = ui_description
        editText.hint = ui_hint
        button.text = ui_button_text
        //
        button.setOnClickListener {
            //基础非空校验
            val inputValue = editText.text.toString().toIntOrNull()
            if (inputValue == null) {
                onReject("未输入任何内容")
                return@setOnClickListener
            }
            //交给调用方校验数值
            val result = validate(inputValue)
            when (result) {
                is ValidationResult.Ok -> {
                    //回调校验通过并关闭弹窗
                    onValid(inputValue)
                    dialog.dismiss()
                }
                is ValidationResult.Reject -> {
                    //回调校验不通过并显示提示
                    onReject(result.message)
                }
            }
        }


        dialog.show()

        //自动弹出输入法
        editText.requestFocus()
        WindowCompat.getInsetsController(dialog.window!!, editText).show(WindowInsetsCompat.Type.ime())

    }



    //显示弹出菜单
    fun show_popup_Menu_v1_v1_base_xml(
        context: Context,
        anchor:View, @MenuRes menuRes:Int,
        onSelect: (MenuItem) -> Boolean
    ) {

        val popup = PopupMenu(context, anchor)
        popup.menuInflater.inflate(menuRes, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            onSelect(item)
        }
        popup.show()

    }
    //显示弹出菜单
    fun show_popup_Menu_v1_v2_base_dynamic_add(
        anchor: View,
        options: List<Pair<String, () -> Unit>>
    ) {
        val popup = PopupMenu(anchor.context, anchor)
        options.forEachIndexed { i, (label, _) ->
            popup.menu.add(Menu.NONE, i, i, label)
        }
        popup.setOnMenuItemClickListener { item ->
            vibrate()
            options[item.itemId].second()
            true
        }
        popup.show()
    }





}