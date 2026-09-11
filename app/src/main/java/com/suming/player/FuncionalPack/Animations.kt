package com.suming.player.FuncionalPack

import android.view.animation.AlphaAnimation

object Animations {

    //淡入动画250ms
    val FadeIn: AlphaAnimation = AlphaAnimation(0.0f, 1.0f).apply { duration = 250 }



    //顶栏效果
    val topBar_Effect_Brush_Duration = 250L
    val topBar_Effect_Square_Duration = 100L


}