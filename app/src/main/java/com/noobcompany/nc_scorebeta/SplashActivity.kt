package com.noobcompany.nc_scorebeta

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * The initial activity displayed when the app launches.
 *
 * It displays a splash screen for a defined duration (2 seconds) before automatically navigating to the [MainActivity].
 * This provides a brief branding moment and allows for any necessary initial background setup (though currently none is performed).
 */
class SplashActivity : AppCompatActivity() {
    /**
     * Called when the activity is first created.
     *
     * It sets the content view to the splash layout and posts a delayed runnable to the main looper.
     * The runnable starts [MainActivity] and finishes [SplashActivity] after 2000 milliseconds.
     *
     * @param savedInstanceState If non-null, this activity is being re-constructed from a previous saved state.
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
