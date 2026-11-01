package com.example.capstoneproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.capstoneproject.ui.HomeFragment
import com.example.capstoneproject.ui.ProfileFragment
import com.example.capstoneproject.ui.MealFragment
import com.example.capstoneproject.ui.ProgressFragment
import com.example.capstoneproject.ui.TrainFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)

        val homeFragment = HomeFragment()
        val profileFragment = ProfileFragment()
        val mealFragment = MealFragment()
        val progressFragment = ProgressFragment()
        val trainFragment = TrainFragment()

        setCurrentFragment(homeFragment)

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.home -> setCurrentFragment(homeFragment)
                R.id.profile -> setCurrentFragment(profileFragment)
                R.id.meal -> setCurrentFragment(mealFragment)
                R.id.progress -> setCurrentFragment(progressFragment)
                R.id.train -> setCurrentFragment(trainFragment)
            }
            true
        }

    }

    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commitNow()
        }


}

