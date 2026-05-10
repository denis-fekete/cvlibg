package com.fekete.bangdemo

import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.data.GameState
import com.fekete.cvlibg.utils.CommonUtils
import com.fekete.bangdemo.databinding.ActivityMainBinding
import com.fekete.bangdemo.utils.navigateMain
import com.fekete.bangdemo.viewmodels.GameStateSharedViewModel
import kotlin.getValue

/**
 * Main activity, uses and host different fragments with shared navigation buttons belonging to the activity. Its
 * purpose is to load, and save [GameState] data, holding data game data from previous sessions.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    private val sharedViewModel: GameStateSharedViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    private val gameStateService by lazy {
        (applicationContext as MyApp).gameStateService
    }

    private val cardDetailsService by lazy {
        (applicationContext as MyApp).cardDetailsService
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        CommonUtils.checkCameraPermission(this, this)

        val app = application as MyApp
        if (app.errorMessageCardDetail != null) {
            AlertDialog.Builder(this)
                .setTitle("Card Details service error")
                .setMessage(app.errorMessageCardDetail)
                .setPositiveButton("OK", null)
                .show()
        }
        if (app.errorMessageClass2Link != null) {
            AlertDialog.Builder(this)
                .setTitle("Class to Link service error")
                .setMessage(app.errorMessageClass2Link)
                .setPositiveButton("OK", null)
                .show()
        }

        loadLastGameState()
    }


    /**
     * Set up navigation using navigation host container to swap between fragments
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostContainer) as NavHostFragment

        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // make camera button invisible when in camera fragment
            binding.btnNavCamera.visibility = if (destination.id == R.id.cameraFragment) INVISIBLE else VISIBLE
            // disable settings btn in settings
            binding.btnNavSettings.visibility = if (destination.id == R.id.settingsFragment) INVISIBLE else VISIBLE
        }

        binding.btnNavCamera.setOnClickListener {
            navController.navigateMain(R.id.cameraFragment)
        }

        binding.btnNavSettings.setOnClickListener {
            navController.navigateMain(R.id.settingsFragment)
        }

        binding.btnNavSearch.setOnClickListener {
            navController.navigateMain(R.id.cardSearchFragment)
        }

        binding.btnNavGameStats.setOnClickListener {
            navController.navigateMain(R.id.gameStatsFragment)
        }
    }

    /**
     * Load last saved [GameState] using the [com.fekete.cvlibg.services.StorageService] and update [sharedViewModel]
     * with its values.
     */
    private fun loadLastGameState() {
        val lastSave = gameStateService.item ?: return

        if (lastSave.role != null) {
            val card = cardDetailsService.data[lastSave.role] ?: CardDetail()
            sharedViewModel.setRole(card)
        }

        if (lastSave.character != null) {
            val card = cardDetailsService.data[lastSave.character] ?: CardDetail()
            sharedViewModel.setCharacter(card)
        }

        for (link in lastSave.inventory) {
            val card = cardDetailsService.data[link] ?: continue
            sharedViewModel.addToInventory(card)
        }
    }

    /**
     * Save the current [GameState] using the [com.fekete.cvlibg.services.StorageService].
     */
    private fun saveGameState() {
        val roleLink = sharedViewModel.role.value.id
        val charLink = sharedViewModel.character.value.id

        val inventoryLinks = mutableListOf<String>()
        sharedViewModel.inventory.value.forEach {
            inventoryLinks.add(it.id)
        }

        val gameState = GameState(
            0,
            roleLink,
            charLink,
            inventoryLinks
        )

        // only the first game state will be used
        gameStateService.data[0] = gameState
        gameStateService.save()
    }


    /**
     * Save [com.fekete.bangdemo.data.GameState] on activity being stopped (minimizing).
     */
    override fun onStop() {
        super.onStop()

        saveGameState()
    }
}