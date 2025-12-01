package com.noobcompany.nc_scorebeta

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * The entry point Activity for the application (Splash Screen).
 *
 * This activity is the first screen shown to the user. It displays the application branding
 * with a zoom-in animation (Netflix style) and automatically navigates to the main dashboard
 * after a fixed delay.
 */
class SplashActivity : AppCompatActivity() {
    /**
     * Initializes the splash screen UI and timer.
     *
     * 1. Sets the content view.
     * 2. Loads and starts the "netflix_scale" animation on the logo container.
     * 3. Posts a delayed Runnable to the main thread's message queue to switch activities after 2500ms.
     *
     * @param savedInstanceState Saved state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Start Netflix-style Animation
        val logoContainer = findViewById<LinearLayout>(R.id.logoContainer)
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.netflix_scale)
        logoContainer.startAnimation(zoomIn)

        // Delay for 2.5 seconds then go to Main
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2500)
    }
}
