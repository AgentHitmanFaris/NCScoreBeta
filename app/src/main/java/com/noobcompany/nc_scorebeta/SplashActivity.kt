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
 * It shows a splash screen for a few seconds before navigating to the [MainActivity].
 */
class SplashActivity : AppCompatActivity() {
    /**
     * Called when the activity is first created.
     *
     * Sets the content view and posts a delayed runnable to start the [MainActivity].
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
