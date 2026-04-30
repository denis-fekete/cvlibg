package com.fekete.bangdemo.fragments

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ScrollView
import androidx.lifecycle.lifecycleScope
import com.fekete.bangdemo.MyApp
import com.fekete.cvlibg.benchmark.AccuracyBenchmark
import com.fekete.cvlibg.benchmark.DetectorBenchmark
import com.fekete.cvlibg.benchmark.PerformanceBenchmark
import com.fekete.cvlibg.utils.DetectorRegistry
import com.fekete.bangdemo.databinding.FragmentBenchmarkBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Subclass of the [Fragment] class hosting benchmark for YOLO models.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class BenchmarkFragment : BaseFragment<FragmentBenchmarkBinding>(
    FragmentBenchmarkBinding::inflate
) {
    private val settingsService by lazy {
        (requireContext().applicationContext as MyApp).settingsService
    }
    private var selectedBenchmark: String? = null
    private var selectedModelName: String? = null
    private var currentBenchmark: DetectorBenchmark? = null
    private val benchmarkMap = mapOf<String, (context: Context) -> DetectorBenchmark>(
        "Performance benchmark" to { context -> PerformanceBenchmark(context) },
        "Accuracy benchmark" to { context -> AccuracyBenchmark(context, "AccuracyBenchmark", 0.4f) },
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.benchmarkSpinner.setup(
            benchmarkMap.keys,
            null,
            settingsService.data.fontSize.toFloat()
        ) { selected: Int ->
            selectedBenchmark = benchmarkMap.keys.toList()[selected]
        }

        selectedBenchmark = benchmarkMap.keys.toList().firstOrNull()

        binding.benchmarkModelSpinner.setup(
            DetectorRegistry.getModelNames(),
            null,
            settingsService.data.fontSize.toFloat()
        ) { selected: Int ->
            selectedModelName = DetectorRegistry.getModelNames()[selected]
        }

        selectedModelName = DetectorRegistry.getModelNames().firstOrNull()

        binding.benchmarkStopButton.setOnClickListener {
            currentBenchmark?.stop()
        }

        binding.benchmarkStartButton.setOnClickListener {
            startBenchmark()
        }

        binding.benchmarkAllCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.benchmarkModelSpinner.isEnabled = !isChecked
        }

        updateFontSize()
    }

    private fun startBenchmark() {
        if (selectedBenchmark == null || selectedModelName == null)
            return

        val benchmarkBuilder = benchmarkMap[selectedBenchmark!!]
        binding.benchmarkStartButton.isEnabled = false
        binding.benchmarkSpinner.isEnabled = false
        binding.benchmarkModelSpinner.isEnabled = false
        binding.benchmarkStopButton.isEnabled = true

        val listOfModels = if (binding.benchmarkAllCheckbox.isChecked) {
            DetectorRegistry.getModelNames()
        } else {
            listOf(selectedModelName!!)
        }

        currentBenchmark = benchmarkBuilder?.invoke(requireContext())

        // connect status update callback function
        currentBenchmark?.onStatusUpdate = { msg ->
            lifecycleScope.launch(Dispatchers.Main) {
                updateStatus(msg)
            }
        }

        // connect on progress update callback
        currentBenchmark?.onProgressUpdate = { value ->
            lifecycleScope.launch(Dispatchers.Main) {
                binding.benchmarkProgressBar.progress = value
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            listOfModels.forEach { modelName ->
                val detector = DetectorRegistry.createDetector(modelName)
                currentBenchmark?.replaceDetector(detector)

                binding.benchmarkProgressBar.progress = 0

                // launch coroutine on main thread, switch to CPU heavy for benchmark, update UI once it finished, on main thread
                withContext(Dispatchers.Default) {
                    currentBenchmark?.run(binding.numberOfRunsEdit.text.toString().toULong())
                }

                withContext(Dispatchers.IO) {
                    // use application context to save data if fragment is left
                    currentBenchmark?.save(null, requireContext().applicationContext)
                }
            }

            binding.benchmarkStartButton.isEnabled = true
            binding.benchmarkStopButton.isEnabled = false
            binding.benchmarkSpinner.isEnabled = true
            binding.benchmarkModelSpinner.isEnabled = true
        }
    }


    /**
     * Updates the status log with new value, each starting on new line
     */
    private fun updateStatus(msg: String) {
        binding.benchmarkStatusLog.text = "${binding.benchmarkStatusLog.text}$msg\n"

        binding.benchmarkScrollView.post {
            binding.benchmarkScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    /**
     * Updates font size of text using [com.fekete.cvlibg.services.ConfigService]
     */
    private fun updateFontSize() {
        val elements = arrayOf(
            binding.benchmarkTitle,
            binding.textChooseBenchmark,
            binding.textChooseBenchmarkModel
        )

        for (element in elements) {
            element.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.data.fontSize.toFloat())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentBenchmark?.destroy()
    }
}