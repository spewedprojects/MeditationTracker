package com.gratus.meditationtrakcer

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.material3.* // Imports MaterialTheme, Card, Text, Switch, etc.

class SettingsInfoActivity : BaseActivity(){

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_settingsinfo)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsInfo)) { v, insets ->
            val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the toolbar and menu button
        setupToolbar(R.id.toolbar2, R.id.menubutton)

        setupComposeView()
    }


    fun setupComposeView(){
        // 2. Find the ComposeView and set the content
        val composeView = findViewById<ComposeView>(R.id.settingsCompose)

        composeView.apply {
            // Dispose strategy is crucial for mixed Views/Compose
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                // MaterialTheme is needed for colors/typography to work correctly
                MaterialTheme {
                    SettingsScreenContent()
                }
            }
        }
    }
}
