// SPDX-FileCopyrightText: Copyright 2026 Eden Emulator Project
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.yuzu.yuzu_emu.features.settings.model.Settings
import org.yuzu.yuzu_emu.views.GameScreenLayoutManager

/**
 * 管理游戏屏幕布局设置的保存和加载
 */
object ScreenLayoutManager {
    
    private const val PREF_SCREEN_LAYOUT_ENABLED = "screen_layout_adjust_enabled"
    private const val PREF_SCREEN_LAYOUT_LEFT = "screen_layout_margin_left"
    private const val PREF_SCREEN_LAYOUT_TOP = "screen_layout_margin_top"
    private const val PREF_SCREEN_LAYOUT_RIGHT = "screen_layout_margin_right"
    private const val PREF_SCREEN_LAYOUT_BOTTOM = "screen_layout_margin_bottom"
    
    /**
     * 保存布局配置
     */
    fun saveLayoutConfig(context: Context, config: GameScreenLayoutManager.LayoutMargins, enabled: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().apply {
            putBoolean(PREF_SCREEN_LAYOUT_ENABLED, enabled)
            putInt(PREF_SCREEN_LAYOUT_LEFT, config.left)
            putInt(PREF_SCREEN_LAYOUT_TOP, config.top)
            putInt(PREF_SCREEN_LAYOUT_RIGHT, config.right)
            putInt(PREF_SCREEN_LAYOUT_BOTTOM, config.bottom)
            apply()
        }
    }
    
    /**
     * 加载布局配置
     */
    fun loadLayoutConfig(context: Context): LayoutConfig {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return LayoutConfig(
            enabled = prefs.getBoolean(PREF_SCREEN_LAYOUT_ENABLED, false),
            left = prefs.getInt(PREF_SCREEN_LAYOUT_LEFT, 200),
            top = prefs.getInt(PREF_SCREEN_LAYOUT_TOP, 0),
            right = prefs.getInt(PREF_SCREEN_LAYOUT_RIGHT, 200),
            bottom = prefs.getInt(PREF_SCREEN_LAYOUT_BOTTOM, 0)
        )
    }
    
    /**
     * 重置布局设置为默认值
     */
    fun resetLayoutConfig(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().apply {
            putBoolean(PREF_SCREEN_LAYOUT_ENABLED, false)
            putInt(PREF_SCREEN_LAYOUT_LEFT, 0)
            putInt(PREF_SCREEN_LAYOUT_TOP, 0)
            putInt(PREF_SCREEN_LAYOUT_RIGHT, 0)
            putInt(PREF_SCREEN_LAYOUT_BOTTOM, 0)
            apply()
        }
    }
    
    /**
     * 布局配置数据类
     */
    data class LayoutConfig(
        val enabled: Boolean = false,
        val left: Int = 0,
        val top: Int = 0,
        val right: Int = 0,
        val bottom: Int = 0
    ) {
        fun toLayoutMargins(): GameScreenLayoutManager.LayoutMargins {
            return GameScreenLayoutManager.LayoutMargins(left, top, right, bottom)
        }
    }
    
    /**
     * 预设布局
     */
    enum class Preset(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        DEFAULT(0, 0, 0, 0),
        COMPACT_LEFT(100, 50, 0, 50),      // 左侧留白，适合横屏游戏
        COMPACT_RIGHT(0, 50, 100, 50),   // 右侧留白
        COMPACT_TOP(50, 100, 50, 0),      // 顶部留白
        COMPACT_BOTTOM(50, 0, 50, 100),   // 底部留白
        WIDESCREEN(0, 80, 0, 80),         // 上下留白，适合超宽屏
        FULLSCREEN(0, 0, 0, 0)           // 全屏无留白
    }
}
