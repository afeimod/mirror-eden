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
        // 从 MeasureSpec 中提取父容器提供的尺寸
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (aspectRatio == 0f) {
            // 拉伸模式：aspectRatio 为 0f 时，填满整个父容器
            // 不维护任何纵横比，让画面完全拉伸到全屏
            setMeasuredDimension(widthSize, heightSize)
        } else {
            // 固定纵横比模式：根据指定的纵横比计算尺寸
            val ratio = aspectRatio

            // 计算基于宽度的情况
            val widthBasedHeight = (widthSize / ratio).toInt()
            // 计算基于高度的情况
            val heightBasedWidth = (heightSize * ratio).toInt()

            // 选择不会超出父容器范围的方案
            val (finalWidth, finalHeight) = if (widthBasedHeight <= heightSize) {
                widthSize to widthBasedHeight
            } else {
                heightBasedWidth to heightSize
            }

            setMeasuredDimension(finalWidth, finalHeight)
        }
    }
}
