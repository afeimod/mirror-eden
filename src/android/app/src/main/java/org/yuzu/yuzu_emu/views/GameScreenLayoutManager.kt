// SPDX-FileCopyrightText: Copyright 2026 Eden Emulator Project
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 高级游戏屏幕布局调整器
 * 支持：
 * - 四边拖动调整大小（无限制）
 * - 整体拖动定位
 * - 边缘手柄可视化
 * - 边缘大小预设
 */
class GameScreenLayoutManager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    // 最小尺寸限制
    private val minWidth = 80  // dp
    private val minHeight = 60 // dp
    
    // 触摸容差
    private val edgeTolerance = 40 // dp
    private val cornerTolerance = 56 // dp
    private val edgeTolerancePx: Float
    private val cornerTolerancePx: Float
    
    // 是否启用调整模式
    var isAdjustModeEnabled = false
        set(value) {
            field = value
            // 使用非常透明的背景
            setBackgroundColor(if (value) Color.argb(10, 0, 0, 0) else Color.TRANSPARENT)
            invalidate()
        }
    
    // 是否显示调整手柄
    var showHandles = true
    
    // 边缘和边距
    private var leftMargin = 0
    private var topMargin = 0
    private var rightMargin = 0
    private var bottomMargin = 0
    
    // 触摸区域检测
    private enum class DragRegion {
        NONE,
        LEFT_EDGE, RIGHT_EDGE, TOP_EDGE, BOTTOM_EDGE,
        TOP_LEFT_CORNER, TOP_RIGHT_CORNER, BOTTOM_LEFT_CORNER, BOTTOM_RIGHT_CORNER,
        CENTER
    }
    
    private var currentDragRegion = DragRegion.NONE
    
    // 上次触摸位置
    private var lastX = 0f
    private var lastY = 0f
    
    // 手柄绘制 - 使用更透明的样式
    private val handlePaint = Paint().apply {
        color = Color.argb(100, 33, 150, 243) // 非常透明
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val handleStrokePaint = Paint().apply {
        color = Color.argb(150, 255, 255, 255) // 半透明白色
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    private val edgePaint = Paint().apply {
        color = Color.argb(80, 33, 150, 243) // 非常透明的蓝色边框
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    private val handleRadius = 12f
    
    // 回调
    interface OnLayoutChangeListener {
        fun onMarginsChanged(left: Int, top: Int, right: Int, bottom: Int)
        fun onPositionChanged(x: Int, y: Int)
    }
    
    var listener: OnLayoutChangeListener? = null
    
    init {
        val density = context.resources.displayMetrics.density
        edgeTolerancePx = edgeTolerance * density
        cornerTolerancePx = cornerTolerance * density
        
        // 设置默认背景（调整模式下显示）
        setBackgroundColor(Color.TRANSPARENT)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isAdjustModeEnabled || !showHandles) return
        
        // 绘制边缘
        canvas.drawRect(2f, 2f, width - 2f, height - 2f, edgePaint)
        
        // 绘制调整手柄
        val handleOffset = handleRadius * 1.5f
        
        // 边缘手柄
        // 左边缘
        drawHandle(canvas, 0f, height / 2f)
        // 右边缘
        drawHandle(canvas, width.toFloat(), height / 2f)
        // 上边缘
        drawHandle(canvas, width / 2f, 0f)
        // 下边缘
        drawHandle(canvas, width / 2f, height.toFloat())
        
        // 角落手柄
        drawHandle(canvas, 0f, 0f)
        drawHandle(canvas, width.toFloat(), 0f)
        drawHandle(canvas, 0f, height.toFloat())
        drawHandle(canvas, width.toFloat(), height.toFloat())
    }
    
    private fun drawHandle(canvas: Canvas, cx: Float, cy: Float) {
        val radius = handleRadius * 1.8f
        canvas.drawCircle(cx, cy, radius, handlePaint)
        canvas.drawCircle(cx, cy, radius, handleStrokePaint)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isAdjustModeEnabled) {
            return super.onTouchEvent(event)
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
                currentDragRegion = detectRegion(event.x, event.y)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - lastX).toInt()
                val deltaY = (event.rawY - lastY).toInt()
                
                applyDrag(deltaX, deltaY)
                
                lastX = event.rawX
                lastY = event.rawY
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentDragRegion = DragRegion.NONE
                listener?.onMarginsChanged(leftMargin, topMargin, rightMargin, bottomMargin)
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun detectRegion(x: Float, y: Float): DragRegion {
        val isLeft = x < edgeTolerancePx
        val isRight = x > width - edgeTolerancePx
        val isTop = y < edgeTolerancePx
        val isBottom = y > height - edgeTolerancePx
        
        val isTopLeft = x < cornerTolerancePx && y < cornerTolerancePx
        val isTopRight = x > width - cornerTolerancePx && y < cornerTolerancePx
        val isBottomLeft = x < cornerTolerancePx && y > height - cornerTolerancePx
        val isBottomRight = x > width - cornerTolerancePx && y > height - cornerTolerancePx
        
        return when {
            isTopLeft -> DragRegion.TOP_LEFT_CORNER
            isTopRight -> DragRegion.TOP_RIGHT_CORNER
            isBottomLeft -> DragRegion.BOTTOM_LEFT_CORNER
            isBottomRight -> DragRegion.BOTTOM_RIGHT_CORNER
            isLeft -> DragRegion.LEFT_EDGE
            isRight -> DragRegion.RIGHT_EDGE
            isTop -> DragRegion.TOP_EDGE
            isBottom -> DragRegion.BOTTOM_EDGE
            else -> DragRegion.CENTER
        }
    }
    
    private fun applyDrag(deltaX: Int, deltaY: Int) {
        val density = context.resources.displayMetrics.density
        val minWidthPx = (minWidth * density).toInt()
        val minHeightPx = (minHeight * density).toInt()
        
        when (currentDragRegion) {
            DragRegion.LEFT_EDGE -> {
                leftMargin = (leftMargin + deltaX).coerceAtLeast(0)
                val maxMargin = width - minWidthPx - rightMargin
                leftMargin = leftMargin.coerceAtMost(maxMargin)
            }
            
            DragRegion.RIGHT_EDGE -> {
                rightMargin = (rightMargin - deltaX).coerceAtLeast(0)
                val maxMargin = width - minWidthPx - leftMargin
                rightMargin = rightMargin.coerceAtMost(maxMargin)
            }
            
            DragRegion.TOP_EDGE -> {
                topMargin = (topMargin + deltaY).coerceAtLeast(0)
                val maxMargin = height - minHeightPx - bottomMargin
                topMargin = topMargin.coerceAtMost(maxMargin)
            }
            
            DragRegion.BOTTOM_EDGE -> {
                bottomMargin = (bottomMargin - deltaY).coerceAtLeast(0)
                val maxMargin = height - minHeightPx - topMargin
                bottomMargin = bottomMargin.coerceAtMost(maxMargin)
            }
            
            DragRegion.TOP_LEFT_CORNER -> {
                applyDrag(deltaX, 0)
                applyDrag(0, deltaY)
            }
            
            DragRegion.TOP_RIGHT_CORNER -> {
                applyDrag(0, deltaY)
                val oldRight = rightMargin
                rightMargin = (rightMargin - deltaX).coerceAtLeast(0)
                val maxMargin = width - minWidthPx - leftMargin
                rightMargin = rightMargin.coerceAtMost(maxMargin)
            }
            
            DragRegion.BOTTOM_LEFT_CORNER -> {
                val oldLeft = leftMargin
                leftMargin = (leftMargin + deltaX).coerceAtLeast(0)
                val maxMargin = width - minWidthPx - rightMargin
                leftMargin = leftMargin.coerceAtMost(maxMargin)
                // 修正：如果左边距变化了，抵消X方向的影响
                if (leftMargin != oldLeft) {
                    // 已在上面处理
                }
                bottomMargin = (bottomMargin - deltaY).coerceAtLeast(0)
                val maxMarginY = height - minHeightPx - topMargin
                bottomMargin = bottomMargin.coerceAtMost(maxMarginY)
            }
            
            DragRegion.BOTTOM_RIGHT_CORNER -> {
                rightMargin = (rightMargin - deltaX).coerceAtLeast(0)
                val maxMarginX = width - minWidthPx - leftMargin
                rightMargin = rightMargin.coerceAtMost(maxMarginX)
                
                bottomMargin = (bottomMargin - deltaY).coerceAtLeast(0)
                val maxMarginY = height - minHeightPx - topMargin
                bottomMargin = bottomMargin.coerceAtMost(maxMarginY)
            }
            
            DragRegion.CENTER -> {
                // 整体移动（转换为边距变化）
                listener?.onPositionChanged(deltaX, deltaY)
            }
            
            DragRegion.NONE -> {}
        }
        
        applyMargins()
        invalidate()
    }
    
    private fun applyMargins() {
        val params = layoutParams as? MarginLayoutParams ?: return
        params.leftMargin = leftMargin
        params.topMargin = topMargin
        params.rightMargin = rightMargin
        params.bottomMargin = bottomMargin
        layoutParams = params
    }
    
    /**
     * 设置边距
     */
    fun setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        leftMargin = left
        topMargin = top
        rightMargin = right
        bottomMargin = bottom
        applyMargins()
        invalidate()
    }
    
    /**
     * 获取当前边距
     */
    fun getMargins(): IntArray {
        return intArrayOf(leftMargin, topMargin, rightMargin, bottomMargin)
    }
    
    /**
     * 重置边距
     */
    fun resetMargins() {
        leftMargin = 0
        topMargin = 0
        rightMargin = 0
        bottomMargin = 0
        applyMargins()
        invalidate()
    }
    
    /**
     * 增加到边缘
     */
    fun addToEdge(edge: Edge, amount: Int) {
        when (edge) {
            Edge.LEFT -> leftMargin = (leftMargin + amount).coerceAtLeast(0)
            Edge.TOP -> topMargin = (topMargin + amount).coerceAtLeast(0)
            Edge.RIGHT -> rightMargin = (rightMargin + amount).coerceAtLeast(0)
            Edge.BOTTOM -> bottomMargin = (bottomMargin + amount).coerceAtLeast(0)
        }
        applyMargins()
        invalidate()
    }
    
    /**
     * 从边缘减少
     */
    fun reduceFromEdge(edge: Edge, amount: Int) {
        when (edge) {
            Edge.LEFT -> leftMargin = (leftMargin - amount).coerceAtLeast(0)
            Edge.TOP -> topMargin = (topMargin - amount).coerceAtLeast(0)
            Edge.RIGHT -> rightMargin = (rightMargin - amount).coerceAtLeast(0)
            Edge.BOTTOM -> bottomMargin = (bottomMargin - amount).coerceAtLeast(0)
        }
        applyMargins()
        invalidate()
    }
    
    /**
     * 获取边距配置
     */
    fun getLayoutConfig(): LayoutMargins {
        return LayoutMargins(leftMargin, topMargin, rightMargin, bottomMargin)
    }
    
    /**
     * 加载边距配置
     */
    fun loadLayoutConfig(config: LayoutMargins) {
        leftMargin = config.left
        topMargin = config.top
        rightMargin = config.right
        bottomMargin = config.bottom
        applyMargins()
        invalidate()
    }
    
    enum class Edge {
        LEFT, TOP, RIGHT, BOTTOM
    }
    
    data class LayoutMargins(
        val left: Int = 0,
        val top: Int = 0,
        val right: Int = 0,
        val bottom: Int = 0
    )
}
