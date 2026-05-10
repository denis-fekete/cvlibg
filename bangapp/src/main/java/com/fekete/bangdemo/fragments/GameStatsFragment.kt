package com.fekete.bangdemo.fragments

import android.os.Bundle
import android.view.View
import com.fekete.bangdemo.databinding.FragmentGameStatsBinding

/**
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class GameStatsFragment : BaseFragment<FragmentGameStatsBinding>(
    FragmentGameStatsBinding::inflate
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameStateSharedViewModel.overlaysVisible(inventory = true, other = true)
    }
}