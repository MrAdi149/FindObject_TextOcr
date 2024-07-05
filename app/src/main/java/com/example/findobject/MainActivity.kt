package com.example.findobject

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.findobject.databinding.ActivityMainBinding
import np.com.susanthapa.curved_bottom_navigation.CbnMenuItem

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        setSupportActionBar(findViewById(R.id.toolbar))

        // Define menu items
        val menuItems = arrayOf(
            CbnMenuItem(
                R.drawable.ic_baseline_photo_camera_24,
                R.drawable.avd_camera,
                R.id.camera_fragment
            ),
            CbnMenuItem(
                R.drawable.ic_baseline_photo_library_24,
                R.drawable.avd_gallery,
                R.id.gallery_fragment
            )
        )

        // Initialize the CurvedBottomNavigationView with menu items
        activityMainBinding.navigation.setMenuItems(menuItems)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        // Set up the AppBarConfiguration with the top-level destinations
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.camera_fragment,
                R.id.gallery_fragment
            )
        )

        // Set up the action bar with the navigation controller and the app bar configuration
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up the bottom navigation view with the navigation controller
        activityMainBinding.navigation.setupWithNavController(navController)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
