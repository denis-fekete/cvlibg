package com.fekete.bangdemo.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import com.fekete.bangdemo.MyApp
import com.fekete.bangdemo.data.Language
import com.fekete.bangdemo.data.uiLabel
import com.fekete.cvlibg.ui.ResizableSpinnerAdapter
import com.fekete.cvlibg.detection.detectors.DetectorRegistry
import com.fekete.bangdemo.databinding.FragmentSettingsBinding

/**
 * Fragment containing UI elements for interacting with [settingsService].
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(
    FragmentSettingsBinding::inflate
) {
    private val settingsService by lazy {
        (requireContext().applicationContext as MyApp).settingsService
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val detectorRegistryModels = DetectorRegistry.getModelNames()

        binding.realtimeModelSpinner.setup(
            detectorRegistryModels,
            settingsService.data.realtimeModel,
            settingsService.data.fontSize.toFloat()
        ) { selected: Int ->
            settingsService.data.realtimeModel = detectorRegistryModels[selected]
            settingsService.save()
        }

        binding.preciseModelSpinner.setup(
            detectorRegistryModels,
            settingsService.data.precisionModel,
            settingsService.data.fontSize.toFloat()
        ) { selected: Int ->
            settingsService.data.precisionModel = detectorRegistryModels[selected]
            settingsService.save()
        }

        val languages = Language.entries

        binding.languageSpinner.setup(
            languages.map { it.uiLabel() },
            settingsService.data.language.uiLabel(),
            settingsService.data.fontSize.toFloat()
        ) { selected: Int ->
            settingsService.data.language = languages[selected]
            settingsService.save()
        }

        setupPerformanceMonitorSwitch()

        setupFontPicker()
    }

    /**
     * Setup performance logging and its verbose option switches.
     */
    private fun setupPerformanceMonitorSwitch() {

        binding.showPerformanceSwitch.isChecked = settingsService.data.showMetrics

        binding.showPerformanceSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsService.data.showMetrics = isChecked
            settingsService.save()
        }

        binding.verbosePerformanceSwitch.isChecked = settingsService.data.verboseMetrics

        binding.verbosePerformanceSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsService.data.verboseMetrics = isChecked
            settingsService.save()
        }
    }

    /**
     * Setup NumberPicker for font size.
     */
    private fun setupFontPicker() {
        binding.fontSizePicker.minValue = 10
        binding.fontSizePicker.maxValue = 30
        binding.fontSizePicker.value = settingsService.data.fontSize
        updateFontSize()

        binding.fontSizePicker.setOnValueChangedListener { _, _, newValue ->
            settingsService.data.fontSize = newValue
            settingsService.save()
            updateFontSize()
        }
    }

    private fun updateFontSize() {
        val textElements = arrayOf(
            binding.textFontSize,
            binding.textShowPerformance,
            binding.textVerbosePerformance,
            binding.textChooseLanguage,
            binding.textChooseRealtimeModel,
            binding.textChoosePrecisionModel,
            binding.textChooseLanguage,
            binding.metricsSeparator
        )
        for (element in textElements) {
            element.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.data.fontSize.toFloat())
        }

        val spinnerElement = arrayOf(
            binding.realtimeModelSpinner,
            binding.preciseModelSpinner,
            binding.languageSpinner
        )

        for (element in spinnerElement) {
            val adapter = element.adapter
            if (adapter is ResizableSpinnerAdapter) {
                adapter.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.data.fontSize.toFloat())
            }
        }
    }
}