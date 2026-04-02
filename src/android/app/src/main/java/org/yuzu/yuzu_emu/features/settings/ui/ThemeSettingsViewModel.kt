package org.yuzu.yuzu_emu.features.settings.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.yuzu.yuzu_emu.R
import org.yuzu.yuzu_emu.features.settings.model.view.SettingsItem
import org.yuzu.yuzu_emu.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThemeSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _items = MutableLiveData<List<SettingsItem>>()
    val items: LiveData<List<SettingsItem>> = _items

    private val _currentThemeName = MutableLiveData<String>()
    val currentThemeName: LiveData<String> = _currentThemeName

    init {
        loadSettings()
    }

    fun refresh() {
        loadSettings()
    }

    private fun loadSettings() {
        val settingsList = mutableListOf<SettingsItem>()

        // Current Theme Section
        settingsList.add(
            SettingsItem(
                type = SettingsItem.ItemType.INFORMATION,
                titleRes = R.string.current_theme,
                summary = _currentThemeName.value ?: "",
                key = "current_theme"
            )
        )

        // Select Custom Theme
        settingsList.add(
            SettingsItem(
                type = SettingsItem.ItemType.RUNNABLE,
                titleRes = R.string.select_custom_theme,
                summaryRes = R.string.select_custom_theme_description,
                key = "select_custom_theme"
            )
        )

        // Reset to Default
        settingsList.add(
            SettingsItem(
                type = SettingsItem.ItemType.RUNNABLE,
                titleRes = R.string.reset_to_default_theme,
                summaryRes = R.string.reset_to_default_theme_description,
                key = "reset_to_default_theme"
            )
        )

        _items.value = settingsList
    }

    fun setCurrentThemeName(name: String) {
        _currentThemeName.value = name
        loadSettings()
    }

    suspend fun applyCustomTheme(context: android.content.Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val themeManager = ThemeManager.getInstance(context)
                themeManager.loadCustomTheme(context, uri)
                setCurrentThemeName(
                    context.getString(R.string.current_theme_custom, Uri.parse(uri.toString()).lastPathSegment ?: "Custom")
                )
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun resetToDefault(context: android.content.Context) {
        val themeManager = ThemeManager.getInstance(context)
        themeManager.resetToDefault(context)
        setCurrentThemeName(context.getString(R.string.current_theme_default))
    }
}
