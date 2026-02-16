package cv.demoapps.bangdemo.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import cv.cbglib.commonUI.ResizableSpinnerAdapter
import cv.cbglib.fragments.BaseFragment
import cv.cbglib.services.SettingsService
import cv.demoapps.bangdemo.MyApp
import cv.demoapps.bangdemo.R

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private val settingsService by lazy {
        (requireContext().applicationContext as MyApp).settingsService
    }

    private val assetService by lazy {
        (requireContext().applicationContext as MyApp).assetService
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinnerWithStringValues(
            R.id.realtimeModelSpinner,
            assetService.availableModels,
            settingsService.realtimeModel
        ) { selected: String ->
            settingsService.realtimeModel = selected
            settingsService.save()
        }

        setupSpinnerWithStringValues(
            R.id.preciseModelSpinner,
            assetService.availableModels,
            settingsService.precisionModel
        ) { selected: String ->
            settingsService.precisionModel = selected
            settingsService.save()
        }

        setupSpinnerWithStringValues(
            R.id.languageSpinner,
            SettingsService.languageOptions,
            settingsService.language
        ) { selected: String ->
            settingsService.language = selected
            settingsService.save()
        }

        setupPerformanceMonitorSwitch()

        setupFontPicker(view)
    }


    /**
     * Reusable function to setup Android Spinner UI element with String array values.
     */
    private fun setupSpinnerWithStringValues(
        spinnerId: Int,
        items: Array<String>,
        selectedItem: String?,
        onItemChanged: (String) -> Unit
    ) {
        val spinner = view?.findViewById<Spinner>(spinnerId)

        val modelAdapter = ResizableSpinnerAdapter(
            requireContext(),
            items,
            TypedValue.COMPLEX_UNIT_SP,
            settingsService.fontSize.toFloat(),
        )
        spinner?.adapter = modelAdapter

        val index = if (selectedItem != null) items.indexOf(selectedItem) else -1
        spinner?.setSelection(if (index == -1) 0 else index, false)

        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            var lastSelected: String = ""

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val current = items[position]

                if (current == lastSelected) return

                lastSelected = current
                onItemChanged(current)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    /**
     * Setup performance logging and its verbose option switches.
     */
    private fun setupPerformanceMonitorSwitch() {

        val switchShow = view?.findViewById<SwitchCompat>(R.id.showPerformanceSwitch)
        switchShow?.isChecked = settingsService.showPerformance

        switchShow?.setOnCheckedChangeListener { _, isChecked ->
            settingsService.showPerformance = isChecked
            settingsService.save()
        }

        val switchVerbose = view?.findViewById<SwitchCompat>(R.id.verbosePerformanceSwitch)
        switchVerbose?.isChecked = settingsService.verbosePerformance

        switchVerbose?.setOnCheckedChangeListener { _, isChecked ->
            settingsService.verbosePerformance = isChecked
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