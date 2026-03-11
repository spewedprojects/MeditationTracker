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

    private var radiiExpanded = false // Standard variable

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("radii_expanded", radiiExpanded)
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        // --- ADD THIS ---
        // Restore the state if coming from recreate(), otherwise default to false
        radiiExpanded = savedInstanceState?.getBoolean("radii_expanded", false) ?: false

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
        setupWeekStartToggle()
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

    private fun setupWeekStartToggle() {
        val dayToggleGroup = findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.switchDay)
        val prefs = getSharedPreferences(BaseActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        val sunBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.set_Sun_btn)
        val monBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.set_Mon_btn)
        val subtitleContainer = findViewById<ViewGroup>(R.id.weekStartTextContainer)
        val subtitle = subtitleContainer?.getChildAt(1) as? TextView

        val isSunday = prefs.getBoolean("week_start_sun", false)
        val activeStroke = resources.getDimensionPixelSize(R.dimen.active_stroke_width)

        if (isSunday) {
            dayToggleGroup.check(R.id.set_Sun_btn)
            sunBtn.strokeWidth = activeStroke
            monBtn.strokeWidth = 0
            subtitle?.text = "Sunday"
        } else {
            dayToggleGroup.check(R.id.set_Mon_btn)
            monBtn.strokeWidth = activeStroke
            sunBtn.strokeWidth = 0
            subtitle?.text = "Monday"
        }

        dayToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val setSunday = (checkedId == R.id.set_Sun_btn)
                prefs.edit { putBoolean("week_start_sun", setSunday) }
                
                if (setSunday) {
                    sunBtn.strokeWidth = activeStroke
                    monBtn.strokeWidth = 0
                    subtitle?.text = "Sunday"
                } else {
                    monBtn.strokeWidth = activeStroke
                    sunBtn.strokeWidth = 0
                    subtitle?.text = "Monday"
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

        val prefs = getSharedPreferences(BaseActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        // Guard against nulls
        if (colExpButton == null || radiiContainer == null) return

        if (radiiExpanded) {
            radiiContainer.visibility = View.VISIBLE
            colExpButton.rotation = 180f
        } else {
            radiiContainer.visibility = View.GONE
            colExpButton.rotation = 0f
        }

        // Click listener to toggle visibility and rotate the button
        colExpButton.setOnClickListener {
            radiiExpanded = !radiiExpanded

            TransitionManager.beginDelayedTransition(cardLinearLayout, AutoTransition())

            if (radiiExpanded) {
                // Expand: make visible and rotate to 180 degrees
                radiiContainer.visibility = View.VISIBLE
                colExpButton.animate().rotation(180f).setDuration(150).start()
            } else {
                // Collapse: hide and rotate back to 0 degrees
                radiiContainer.visibility = View.GONE
                colExpButton.animate().rotation(0f).setDuration(150).start()
            }
        }

        // --- Slider Logic ---
        val cardSlider = findViewById<com.google.android.material.slider.Slider>(R.id.setcard_radius_slider)
        val btnSlider = findViewById<com.google.android.material.slider.Slider>(R.id.setbtn_radius_slider)

        // Guard
        if (cardSlider == null || btnSlider == null) return

        // Set initial values (Make sure to remove the old addOnChangeListener blocks!)
        cardSlider.value = prefs.getFloat("custom_card_radius", 12f)
        btnSlider.value = prefs.getFloat("custom_btn_radius", 10f)

        // Only save and recreate when the user lets go of the slider
        cardSlider.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                // Do nothing while dragging
            }

            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                prefs.edit { putFloat("custom_card_radius", slider.value) }
                recreate() // Recreate exactly once when they drop it
            }
        })

        btnSlider.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}

            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                prefs.edit { putFloat("custom_btn_radius", slider.value) }
                recreate()
            }
        })
    }
}
