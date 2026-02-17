package cv.cbglib.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Abstract Fragment class used as a base for all Fragments.
 * [layoutRes] is an ID in from `R.layout.` and must correspond to the xml file that the subclassed fragment belongs to.
 */
abstract class BaseFragment(
    private val layoutRes: Int
) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(layoutRes, container, false)
    }
}