package cv.demoapps.bangdemo.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import cv.cbglib.fragments.AbstractCameraFragment
import cv.demoapps.bangdemo.MyApp
import cv.demoapps.bangdemo.R

/**
 * [CameraFragment] is class derived from [AbstractCameraFragment]. Basic functionality can be achieved by simply
 * inheriting from class, giving current layout "ID". On detection click must be activated here! The
 * [CameraFragmentDirections] is needed for navigation unless other navigation system is used.
 */
class CameraFragment : AbstractCameraFragment(R.layout.fragment_camera) {
    private val class2linkService by lazy {
        (requireContext().applicationContext as MyApp).class2linkService
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // attaching onDetectionClicked event
        detectionOverlay.onDetectionClicked = { detection ->
            val action =
                CameraFragmentDirections.actionCameraFragmentToCardDetailsFragment(class2linkService.items[detection.classIndex]!!.linkId)

            findNavController().navigate(action)
        }
    }
}