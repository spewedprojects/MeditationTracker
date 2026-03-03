package com.gratus.meditationtrakcer

import android.content.Context
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.content.edit
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import com.google.android.material.materialswitch.MaterialSwitch

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

        setupYAxisSwitch()
        setupFontToggle()
        setupCustomRadiiBlock()
    }

    private fun setupYAxisSwitch() {
        val yAxisSwitch = findViewById<MaterialSwitch>(R.id.switchYAxis)
        if (yAxisSwitch != null) {
            val prefs = getSharedPreferences(BaseActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val isVisible = prefs.getBoolean("y_axis_visible", true) // Default true

            yAxisSwitch.isChecked = isVisible

            yAxisSwitch.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit { putBoolean("y_axis_visible", isChecked) }
            }
        }
    }

    private fun setupFontToggle() {
        val fontToggleGroup = findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.switchFont)
        val prefs = getSharedPreferences(BaseActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        val systemBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.set_system_font_btn)
        val customBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.set_custom_font_btn)
        val subtitle = findViewById<TextView>(R.id.fontSubtitle)

        // Check current preference
        val useSystemFont = prefs.getBoolean("use_system_font", false)

        if (useSystemFont) {
            fontToggleGroup.check(R.id.set_system_font_btn)
            systemBtn.strokeWidth = resources.getDimensionPixelSize(R.dimen.active_stroke_width)
            customBtn.strokeWidth = 0
            subtitle.text = "Using System font"
        } else {
            fontToggleGroup.check(R.id.set_custom_font_btn)
            customBtn.strokeWidth = resources.getDimensionPixelSize(R.dimen.active_stroke_width)
            systemBtn.strokeWidth = 0
            subtitle.text = "Using App Font"
        }

        fontToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val enableSystemFont = (checkedId == R.id.set_system_font_btn)
                prefs.edit { putBoolean("use_system_font", enableSystemFont) }

                if (enableSystemFont) {
                    systemBtn.strokeWidth = resources.getDimensionPixelSize(R.dimen.active_stroke_width)
                    customBtn.strokeWidth = 0
                    subtitle.text = "Using System font"
                } else {
                    customBtn.strokeWidth = resources.getDimensionPixelSize(R.dimen.active_stroke_width)
                    systemBtn.strokeWidth = 0
                    subtitle.text = "Using App Font"
                }

                recreate()
            }
        }
    }

    private fun setupCustomRadiiBlock(){
        // 1. Initialize the new Views
        val colExpButton = findViewById<ImageButton?>(R.id.btn_expand_radius_options)
        val radiiContainer = findViewById<ViewGroup?>(R.id.customRadiiContainer)


        // Prepare the smooth transition (AutoTransition handles layout changes automatically)
        val cardLinearLayout = findViewById<ViewGroup?>(R.id.settingscard_const_layout)
        TransitionManager.beginDelayedTransition(cardLinearLayout, AutoTransition())

        // Guard against nulls
        if (colExpButton == null || radiiContainer == null) return

        // Ensure initial state
        radiiContainer.visibility = View.GONE
        colExpButton.rotation = 0f

        // Click listener to toggle visibility and rotate the button
        colExpButton.setOnClickListener {
            val isCollapsed = radiiContainer.isGone

            if (isCollapsed) {
                // Expand: make visible and rotate to 180 degrees
                radiiContainer.visibility = View.VISIBLE
                colExpButton.animate()
                    .rotation(180f)
                    .setDuration(150)
                    .start()
            } else {
                // Collapse: hide and rotate back to 0 degrees
                radiiContainer.visibility = View.GONE
                colExpButton.animate()
                    .rotation(0f)
                    .setDuration(150)
                    .start()
            }
        }

    }
}
