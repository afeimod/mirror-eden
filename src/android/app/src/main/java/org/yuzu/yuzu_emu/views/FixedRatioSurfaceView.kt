// SPDX-FileCopyrightText: 2023 yuzu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

package org.yuzu.yuzu_emu.views

import android.content.Context
import android.util.AttributeSet
import android.util.Rational
import android.view.SurfaceView

class FixedRatioSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {
    private var aspectRatio: Float = 0f // (width / height), 0f is a special value for stretch

    /**
     * Sets the desired aspect ratio for this view
     * @param ratio the ratio to force the view to, or null to stretch to fit
     */
    fun setAspectRatio(ratio: Rational?) {
        aspectRatio = ratio?.toFloat() ?: 0f
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 强制拉伸模式 - 不管 aspectRatio 的值如何，始终填满整个父容器
        // 这样可以确保游戏画面总是拉伸到全屏，没有黑边
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
