// SPDX-FileCopyrightText: Copyright 2026 Eden Emulator Project
// SPDX-License-Identifier: GPL-3.0-or-later

package org.yuzu.yuzu_emu.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * ThemeManager manages loading and caching of custom button themes from ZIP files.
 * It supports loading themes from both app assets (default.zip) and user-selected external ZIP files.
 */
class ThemeManager private constructor() {

    private var currentThemePath: String? = null
    private var currentThemeUri: Uri? = null
    private val bitmapCache: MutableMap<String, Bitmap> = mutableMapOf()
    private var isUsingCustomTheme = false

    companion object {
        const val PREF_CUSTOM_THEME_URI = "custom_theme_uri"
        const val PREF_THEME_BACKGROUND_ENABLED = "theme_background_enabled"
        const val DEFAULT_THEME_NAME = "default"
        const val BACKGROUND_IMAGE_NAME = "background.png"
        private const val THEME_CACHE_DIR = "theme_cache"

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager().also { instance = it }
            }
        }
    }

    /**
     * Initialize the theme manager with the current context.
     * Call this in Application class or when the app starts.
     */
    fun initialize(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val savedUri = prefs.getString(PREF_CUSTOM_THEME_URI, null)
        if (savedUri != null) {
            currentThemeUri = Uri.parse(savedUri)
            isUsingCustomTheme = true
        }
        prepareThemeCache(context)
    }

    /**
     * Prepare the theme cache directory
     */
    private fun prepareThemeCache(context: Context) {
        val cacheDir = File(context.cacheDir, THEME_CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Get the cache directory for theme files
     */
    private fun getThemeCacheDir(context: Context): File {
        return File(context.cacheDir, THEME_CACHE_DIR)
    }

    /**
     * Load a bitmap from the current theme.
     * First tries to load from custom theme, then falls back to default theme.
     *
     * @param context The context
     * @param assetName The name of the asset file (without path)
     * @return The loaded Bitmap or null if not found
     */
    fun getBitmap(context: Context, assetName: String): Bitmap? {
        // Check cache first
        val cacheKey = "${currentThemePath ?: DEFAULT_THEME_NAME}_$assetName"
        bitmapCache[cacheKey]?.let { return it }

        val bitmap = try {
            if (isUsingCustomTheme && currentThemePath != null) {
                loadFromCustomTheme(context, assetName)
            } else {
                loadFromDefaultTheme(context, assetName)
            }
        } catch (e: Exception) {
            Log.error("[ThemeManager] Failed to load bitmap $assetName: ${e.message}")
            // Fallback to default theme
            try {
                loadFromDefaultTheme(context, assetName)
            } catch (fallbackError: Exception) {
                Log.error("[ThemeManager] Fallback also failed: ${fallbackError.message}")
                null
            }
        }

        bitmap?.let {
            bitmapCache[cacheKey] = it
        }

        return bitmap
    }

    /**
     * Load a bitmap from the default theme in assets
     */
    private fun loadFromDefaultTheme(context: Context, assetName: String): Bitmap? {
        return try {
            context.assets.open("default.zip").use { zipInputStream ->
                val tempFile = File(getThemeCacheDir(context), "default_temp.zip")
                tempFile.outputStream().use { output ->
                    zipInputStream.copyTo(output)
                }
                
                ZipFile(tempFile).use { zipFile ->
                    val entry = zipFile.getEntry(assetName)
                    if (entry != null) {
                        zipFile.getInputStream(entry).use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                    } else {
                        Log.warning("[ThemeManager] Asset $assetName not found in default.zip")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("[ThemeManager] Error loading from default theme: ${e.message}")
            null
        }
    }

    /**
     * Load a bitmap from the custom theme ZIP file
     */
    private fun loadFromCustomTheme(context: Context, assetName: String): Bitmap? {
        val themePath = currentThemePath ?: return null
        
        return try {
            ZipFile(themePath).use { zipFile ->
                val entry = zipFile.getEntry(assetName)
                if (entry != null) {
                    zipFile.getInputStream(entry).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } else {
                    Log.warning("[ThemeManager] Asset $assetName not found in custom theme")
                    null
                }
            }
        } catch (e: Exception) {
            Log.error("[ThemeManager] Error loading from custom theme: ${e.message}")
            null
        }
    }

    /**
     * Load a bitmap from a URI (for newly selected themes before saving)
     */
    suspend fun loadBitmapFromUri(context: Context, uri: Uri, assetName: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Copy to temp file for ZipFile access
                val tempFile = File(getThemeCacheDir(context), "temp_theme.zip")
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                
                ZipFile(tempFile).use { zipFile ->
                    val entry = zipFile.getEntry(assetName)
                    if (entry != null) {
                        zipFile.getInputStream(entry).use { zipInputStream ->
                            BitmapFactory.decodeStream(zipInputStream)
                        }
                    } else {
                        Log.warning("[ThemeManager] Asset $assetName not found in selected ZIP")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("[ThemeManager] Error loading from URI: ${e.message}")
            null
        }
    }

    /**
     * Set a custom theme from a content URI.
     * This copies the ZIP file to internal storage and extracts needed assets.
     */
    suspend fun setCustomTheme(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // Validate the ZIP file first
            val tempFile = File(getThemeCacheDir(context), "temp_validate.zip")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
            } ?: return@withContext false

            // Verify it's a valid ZIP and check for required files
            val requiredFiles = listOf(
                "background.png",
                "facebutton_a.png",
                "facebutton_b.png",
                "facebutton_x.png",
                "facebutton_y.png"
            )

            try {
                ZipFile(tempFile).use { zipFile ->
                    val entries = zipFile.entries().asSequence().map { it.name }.toSet()
                    val missing = requiredFiles.filter { it !in entries }
                    if (missing.isNotEmpty()) {
                        Log.warning("[ThemeManager] Missing required files: $missing")
                        // Continue anyway, some files might be optional
                    }
                }
            } catch (e: Exception) {
                Log.error("[ThemeManager] Invalid ZIP file: ${e.message}")
                return@withContext false
            }

            // Copy to permanent location
            val themeFile = File(getThemeCacheDir(context), "custom_theme.zip")
            themeFile.delete()
            tempFile.renameTo(themeFile)

            // Update state
            currentThemePath = themeFile.absolutePath
            currentThemeUri = uri
            isUsingCustomTheme = true

            // Clear cache
            clearCache()

            // Save preference
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putString(PREF_CUSTOM_THEME_URI, uri.toString()).apply()

            Log.info("[ThemeManager] Custom theme set successfully")
            true
        } catch (e: Exception) {
            Log.error("[ThemeManager] Failed to set custom theme: ${e.message}")
            false
        }
    }

    /**
     * Clear the custom theme and revert to default
     */
    fun clearCustomTheme(context: Context) {
        currentThemePath = null
        currentThemeUri = null
        isUsingCustomTheme = false

        // Clear cached files
        val themeFile = File(getThemeCacheDir(context), "custom_theme.zip")
        if (themeFile.exists()) {
            themeFile.delete()
        }

        // Clear cache
        clearCache()

        // Clear preference
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().remove(PREF_CUSTOM_THEME_URI).apply()

        Log.info("[ThemeManager] Custom theme cleared, using default")
    }

    /**
     * Clear the bitmap cache
     */
    private fun clearCache() {
        bitmapCache.values.forEach { it.recycle() }
        bitmapCache.clear()
    }

    /**
     * Check if currently using a custom theme
     */
    fun isUsingCustomTheme(): Boolean = isUsingCustomTheme

    /**
     * Get the current theme name for display
     */
    fun getCurrentThemeName(): String {
        return if (isUsingCustomTheme) {
            currentThemeUri?.lastPathSegment?.removeSuffix(".zip") ?: "Custom"
        } else {
            DEFAULT_THEME_NAME
        }
    }

    /**
     * Check if a specific asset exists in the current theme
     */
    fun hasAsset(context: Context, assetName: String): Boolean {
        return getBitmap(context, assetName) != null
    }

    /**
     * Reload theme (call after theme change)
     */
    fun reloadTheme(context: Context) {
        clearCache()
        initialize(context)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        clearCache()
        instance = null
    }

    /**
     * Get the custom theme path (for ViewModel compatibility)
     */
    fun getCustomThemePath(): String? = currentThemePath

    /**
     * Load custom theme from URI (for ViewModel compatibility)
     * Uses blocking call - should be called from IO dispatcher
     */
    fun loadCustomTheme(context: Context, uri: Uri): Boolean {
        return try {
            // Use blocking approach for ViewModel compatibility
            val tempFile = File(getThemeCacheDir(context), "temp_theme.zip")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
            } ?: return false

            // Copy to permanent location
            val themeFile = File(getThemeCacheDir(context), "custom_theme.zip")
            themeFile.delete()
            tempFile.renameTo(themeFile)

            // Update state
            currentThemePath = themeFile.absolutePath
            currentThemeUri = uri
            isUsingCustomTheme = true

            // Clear cache
            clearCache()

            // Save preference
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putString(PREF_CUSTOM_THEME_URI, uri.toString()).apply()

            Log.info("[ThemeManager] Custom theme loaded successfully")
            true
        } catch (e: Exception) {
            Log.error("[ThemeManager] Failed to load custom theme: ${e.message}")
            false
        }
    }

    /**
     * Reset to default theme (for ViewModel compatibility)
     */
    fun resetToDefault(context: Context) {
        clearCustomTheme(context)
    }

    /**
     * Get the background image bitmap
     * @param context The context
     * @return The background Bitmap or null if not found
     */
    fun getBackgroundBitmap(context: Context): Bitmap? {
        return getBitmap(context, BACKGROUND_IMAGE_NAME)
    }

    /**
     * Check if background image should be shown
     * @param context The context
     * @return true if background is enabled and available
     */
    fun shouldShowBackground(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREF_THEME_BACKGROUND_ENABLED, true) && getBackgroundBitmap(context) != null
    }

    /**
     * Set background visibility preference
     */
    fun setBackgroundEnabled(context: Context, enabled: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(PREF_THEME_BACKGROUND_ENABLED, enabled).apply()
    }
}
