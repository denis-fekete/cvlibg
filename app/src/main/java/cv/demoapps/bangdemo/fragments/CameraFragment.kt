package cv.demoapps.bangdemo.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import cv.cbglib.fragments.AbstractCameraFragment
import cv.demoapps.bangdemo.MyApp
import cv.demoapps.bangdemo.R

/**
 * [CameraFragment] is class derived from [AbstractCameraFragment]. Basic functionality can be achieved by simply
 * inheriting from class, giving current layout "ID". On detection click must be activated here!
 */
class CameraFragment : AbstractCameraFragment(R.layout.fragment_camera) {
    private val class2linkService by lazy {
        (requireContext().applicationContext as MyApp).class2linkService
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // attaching onDetectionClicked event
        detectionOverlay.onDetectionClicked = { detection ->
            val bundle = bundleOf(
                "id" to class2linkService.items[detection.classIndex]!!.linkId
            )

            findNavController().navigate(R.id.cardDetailsFragment, bundle)
        }
    }
}