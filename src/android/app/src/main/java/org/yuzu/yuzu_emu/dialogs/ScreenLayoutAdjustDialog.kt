// SPDX-FileCopyrightText: Copyright 2026 Eden Emulator Project
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.yuzu.yuzu_emu.R
import org.yuzu.yuzu_emu.databinding.DialogScreenLayoutAdjustBinding
import org.yuzu.yuzu_emu.utils.ScreenLayoutManager
import org.yuzu.yuzu_emu.utils.ScreenLayoutManager.Preset
import org.yuzu.yuzu_emu.views.GameScreenLayoutManager

/**
 * 游戏屏幕布局调整对话框
 * 提供滑动条和预设来调整游戏屏幕的四边边距
 */
class ScreenLayoutAdjustDialog : DialogFragment() {
    
    private var _binding: DialogScreenLayoutAdjustBinding? = null
    private val binding get() = _binding!!
    
    private var layoutManager: GameScreenLayoutManager? = null
    private var onDismissListener: (() -> Unit)? = null
    private var onShowListener: (() -> Unit)? = null
    
    companion object {
        const val TAG = "ScreenLayoutAdjustDialog"
        
        fun newInstance(): ScreenLayoutAdjustDialog {
            return ScreenLayoutAdjustDialog()
        }
    }
    
    fun setLayoutManager(manager: GameScreenLayoutManager) {
        layoutManager = manager
    }
    
    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }
    
    fun setOnShowListener(listener: () -> Unit) {
        onShowListener = listener
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogScreenLayoutAdjustBinding.inflate(layoutInflater)
        
        setupUI()
        loadCurrentSettings()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.screen_layout_adjust)
            .setView(binding.root)
            .create()
    }
    
    override fun onStart() {
        super.onStart()
        
        // 设置对话框背景透明
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // 设置对话框宽度为屏幕宽度的 90%，高度自适应
            val params = attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.y = resources.displayMetrics.heightPixels / 10 // 距顶部 10%
            attributes = params
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 通知主界面隐藏 Drawer
        onShowListener?.invoke()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupUI() {
        // 启用/禁用调整模式开关
        binding.switchEnableResize.setOnCheckedChangeListener { _, isChecked ->
            binding.textResizeHint.visibility = if (isChecked) View.VISIBLE else View.GONE
            layoutManager?.isAdjustModeEnabled = isChecked
        }
        
        // 左边距滑动条
        binding.sliderLeftMargin.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.textLeftValue.text = "${value.toInt()}dp"
                layoutManager?.setMargins(
                    value.toInt(),
                    binding.sliderTopMargin.value.toInt(),
                    binding.sliderRightMargin.value.toInt(),
                    binding.sliderBottomMargin.value.toInt()
                )
            }
        }
        
        // 上边距滑动条
        binding.sliderTopMargin.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.textTopValue.text = "${value.toInt()}dp"
                layoutManager?.setMargins(
                    binding.sliderLeftMargin.value.toInt(),
                    value.toInt(),
                    binding.sliderRightMargin.value.toInt(),
                    binding.sliderBottomMargin.value.toInt()
                )
            }
        }
        
        // 右边距滑动条
        binding.sliderRightMargin.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.textRightValue.text = "${value.toInt()}dp"
                layoutManager?.setMargins(
                    binding.sliderLeftMargin.value.toInt(),
                    binding.sliderTopMargin.value.toInt(),
                    value.toInt(),
                    binding.sliderBottomMargin.value.toInt()
                )
            }
        }
        
        // 下边距滑动条
        binding.sliderBottomMargin.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.textBottomValue.text = "${value.toInt()}dp"
                layoutManager?.setMargins(
                    binding.sliderLeftMargin.value.toInt(),
                    binding.sliderTopMargin.value.toInt(),
                    binding.sliderRightMargin.value.toInt(),
                    value.toInt()
                )
            }
        }
        
        // 预设按钮
        binding.chipDefault.setOnClickListener { applyPreset(Preset.DEFAULT) }
        binding.chipCompact.setOnClickListener { applyPreset(Preset.COMPACT_LEFT) }
        binding.chipWidescreen.setOnClickListener { applyPreset(Preset.WIDESCREEN) }
        binding.chipFullscreen.setOnClickListener { applyPreset(Preset.FULLSCREEN) }
        
        // 重置按钮
        binding.buttonReset.setOnClickListener {
            layoutManager?.resetMargins()
            layoutManager?.isAdjustModeEnabled = false
            binding.switchEnableResize.isChecked = false
            resetSliders()
        }
        
        // 完成按钮
        binding.buttonDone.setOnClickListener {
            saveSettings()
            dismiss()
        }
    }
    
    private fun loadCurrentSettings() {
        val config = ScreenLayoutManager.loadLayoutConfig(requireContext())
        
        binding.switchEnableResize.isChecked = config.enabled
        binding.textResizeHint.visibility = if (config.enabled) View.VISIBLE else View.GONE
        
        binding.sliderLeftMargin.value = config.left.toFloat()
        binding.sliderTopMargin.value = config.top.toFloat()
        binding.sliderRightMargin.value = config.right.toFloat()
        binding.sliderBottomMargin.value = config.bottom.toFloat()
        
        binding.textLeftValue.text = "${config.left}dp"
        binding.textTopValue.text = "${config.top}dp"
        binding.textRightValue.text = "${config.right}dp"
        binding.textBottomValue.text = "${config.bottom}dp"
        
        // 应用到布局管理器
        layoutManager?.apply {
            loadLayoutConfig(config.toLayoutMargins())
            isAdjustModeEnabled = config.enabled
        }
    }
    
    private fun applyPreset(preset: Preset) {
        binding.sliderLeftMargin.value = preset.left.toFloat()
        binding.sliderTopMargin.value = preset.top.toFloat()
        binding.sliderRightMargin.value = preset.right.toFloat()
        binding.sliderBottomMargin.value = preset.bottom.toFloat()
        
        binding.textLeftValue.text = "${preset.left}dp"
        binding.textTopValue.text = "${preset.top}dp"
        binding.textRightValue.text = "${preset.right}dp"
        binding.textBottomValue.text = "${preset.bottom}dp"
        
        layoutManager?.setMargins(preset.left, preset.top, preset.right, preset.bottom)
    }
    
    private fun resetSliders() {
        binding.sliderLeftMargin.value = 0f
        binding.sliderTopMargin.value = 0f
        binding.sliderRightMargin.value = 0f
        binding.sliderBottomMargin.value = 0f
        
        binding.textLeftValue.text = "0dp"
        binding.textTopValue.text = "0dp"
        binding.textRightValue.text = "0dp"
        binding.textBottomValue.text = "0dp"
    }
    
    private fun saveSettings() {
        val margins = layoutManager?.getLayoutConfig() ?: return
        
        ScreenLayoutManager.saveLayoutConfig(
            requireContext(),
            margins,
            binding.switchEnableResize.isChecked
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // 保存设置
        if (_binding != null) {
            saveSettings()
        }
        _binding = null
        onDismissListener?.invoke()
    }
}
