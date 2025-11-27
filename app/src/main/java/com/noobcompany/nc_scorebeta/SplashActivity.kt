package com.noobcompany.nc_scorebeta

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

        // Delay for 2 seconds then go to Main
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}
