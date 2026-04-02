package org.yuzu.yuzu_emu.features.settings.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.yuzu.yuzu_emu.R
import org.yuzu.yuzu_emu.databinding.FragmentThemeSettingsBinding
import org.yuzu.yuzu_emu.features.settings.model.SettingsItem
import org.yuzu.yuzu_emu.features.settings.ui.adapter.SettingsAdapter
import org.yuzu.yuzu_emu.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThemeSettingsFragment : Fragment() {

    private var _binding: FragmentThemeSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SettingsAdapter
    private val viewModel: ThemeSettingsViewModel by activityViewModels()

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedTheme(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThemeSettingsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupToolbar()
        observeViewModel()
        loadCurrentTheme()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = SettingsAdapter(
            onItemClick = { item ->
                when (item.type) {
                    SettingsItem.ItemType.RUNNABLE -> handleRunnableClick(item)
                    SettingsItem.ItemType.SWITCH -> handleSwitchClick(item)
                }
            },
            onResume = { viewModel.refresh() }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ThemeSettingsFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        viewModel.currentThemeName.observe(viewLifecycleOwner) { themeName ->
            updateCurrentThemeDisplay(themeName)
        }
    }

    private fun loadCurrentTheme() {
        val themeManager = ThemeManager.getInstance(requireContext())
        val customPath = themeManager.getCustomThemePath()
        val themeName = if (customPath.isNullOrEmpty()) {
            getString(R.string.current_theme_default)
        } else {
            getString(R.string.current_theme_custom, Uri.parse(customPath).lastPathSegment ?: "Custom")
        }
        viewModel.setCurrentThemeName(themeName)
    }

    private fun updateCurrentThemeDisplay(themeName: String) {
        binding.currentThemeText.text = themeName
    }

    private fun handleRunnableClick(item: SettingsItem) {
        when (item.key) {
            "select_custom_theme" -> openFilePicker()
            "reset_to_default_theme" -> confirmResetToDefault()
        }
    }

    private fun handleSwitchClick(item: SettingsItem) {
        // Handle switch items if needed in the future
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        openDocumentLauncher.launch(intent)
    }

    private fun handleSelectedTheme(uri: Uri) {
        // Take persistable permission
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Permission might already be granted or not available
        }

        lifecycleScope.launch {
            val result = viewModel.applyCustomTheme(requireContext(), uri)

            if (result) {
                Toast.makeText(
                    requireContext(),
                    R.string.custom_theme_applied,
                    Toast.LENGTH_SHORT
                ).show()
                loadCurrentTheme()
            } else {
                showThemeLoadError()
            }
        }
    }

    private fun confirmResetToDefault() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset_to_default_theme)
            .setMessage(R.string.reset_to_default_theme_description)
            .setPositiveButton(R.string.confirm) { _, _ ->
                resetToDefaultTheme()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun resetToDefaultTheme() {
        viewModel.resetToDefault(requireContext())
        Toast.makeText(
            requireContext(),
            R.string.theme_reset_to_default,
            Toast.LENGTH_SHORT
        ).show()
        loadCurrentTheme()
    }

    private fun showThemeLoadError() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.theme_load_error)
            .setMessage(R.string.theme_load_error_description)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
