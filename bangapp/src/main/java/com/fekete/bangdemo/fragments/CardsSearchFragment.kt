package com.fekete.bangdemo.fragments

import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.fekete.bangdemo.MyApp
import com.fekete.bangdemo.R
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.data.CardType
import com.fekete.bangdemo.databinding.FragmentCardsSearchBinding
import com.fekete.bangdemo.search.CardSearchAdapter
import com.fekete.bangdemo.utils.navigateAction
import com.fekete.cvlibg.utils.AssetLoader
import java.util.Locale.getDefault

/**
 * Class utilizing [SearchView] and [androidx.recyclerview.widget.RecyclerView] for loading all available [CardDetail]
 * objects from [cardDetailsService]. Clicks move user to the details or add card to the inventory using
 * [gameStateSharedViewModel].
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class CardsSearchFragment : BaseFragment<FragmentCardsSearchBinding>(
    FragmentCardsSearchBinding::inflate
) {
    private val cardDetailsService by lazy {
        (requireContext().applicationContext as MyApp).cardDetailsService
    }

    private val assetLoader by lazy {
        return@lazy AssetLoader((requireContext().applicationContext as Application))
    }

    private lateinit var adapter: CardSearchAdapter

    /**
     * List of cards, that can be searched
     */
    private var searchableCards: List<CardDetail> = emptyList()
    private lateinit var navController: NavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameStateSharedViewModel.overlaysVisible(
            inventory = false,
            other = false
        ) // overlays are not desired on this fragment

        navController = findNavController()

        adapter = CardSearchAdapter(
            assetLoader = assetLoader,
            onCardClicked = { cardDetail ->
                onCardClicked(cardDetail)
            },
            onAddCardClicked = { cardDetail ->
                onAddCardClicked(cardDetail)
            })

        binding.searchRecycleView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.searchRecycleView.adapter = adapter

        // don't allow searching for symbols, and backgrounds
        searchableCards =
            cardDetailsService.data.values.filter { it.type != CardType.Other && it.type != CardType.Symbol }.toList()

        // display all cards on load
        adapter.setList(searchableCards)

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                filter(query)
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filter(newText)
                return true
            }
        })
    }

    /**
     * Called on card/image being clicked. Search is part of nav host, use actions instead of bundle.
     */
    private fun onCardClicked(cardDetail: CardDetail) {
        val action = CardsSearchFragmentDirections.actionCardSearchFragmentToCardDetailsFragment(cardDetail.id)
        navController.navigateAction(action)
    }

    /**
     * Called on add button under card being clicked.
     */
    private fun onAddCardClicked(cardDetail: CardDetail) {
        gameStateSharedViewModel.addToInventory(cardDetail)

        Toast.makeText(
            requireContext(),
            getString(R.string.added_to_inventory_popup),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Filter cards from the [searchableCards] based on the provided string [query]. Use card's title or role.
     */
    private fun filter(query: String) {
        val found = mutableListOf<CardDetail>()

        val lowerCaseQuery = query.lowercase(getDefault())
        for (card in searchableCards) {
            if (card.title.lowercase(getDefault()).contains(lowerCaseQuery)) {
                found.add(card)
            } else if (card.type.name.lowercase(getDefault()).contains(lowerCaseQuery)) {
                found.add(card)
            }
        }

        adapter.setList(found)
    }
}