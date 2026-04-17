package cv.demoapps.bangdemo.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import cv.cbglib.ui.ResizableSpinnerAdapter
import cv.cbglib.detection.detectors.DetectorRegistry
import cv.cbglib.services.SettingsService
import cv.cbglib.ui.StringListSpinner
import cv.demoapps.bangdemo.MyApp
import cv.demoapps.bangdemo.R

class SettingsFragment : Fragment() {

    private val settingsService by lazy {
        (requireContext().applicationContext as MyApp).settingsService
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val detectorRegistryModels = DetectorRegistry.getModelNames()

        val realtimeModelSpinner = view.findViewById<StringListSpinner>(R.id.realtimeModelSpinner)
        realtimeModelSpinner.setup(
            detectorRegistryModels,
            settingsService.realtimeModel,
            settingsService.fontSize.toFloat()
        ) { selected: Int ->
            settingsService.realtimeModel = detectorRegistryModels[selected]
            settingsService.save()
        }

        val preciseModelSpinner = view.findViewById<StringListSpinner>(R.id.preciseModelSpinner)
        preciseModelSpinner.setup(
            detectorRegistryModels,
            settingsService.precisionModel,
            settingsService.fontSize.toFloat()
        ) { selected: Int ->
            settingsService.precisionModel = detectorRegistryModels[selected]
            settingsService.save()
        }

        val languageSpinner = view.findViewById<StringListSpinner>(R.id.languageSpinner)
        languageSpinner.setup(
            SettingsService.languageOptions,
            settingsService.language,
            settingsService.fontSize.toFloat()
        ) { selected: Int ->
            settingsService.language = SettingsService.languageOptions[selected]
            settingsService.save()
        }

        setupPerformanceMonitorSwitch()

        setupFontPicker(view)
    }

    /**
     * Setup performance logging and its verbose option switches.
     */
    private fun setupPerformanceMonitorSwitch() {

        val switchShow = view?.findViewById<SwitchCompat>(R.id.showPerformanceSwitch)
        switchShow?.isChecked = settingsService.showMetrics

        switchShow?.setOnCheckedChangeListener { _, isChecked ->
            settingsService.showMetrics = isChecked
            settingsService.save()
        }

        val switchVerbose = view?.findViewById<SwitchCompat>(R.id.verbosePerformanceSwitch)
        switchVerbose?.isChecked = settingsService.verboseMetrics

        switchVerbose?.setOnCheckedChangeListener { _, isChecked ->
            settingsService.verboseMetrics = isChecked
            settingsService.save()
        }
    }

    /**
     * Setup NumberPicker for font size.
     */
    private fun setupFontPicker(view: View) {
        val picker = view.findViewById<NumberPicker>(R.id.fontSizePicker)
        picker.minValue = 10
        picker.maxValue = 30
        picker.value = settingsService.fontSize
        updateFontSize()

        picker.setOnValueChangedListener { _, _, newValue ->
            settingsService.fontSize = newValue
            settingsService.save()
            updateFontSize()
        }
    }

    private fun updateFontSize() {
        val textIds = arrayOf(
            R.id.textFontSize,
            R.id.textShowPerformance,
            R.id.textVerbosePerformance,
            R.id.textChooseLanguage,
            R.id.textChooseRealtimeModel,
            R.id.textChoosePrecisionModel,
            R.id.textChooseLanguage,
            R.id.metricsSeparator
        )
        for (i in textIds) {
            view?.findViewById<TextView>(i)
                ?.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.fontSize.toFloat())
        }

        val spinnerIds = arrayOf(
            R.id.realtimeModelSpinner,
            R.id.preciseModelSpinner,
            R.id.languageSpinner
        )

        for (i in spinnerIds) {
            val adapter = view?.findViewById<Spinner>(i)?.adapter
            if (adapter is ResizableSpinnerAdapter) {
                adapter.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.fontSize.toFloat())
            }
        }
    }
}