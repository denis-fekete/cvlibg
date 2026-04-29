package com.fekete.bangdemo.fragments

import android.app.Application
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.fekete.bangdemo.MyApp
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.views.LinkView
import com.fekete.cvlibg.utils.AssetLoader
import com.fekete.bangdemo.R
import com.fekete.bangdemo.databinding.FragmentCardDetailsBinding
import kotlin.collections.get

private const val ARG_DETECTION_ID = "id" // must match with argument name in nav_graph.xml

/**
 * Fragment to used for Card details display,
 * Use the [DetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DetailsFragment : BaseFragment<FragmentCardDetailsBinding>(
    FragmentCardDetailsBinding::inflate
) {
    private var linkId: String? = null

    lateinit var thisCard: CardDetail

    private val assetLoader by lazy {
        return@lazy AssetLoader((requireContext().applicationContext as Application))
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
        // remove template view items
        binding.linksLayout.removeAllViews()

        if (linkId != null && cardDetailsService.data[linkId] != null) {
            thisCard = cardDetailsService.data[linkId]!!
        } else {
            return
        }

        binding.titleText.text = thisCard.title
        binding.descriptionText.text = thisCard.description
        binding.descriptionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.data.fontSize.toFloat())

        if (thisCard.imagePath != null) {
            val bitmap =
                assetLoader.loadImage("${thisCard.imagePath}", "card_scans")
            if (bitmap != null) {
                binding.imageView.setImageBitmap(bitmap)
            } else {
                binding.imageView.visibility = View.GONE
            }
        } else {
            binding.imageView.visibility = View.GONE
        }

        val navController = findNavController()
        if (thisCard.links.isNotEmpty()) {
            for (id in thisCard.links) {
                val linkCard = cardDetailsService.data[id] ?: continue

                var linkView: LinkView
                val imgPath = linkCard.imagePath

                if (imgPath != null) {
                    val bitmap = assetLoader.loadImage(imgPath, "")
                    linkView = LinkView(
                        context,
                        null,
                        TypedValue.COMPLEX_UNIT_SP,
                        settingsService.data.fontSize.toFloat(),
                        bitmap
                    )
                } else {
                    linkView = LinkView(
                        context,
                        linkCard.title,
                        TypedValue.COMPLEX_UNIT_SP,
                        settingsService.data.fontSize.toFloat(),
                        null
                    )
                }

                linkView.setOnClickListener {
                    navController.navigate(R.id.cardDetailsFragment, bundleOf("id" to id))
                }

                binding.linksLayout.addView(linkView)
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
            DetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DETECTION_ID, id)
                }
            }
    }
}