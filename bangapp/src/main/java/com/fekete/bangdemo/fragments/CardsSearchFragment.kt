package com.fekete.bangdemo.fragments

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.fekete.bangdemo.MyApp
import com.fekete.bangdemo.databinding.FragmentCardsSearchBinding
import com.fekete.cvlibg.utils.AssetLoader

/**
 * A simple [Fragment] subclass.
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

    private val settingsService by lazy {
        (requireContext().applicationContext as MyApp).settingsService
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.overlaysVisible(inventory = false, other = false) // overlays are not desired on this fragment
    }
}