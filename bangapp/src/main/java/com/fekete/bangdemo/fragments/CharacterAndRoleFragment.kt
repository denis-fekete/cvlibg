package com.fekete.bangdemo.fragments

import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.fekete.bangdemo.R
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.databinding.FragmentCharacterAndRoleBinding
import com.fekete.bangdemo.utils.navigateDestination
import com.fekete.cvlibg.utils.AssetLoader
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * Use the [CharacterAndRoleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CharacterAndRoleFragment : BaseFragment<FragmentCharacterAndRoleBinding>(
    FragmentCharacterAndRoleBinding::inflate
) {
    private val assetLoader by lazy {
        AssetLoader((requireContext().applicationContext as Application))
    }

    private var navController: NavController? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = requireActivity()
            .supportFragmentManager
            .findFragmentById(R.id.navHostContainer)
            ?.findNavController()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.character.collect { card ->
                        updateCharacterPreview(card)
                    }
                }
                launch {
                    sharedViewModel.role.collect { card ->
                        updateRole(card)
                    }
                }

                launch {
                    sharedViewModel.otherOverlaysVisible.collect { value ->
                        binding.root.visibility = if (value) VISIBLE else GONE
                    }
                }
            }
        }
    }

    /**
     * Update
     */
    private fun updateCharacterPreview(card: CardDetail) {
        if (card.imagePath == null) {
            binding.characterPreview.visibility = INVISIBLE
            return
        }

        binding.characterPreview.visibility = VISIBLE
        val bitmap = assetLoader.loadImage(card.imagePath)

        binding.characterPreview.setImageBitmap(bitmap)

        binding.characterPreview.setOnClickListener {
            navController?.navigateDestination(
                R.id.cardDetailsFragment,
                bundleOf("id" to card.id)
            )
        }
    }

    /**
     * Update the current role to the provided [card]. If the card has empty string as ID, its navigation will be
     * turned off.
     */
    private fun updateRole(card: CardDetail) {
        binding.roleContainer.setOnClickListener {
            if (card.id.isEmpty())
                return@setOnClickListener

            navController?.navigateDestination(
                R.id.cardDetailsFragment,
                bundleOf("id" to card.id)
            )
        }
    }
}