// SPDX-FileCopyrightText: Copyright 2026 Eden Emulator Project
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * 自定义布局容器，支持游戏屏幕的四边拖动调整大小和整体拖动定位
 * 功能：
 * - 四边和四角可拖动调整大小
 * - 中心区域可拖动定位
 * - 无限制调整大小
 */
class ResizableGameContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    // 边缘调整的最小尺寸
    private val minWidth = 100
    private val minHeight = 100
    
    // 边缘调整区域宽度
    private val edgeTouchTolerance = 48 // dp
    private val edgeTouchTolerancePx: Float
    
    // 角落调整区域大小
    private val cornerTouchTolerance = 72 // dp
    private val cornerTouchTolerancePx: Float
    
    // 是否启用调整模式
    var isResizeEnabled = false
        set(value) {
            field = value
            invalidate()
        }
    
    // 边缘颜色
    private val edgePaint = Paint().apply {
        color = Color.argb(200, 0, 120, 215) // 蓝色边缘
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    // 调整手柄颜色
    private val handlePaint = Paint().apply {
        color = Color.argb(255, 0, 120, 215)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // 手柄半径
    private val handleRadius = 16f
    
    // 存储当前边缘调整状态
    private var edgeSize = floatArrayOf(0f, 0f, 0f, 0f) // left, top, right, bottom
    
    // 上次触摸位置
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    // 触摸区域类型
    private enum class TouchArea {
        NONE, LEFT, RIGHT, TOP, BOTTOM,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        CENTER
    }
    
    private var currentTouchArea = TouchArea.NONE
    
    // 回调接口
    interface OnResizeListener {
        fun onResize(left: Int, top: Int, right: Int, bottom: Int)
        fun onDrag(x: Int, y: Int)
    }
    
    var resizeListener: OnResizeListener? = null
    
    init {
        // 将dp转换为px
        val density = context.resources.displayMetrics.density
        edgeTouchTolerancePx = edgeTouchTolerance * density
        cornerTouchTolerancePx = cornerTouchTolerance * density
        
        // 设置背景为半透明黑色，便于显示调整边缘
        setBackgroundColor(Color.argb(0, 0, 0, 0))
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isResizeEnabled) return
        
        // 绘制边框
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), edgePaint)
        
        // 绘制调整手柄
        val handleRadiusAdjusted = handleRadius * 2
        
        // 四边中心手柄
        // 左边
        canvas.drawCircle(0f, height / 2f, handleRadiusAdjusted, handlePaint)
        // 右边
        canvas.drawCircle(width.toFloat(), height / 2f, handleRadiusAdjusted, handlePaint)
        // 上边
        canvas.drawCircle(width / 2f, 0f, handleRadiusAdjusted, handlePaint)
        // 下边
        canvas.drawCircle(width / 2f, height.toFloat(), handleRadiusAdjusted, handlePaint)
        
        // 四角手柄
        canvas.drawCircle(0f, 0f, handleRadiusAdjusted, handlePaint)
        canvas.drawCircle(width.toFloat(), 0f, handleRadiusAdjusted, handlePaint)
        canvas.drawCircle(0f, height.toFloat(), handleRadiusAdjusted, handlePaint)
        canvas.drawCircle(width.toFloat(), height.toFloat(), handleRadiusAdjusted, handlePaint)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isResizeEnabled) {
            return super.onTouchEvent(event)
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                currentTouchArea = detectTouchArea(event.x, event.y)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - lastTouchX).toInt()
                val deltaY = (event.rawY - lastTouchY).toInt()
                
                when (currentTouchArea) {
                    TouchArea.LEFT -> adjustLeft(deltaX)
                    TouchArea.RIGHT -> adjustRight(deltaX)
                    TouchArea.TOP -> adjustTop(deltaY)
                    TouchArea.BOTTOM -> adjustBottom(deltaY)
                    TouchArea.TOP_LEFT -> {
                        adjustLeft(deltaX)
                        adjustTop(deltaY)
                    }
                    TouchArea.TOP_RIGHT -> {
                        adjustRight(deltaX)
                        adjustTop(deltaY)
                    }
                    TouchArea.BOTTOM_LEFT -> {
                        adjustLeft(deltaX)
                        adjustBottom(deltaY)
                    }
                    TouchArea.BOTTOM_RIGHT -> {
                        adjustRight(deltaX)
                        adjustBottom(deltaY)
                    }
                    TouchArea.CENTER -> {
                        // 整体拖动
                        resizeListener?.onDrag(
                            (layoutParams as? MarginLayoutParams)?.leftMargin ?: 0 + deltaX,
                            (layoutParams as? MarginLayoutParams)?.topMargin ?: 0 + deltaY
                        )
                    }
                    TouchArea.NONE -> {}
                }
                
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                
                invalidate()
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentTouchArea = TouchArea.NONE
                // 通知调整完成
                resizeListener?.onResize(
                    edgeSize[0].toInt(),
                    edgeSize[1].toInt(),
                    edgeSize[2].toInt(),
                    edgeSize[3].toInt()
                )
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun detectTouchArea(x: Float, y: Float): TouchArea {
        val parent = parent as? View ?: return TouchArea.CENTER
        
        // 检测是否在边缘区域
        val isLeftEdge = x < edgeTouchTolerancePx
        val isRightEdge = x > width - edgeTouchTolerancePx
        val isTopEdge = y < edgeTouchTolerancePx
        val isBottomEdge = y > height - edgeTouchTolerancePx
        
        // 检测四角
        val isTopLeftCorner = x < cornerTouchTolerancePx && y < cornerTouchTolerancePx
        val isTopRightCorner = x > width - cornerTouchTolerancePx && y < cornerTouchTolerancePx
        val isBottomLeftCorner = x < cornerTouchTolerancePx && y > height - cornerTouchTolerancePx
        val isBottomRightCorner = x > width - cornerTouchTolerancePx && y > height - cornerTouchTolerancePx
        
        return when {
            isTopLeftCorner -> TouchArea.TOP_LEFT
            isTopRightCorner -> TouchArea.TOP_RIGHT
            isBottomLeftCorner -> TouchArea.BOTTOM_LEFT
            isBottomRightCorner -> TouchArea.BOTTOM_RIGHT
            isLeftEdge -> TouchArea.LEFT
            isRightEdge -> TouchArea.RIGHT
            isTopEdge -> TouchArea.TOP
            isBottomEdge -> TouchArea.BOTTOM
            else -> TouchArea.CENTER
        }
    }
    
    private fun adjustLeft(deltaX: Int) {
        val newLeft = (edgeSize[0] + deltaX).coerceIn(0f, (width - minWidth).toFloat())
        edgeSize[0] = newLeft
        updateMargins()
    }
    
    private fun adjustRight(deltaX: Int) {
        val newRight = (edgeSize[2] - deltaX).coerceIn(0f, (width - minWidth).toFloat())
        edgeSize[2] = newRight
        updateMargins()
    }
    
    private fun adjustTop(deltaY: Int) {
        val newTop = (edgeSize[1] + deltaY).coerceIn(0f, (height - minHeight).toFloat())
        edgeSize[1] = newTop
        updateMargins()
    }
    
    private fun adjustBottom(deltaY: Int) {
        val newBottom = (edgeSize[3] - deltaY).coerceIn(0f, (height - minHeight).toFloat())
        edgeSize[3] = newBottom
        updateMargins()
    }
    
    private fun updateMargins() {
        val params = layoutParams as? MarginLayoutParams ?: return
        
        params.leftMargin = edgeSize[0].toInt()
        params.topMargin = edgeSize[1].toInt()
        params.rightMargin = edgeSize[2].toInt()
        params.bottomMargin = edgeSize[3].toInt()
        
        layoutParams = params
    }
    
    /**
     * 设置边缘大小
     */
    fun setEdgeSize(left: Int, top: Int, right: Int, bottom: Int) {
        edgeSize = floatArrayOf(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        updateMargins()
        invalidate()
    }
    
    /**
     * 获取当前边缘大小
     */
    fun getEdgeSize(): IntArray {
        return intArrayOf(
            edgeSize[0].toInt(),
            edgeSize[1].toInt(),
            edgeSize[2].toInt(),
            edgeSize[3].toInt()
        )
    }
    
    /**
     * 重置边缘大小
     */
    fun resetEdgeSize() {
        edgeSize = floatArrayOf(0f, 0f, 0f, 0f)
        updateMargins()
        invalidate()
    }
    
    /**
     * 保存布局配置
     */
    fun saveLayoutConfig(): LayoutConfig {
        return LayoutConfig(
            leftMargin = edgeSize[0].toInt(),
            topMargin = edgeSize[1].toInt(),
            rightMargin = edgeSize[2].toInt(),
            bottomMargin = edgeSize[3].toInt()
        )
    }
    
    /**
     * 加载布局配置
     */
    fun loadLayoutConfig(config: LayoutConfig) {
        edgeSize = floatArrayOf(
            config.leftMargin.toFloat(),
            config.topMargin.toFloat(),
            config.rightMargin.toFloat(),
            config.bottomMargin.toFloat()
        )
        updateMargins()
        invalidate()
    }
    
    /**
     * 布局配置数据类
     */
    data class LayoutConfig(
        val leftMargin: Int = 0,
        val topMargin: Int = 0,
        val rightMargin: Int = 0,
        val bottomMargin: Int = 0
    )
}
