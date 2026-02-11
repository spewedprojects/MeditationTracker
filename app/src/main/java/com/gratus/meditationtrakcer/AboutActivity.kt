package com.gratus.meditationtrakcer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.net.toUri

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_about)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_about)) { v, insets ->
            val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton)

        val icon = findViewById<ImageView>(R.id.github_icon)
        icon.setOnClickListener { v: View? ->
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://www.github.com/spewedprojects/MeditationTracker".toUri()
            )
            v!!.context.startActivity(intent)
        }
    }
}
