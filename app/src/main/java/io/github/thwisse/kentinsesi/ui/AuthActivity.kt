package io.github.thwisse.kentinsesi.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.thwisse.kentinsesi.databinding.ActivityAuthBinding
import io.github.thwisse.kentinsesi.util.LocaleHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        syncSystemBarsWithToolbar()
        setupToggleButtons()
    }

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMain()
        }
    }

    private fun setupToggleButtons() {
        updateToggleButtonTexts()

        binding.btnLanguageToggle.setOnClickListener {
            val current = LocaleHelper.getPersistedLanguage(this)
            val effectiveLang = if (current == LocaleHelper.LANGUAGE_SYSTEM) {
                LocaleHelper.getEffectiveLocale(this).language
            } else {
                current
            }
            val newLang = if (effectiveLang == LocaleHelper.LANGUAGE_TURKISH) {
                LocaleHelper.LANGUAGE_ENGLISH
            } else {
                LocaleHelper.LANGUAGE_TURKISH
            }
            LocaleHelper.setLocaleAndRestart(this, newLang)
        }

        binding.btnThemeToggle.setOnClickListener {
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val current = prefs.getString("theme_mode", "system")
            val newTheme = if (current == "dark") "light" else "dark"
            prefs.edit().putString("theme_mode", newTheme).apply()
            when (newTheme) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            recreate()
        }
    }

    private fun updateToggleButtonTexts() {
        val currentLang = LocaleHelper.getPersistedLanguage(this)
        val effectiveLang = if (currentLang == LocaleHelper.LANGUAGE_SYSTEM) {
            LocaleHelper.getEffectiveLocale(this).language
        } else {
            currentLang
        }
        binding.btnLanguageToggle.text = if (effectiveLang == LocaleHelper.LANGUAGE_TURKISH) "EN" else "TR"

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currentTheme = prefs.getString("theme_mode", "system")
        val isDark = when (currentTheme) {
            "dark" -> true
            "light" -> false
            else -> isNightMode()
        }
        binding.btnThemeToggle.text = if (isDark) "☀️" else "🌙"
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun syncSystemBarsWithToolbar() {
        val primary = ContextCompat.getColor(this, io.github.thwisse.kentinsesi.R.color.colorPrimary)
        window.statusBarColor = primary
        window.navigationBarColor = primary

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val isDark = isNightMode()
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }

    private fun isNightMode(): Boolean {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
