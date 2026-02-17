package cv.demoapps.bangdemo.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import cv.cbglib.fragments.BaseFragment
import cv.demoapps.bangdemo.MyApp
import cv.demoapps.bangdemo.R
import cv.demoapps.bangdemo.data.CardDetail
import cv.demoapps.bangdemo.views.LinkView

// must match with argument name in nav_graph.xml
private const val ARG_DETECTION_ID = "id"

/**
 * Fragment to used for Card details display,
 * Use the [CardDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CardDetailsFragment : BaseFragment(R.layout.fragment_card_details) {
    private var linkId: String? = null
    lateinit var titleTextView: TextView
    lateinit var descriptionTextView: TextView
    lateinit var imageView: ImageView
    lateinit var linksLayout: LinearLayout
    lateinit var thisCard: CardDetail

    private val assetService by lazy {
        (requireContext().applicationContext as MyApp).assetService
    }

    private val cardDetailsService by lazy {
        (requireContext().applicationContext as MyApp).cardDetailsService
    }

    private val settingsService by lazy {
        (requireContext().applicationContext as MyApp).settingsService
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            linkId = it.getString(ARG_DETECTION_ID)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        titleTextView = view.findViewById<TextView>(R.id.titleText)
        descriptionTextView = view.findViewById<TextView>(R.id.descriptionText)
        imageView = view.findViewById<ImageView>(R.id.imageView)
        linksLayout = view.findViewById<LinearLayout>(R.id.linksLayout)

        // remove template view items
        linksLayout.removeAllViews()

        if (linkId != null && cardDetailsService.items[linkId] != null) {
            thisCard = cardDetailsService.items[linkId]!!
        } else {
            return
        }

        titleTextView.text = thisCard.title
        descriptionTextView.text = thisCard.description
        descriptionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.fontSize.toFloat())

        if (thisCard.imagePath != null) {
            val bitmap =
                assetService.getImageBitmap("${thisCard.imagePath}", "card_scans")
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.visibility = View.GONE
            }
        } else {
            imageView.visibility = View.GONE
        }

        val navController = findNavController()
        if (thisCard.links.isNotEmpty()) {
            for (id in thisCard.links) {
                val linkCard = cardDetailsService.items[id] ?: continue

                var linkView: LinkView
                val imgPath = linkCard.imagePath

                if (imgPath != null) {
                    val bitmap = assetService.getImageBitmap(imgPath, "")
                    linkView = LinkView(
                        context,
                        null,
                        TypedValue.COMPLEX_UNIT_SP,
                        settingsService.fontSize.toFloat(),
                        bitmap
                    )
                } else {
                    linkView = LinkView(
                        context,
                        linkCard.title,
                        TypedValue.COMPLEX_UNIT_SP,
                        settingsService.fontSize.toFloat(),
                        null
                    )
                }

                linkView.setOnClickListener {
                    navController.navigate(R.id.cardDetailsFragment, bundleOf("id" to id))
                }

                linksLayout.addView(linkView)
            }
        }
    }

    companion object {
        /**
         * Factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param id Detection ID that will be displayed in Fragment
         * @return A new instance of fragment CardDetailsFragment.
         */
        @JvmStatic
        fun newInstance(id: String) =
            CardDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DETECTION_ID, id)
                }
            }
    }
}