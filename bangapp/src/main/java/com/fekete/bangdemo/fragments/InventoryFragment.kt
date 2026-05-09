package com.fekete.bangdemo.fragments

import android.app.Application
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout.LayoutParams
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.fekete.bangdemo.MyApp
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.databinding.FragmentInventoryBinding
import com.fekete.bangdemo.R
import com.fekete.bangdemo.utils.navigateDestination
import com.fekete.bangdemo.views.InventoryItem
import com.fekete.cvlibg.utils.AssetLoader
import kotlinx.coroutines.launch

/**
 * Long living fragment, meant to be placed on top of other fragments. Contains representation of inventory and reacts
 * to changes in it from the [sharedViewModel].
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class InventoryFragment : BaseFragment<FragmentInventoryBinding>(
    FragmentInventoryBinding::inflate
) {
    private val cardDetailsService by lazy {
        (requireContext().applicationContext as MyApp).cardDetailsService
    }

    private val assetLoader by lazy {
        return@lazy AssetLoader((requireContext().applicationContext as Application))
    }

    private var navController: NavController? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = requireActivity()
            .supportFragmentManager
            .findFragmentById(R.id.navHostContainer)
            ?.findNavController()

        binding.hideInventory.setOnClickListener {
            setInventoryShowed(false)
        }

        binding.showInventory.setOnClickListener {
            setInventoryShowed(true)
        }

        setInventoryShowed(false)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.inventory.collect { cardsList ->
                        refreshCardList(cardsList)
                    }
                }

                launch {
                    sharedViewModel.inventoryVisible.collect { value ->
                        binding.root.visibility = if (value) VISIBLE else GONE
                    }
                }
            }
        }
    }

    /**
     * Refreshes the inventory (scroll view) with new values
     */
    private fun refreshCardList(cardsList: List<CardDetail>) {
        binding.inventoryListView.removeAllViews()

        for (card in cardsList) {
            val linkCard = cardDetailsService.data[card.id] ?: continue

            val imgPath = linkCard.imagePath ?: continue

            val bitmap = assetLoader.loadImage(imgPath, "") ?: continue
            val item = InventoryItem(requireContext(), bitmap)

            item.onImageClicked = {
                navController?.navigateDestination(
                    R.id.cardDetailsFragment,
                    bundleOf("id" to card.id)
                )
            }

            item.onDeleteClicked = {
                sharedViewModel.removeFromInventory(card)
            }

            binding.inventoryListView.addView(item)
        }
    }


    private fun setInventoryShowed(value: Boolean) {
        binding.inventoryContainer.visibility = if (value) VISIBLE else GONE
        binding.hideInventory.visibility = if (value) VISIBLE else GONE
        binding.showInventory.visibility = if (value) GONE else VISIBLE
    }
}