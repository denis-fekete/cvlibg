package com.fekete.cvlibg.detection.detectors

/**
 * A data class that registers/links [Detector] derived classes to the name of model. The name of model can be shown in
 * UI or a static 'code' for specific model. See [register] function for usage.
 */
data class DetectorRegistry(
    val name: String,
    val modelPath: String,
    val factory: (String) -> Detector,
) {
    private fun create(): Detector = factory(modelPath)

    companion object {
        private val registry = mutableMapOf<String, DetectorRegistry>()

        /**
         * Links a model name to the detector factory.
         *
         * @param modelName string name representing the [Detector].
         * @param modelPath path to the model under assets.
         * @param factory a factory method that creates the model. Usage:
         * ```
         * DetectorRegistry.link("Model name (displayed for example in UI)", "path_to_model_file.onnx")
         *         { path -> Detector(path, arg1, arg2, ...) }
         * ```
         * Where Detector is a class that was derived from abstract class [Detector].
         */
        fun register(modelName: String, modelPath: String, factory: (String) -> Detector) {
            registry[modelName] = DetectorRegistry(modelName, modelPath, factory)
        }

        /**
         * Returns a list of all registered model names
         */
        fun getModelNames(): List<String> {
            return registry.keys.toList()
        }

        /**
         * Create an instance of Detector based on model name that was registered.
         */
        fun createDetector(name: String): Detector {
            return registry[name]?.create()
                ?: throw IllegalArgumentException("Model '$name' not registered")
        }

        /**
         * Create an instance of Detector, first detector from the registry is chosen.
         */
        fun createDetector(): Detector {
            if (registry.isNotEmpty()) {
                val key = registry.keys.toList().first()
                return createDetector(key)
            } else {
                throw IllegalArgumentException("No model were registered into DetectorRegistry")
            }
        }
    }
}
