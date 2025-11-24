package com.suming.player

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast

fun Context.showCustomToast(message: String, duration: Int = Toast.LENGTH_SHORT, position: Int) {

    val activity = this as? Activity ?: return

    val inflater: LayoutInflater = activity.layoutInflater

    val layout: View = inflater.inflate(R.layout.toast, activity.findViewById(R.id.custom_toast_root))


    val textView: TextView = layout.findViewById(R.id.ToastText)
    textView.text = message

    val toast = Toast(this)


    when (position) {
        1 -> {
            toast.setGravity(Gravity.BOTTOM, 0, 300)
        }
        2 -> {
            toast.setGravity(Gravity.TOP, 0, 300)
        }
        3 -> {
            toast.setGravity(Gravity.CENTER, 0, 0)
        }
    }


    toast.apply {
        this.duration = duration
        this.view = layout
        this.show()
    }
}

