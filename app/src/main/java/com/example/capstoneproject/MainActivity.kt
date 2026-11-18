package com.example.capstoneproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.capstoneproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get NavHostFragment and NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.flFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Connect BottomNavigationView to NavController
        binding.bottomNavigationView.setupWithNavController(navController)

        // targetSdk 35+ ignores windowSoftInputMode=adjustResize unless the app handles
        // insets itself: pad the root by the keyboard height so scrollable forms stay reachable.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = ime)
            insets
        }
    }
}

