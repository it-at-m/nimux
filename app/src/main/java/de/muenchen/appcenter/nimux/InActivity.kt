package de.muenchen.appcenter.nimux

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import de.muenchen.appcenter.nimux.InActivity.Companion.AUTO_HIDE
import de.muenchen.appcenter.nimux.InActivity.Companion.AUTO_HIDE_DELAY_MILLIS
import de.muenchen.appcenter.nimux.databinding.ActivityInBinding
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * It is used for "stand-by" mode of the app. Black screen and preventing device screen to go in
 * real "stand-by.
 */
class InActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInBinding
    private lateinit var fullscreenContent: FrameLayout
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler()
    private lateinit var cameraExecutor: ExecutorService

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
//        backToMain()
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        if (resources.configuration.smallestScreenWidthDp >= 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = binding.fullscreenContent

        fullscreenContent.setOnTouchListener { view, event ->
            val x = event?.x?.toInt() ?: 0
            val y = event?.y?.toInt() ?: 0
            backToMainTouch(x, y)
            false
        }
        fullscreenContentControls = binding.fullscreenContentControls

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }

    private fun backToMainTouch(x: Int, y: Int) {

        val metrics = resources.displayMetrics
        val fullWidth = metrics.widthPixels
        val spaceRight = fullWidth - x
        val biggerWidth = if (spaceRight > x) spaceRight else x

        Timber.d(
            "Success! $x $y $spaceRight ${binding.touchAnimationHelperView.width}"
        )
        Log.d("backToMainTouch", "$fullWidth $biggerWidth $spaceRight $x")

        val circularReveal = ViewAnimationUtils.createCircularReveal(
            binding.touchAnimationHelperView,
            x,
            y,
            00f,
            biggerWidth.times(1.4).toFloat()
        )
        circularReveal.apply {
            duration = resources.getInteger(R.integer.motion_medium).toLong()
            interpolator = AccelerateDecelerateInterpolator()
        }
        binding.touchAnimationHelperView.visibility = View.VISIBLE
        circularReveal.start()

        // Navigation from InActivity to the MainActivity
        binding.touchAnimationHelperView.postDelayed({
            val intent = Intent(this@InActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            val options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                binding.touchAnimationHelperView,
                "inactivity_to_main_transition"
            )
            startActivity(intent, options.toBundle())
        }, resources.getInteger(R.integer.motion_medium).div(2).toLong())
    }

}