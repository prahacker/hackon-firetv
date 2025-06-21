package com.example.firetv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                // This line is the critical fix.
                // It ensures the banner layout is NOT deleted.
                .replace(R.id.browse_fragment, MovieFragment())
                .commitNow()
        }
    }
}