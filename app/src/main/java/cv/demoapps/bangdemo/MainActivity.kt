package cv.demoapps.bangdemo

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import cv.cbglib.services.PermissionService

class MainActivity : AppCompatActivity() {
    private lateinit var btnCamera: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupNavigation()

        PermissionService.checkCameraPermission(this, this)
        PermissionService.checkStoragePermission(this, this)

        val app = application as MyApp
        if (app.errorMessageCardDetail != null) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("JsonAssetService error")
                .setMessage(app.errorMessageCardDetail)
                .setPositiveButton("OK", null)
                .show()
        }
        if (app.errorMessageClass2Link != null) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("JsonAssetService error")
                .setMessage(app.errorMessageClass2Link)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostContainer) as NavHostFragment

        navController = navHostFragment.navController

        btnCamera = findViewById<ImageButton>(R.id.btnNavCamera)
        btnSettings = findViewById<ImageButton>(R.id.btnNavSettings)

        btnCamera.setOnClickListener {
            navController.navigate(R.id.cameraFragment)
        }

        btnSettings.setOnClickListener {
            navController.navigate(R.id.settingsFragment)
        }
    }
}