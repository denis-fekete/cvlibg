package com.fekete.bangdemo.fragments

import android.app.Application
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.fekete.bangdemo.MyApp
import com.fekete.bangdemo.R
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.data.CardType
import com.fekete.bangdemo.data.Language
import com.fekete.bangdemo.views.LinkView
import com.fekete.cvlibg.utils.AssetLoader
import com.fekete.bangdemo.databinding.FragmentCardDetailsBinding
import kotlin.collections.get

private const val ARG_DETECTION_ID = "id" // must match with argument name in nav_graph.xml

/**
 * Fragment hosting information about the provided [CardDetail] card, this data is loaded using [cardDetailsService].
 * The argument accepted on creation of this fragment must match [CardDetail.id] of loaded cards from the service,
 * otherwise a generic with no information will be loaded.
 *
 * All links from the [CardDetail.links] are turned into clickable links to another [DetailsFragment].
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class DetailsFragment : BaseFragment<FragmentCardDetailsBinding>(
    FragmentCardDetailsBinding::inflate
) {
    private var currentId: String? = null

    lateinit var thisCard: CardDetail

    private val assetLoader by lazy {
        AssetLoader((requireContext().applicationContext as Application))
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
            currentId = it.getString(ARG_DETECTION_ID)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        gameStateSharedViewModel.overlaysVisible(
            inventory = false,
            other = false
        ) // overlays are not desired on this fragment

        // remove temporary view items, used for "seeing" how the preview from IDE looks like
        binding.linksLayout.removeAllViews()

        if (currentId != null && cardDetailsService.data[currentId] != null) {
            thisCard = cardDetailsService.data[currentId]!!
        } else {
            return
        }

        binding.titleText.text = thisCard.title
        when (settingsService.data.language) {
            Language.ENGLISH -> {
                binding.descriptionText.text = thisCard.descriptionEN
            }

            Language.CZECH -> {
                binding.descriptionText.text = thisCard.descriptionCS
            }

            Language.SLOVAK -> {
                binding.descriptionText.text = thisCard.descriptionSK
            }
        }

        binding.descriptionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.data.fontSize.toFloat())
        binding.relatedLinksTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.data.fontSize.toFloat())

        if (thisCard.imagePath != null) {
            val bitmap =
                assetLoader.loadImage("${thisCard.imagePath}", "card_scans")
            if (bitmap != null) {
                binding.detailsTitleImage.setImageBitmap(bitmap)
            } else {
                binding.detailsTitleImage.visibility = GONE
            }
        } else {
            binding.detailsTitleImage.visibility = GONE
        }


        if (thisCard.links.isNotEmpty()) {
            generateLinkList()
        } else {
            binding.relatedLinksTextView.visibility = GONE
            binding.linksScrollView.visibility = GONE
        }

        setupByCardType()
    }

    /**
     * Enable/disable functionalities of [DetailsFragment] based on loaded card type (role, character, ...).
     *
     * Based on the [thisCard] [CardDetail.type] value, disable, and enables buttons responsible for its actions.
     */
    private fun setupByCardType() {
        var selectedAddButton: androidx.appcompat.widget.AppCompatButton? = null
        var selectedRemoveButton: androidx.appcompat.widget.AppCompatButton? = null

        binding.btnAddToInventory.visibility = GONE
        binding.btnSetRole.visibility = GONE
        binding.btnSetCharacter.visibility = GONE
        binding.btnRemoveRole.visibility = GONE
        binding.btnRemoveCharacter.visibility = GONE

        when (thisCard.type) {
            CardType.Effect, CardType.Action, CardType.Weapon -> {
                selectedAddButton = binding.btnAddToInventory

                binding.btnAddToInventory.setOnClickListener {
                    val card = cardDetailsService.data[currentId] ?: return@setOnClickListener
                    gameStateSharedViewModel.addToInventory(card)

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.added_to_inventory_popup),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            CardType.Role -> {
                selectedAddButton = binding.btnSetRole
                binding.btnSetRole.setOnClickListener {
                    val card = cardDetailsService.data[currentId] ?: return@setOnClickListener
                    gameStateSharedViewModel.setRole(card)

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.role_updated_popup),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                if (gameStateSharedViewModel.role.value.id == thisCard.id) {
                    selectedRemoveButton = binding.btnRemoveRole
                    binding.btnRemoveRole.setOnClickListener {
                        gameStateSharedViewModel.setRole(CardDetail()) // setting card with default id will count as deleting it

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.role_clear_popup),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            CardType.Character -> {
                selectedAddButton = binding.btnSetCharacter
                binding.btnSetCharacter.setOnClickListener {
                    val card = cardDetailsService.data[currentId] ?: return@setOnClickListener
                    gameStateSharedViewModel.setCharacter(card)

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.character_updated_popup),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                if (gameStateSharedViewModel.character.value.id == thisCard.id) {
                    selectedRemoveButton = binding.btnRemoveCharacter
                    binding.btnRemoveCharacter.setOnClickListener {
                        gameStateSharedViewModel.setCharacter(CardDetail()) // setting card with default id will count as deleting it

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.character_clear_popup),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            else -> {}
        }

        selectedAddButton?.visibility = VISIBLE
        selectedAddButton?.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.data.fontSize.toFloat())
        selectedRemoveButton?.visibility = VISIBLE
        selectedRemoveButton?.setTextSize(TypedValue.COMPLEX_UNIT_SP, settingsService.data.fontSize.toFloat())

    }

    /**
     * Generates list of LinkView objects based on the current's card `link` property. Each link leads to another
     * [DetailsFragment]. Link are loaded with image if available, or their title property.
     */
    private fun generateLinkList() {
        val navController = findNavController()

        for (id in thisCard.links) {
            val card = cardDetailsService.data[id] ?: continue

            val imgPath = card.imagePath
            val bitmap = if (imgPath != null) assetLoader.loadImage(imgPath, "") else null

            // link view is linear layout with two elements, if image available, it will be used, otherwise its title
            // will be used
            val linkView = LinkView(
                requireContext(),
                bitmap,
                card.title,
                settingsService.data.fontSize.toFloat(),
            )

            linkView.setOnClickListener {
                val action = DetailsFragmentDirections.actionCardDetailsFragmentSelf(id = id)
                navController.navigate(action) // do not use `navigateAction`, so that the stack is not resetted
            }

            binding.linksLayout.addView(linkView)
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