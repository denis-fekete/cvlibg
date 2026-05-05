package com.fekete.bangdemo

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.fekete.cvlibg.utils.CommonUtils
import com.fekete.bangdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var btnCamera: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnBenchmark: ImageButton
    private lateinit var navController: NavController

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

        btnCamera = binding.btnNavCamera
        btnSettings = binding.btnNavSettings
        btnBenchmark = binding.btnBenchmarks

        btnCamera.setOnClickListener {
            navigateTo(R.id.cameraFragment)
        }

        btnSettings.setOnClickListener {
            navigateTo(R.id.settingsFragment)
        }

        btnBenchmark.setOnClickListener {
            navigateTo(R.id.benchmarkFragment)
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

        navController.navigate(destinationId)
    }
}