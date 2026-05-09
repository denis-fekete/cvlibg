package com.fekete.bangdemo

import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.fekete.cvlibg.utils.CommonUtils
import com.fekete.bangdemo.databinding.ActivityMainBinding
import com.fekete.bangdemo.utils.navigateMain
import com.fekete.bangdemo.viewmodels.SharedCardsViewModel
import kotlin.getValue

/**
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    private val sharedViewModel: SharedCardsViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

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
    }

    /**
     * Navigates to another fragment, disables navigating to same, and destroys back stack of previous destinations.
     */
    private fun navigateTo(destinationId: Int) {
        val currentId = navController.currentDestination?.id

        if (currentId == destinationId) return

        if (currentId != null) {
            navController.popBackStack(currentId, true)
        }
    }
}