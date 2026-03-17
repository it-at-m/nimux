package de.muenchen.appcenter.nimux

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.core.view.get
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.util.UserSessionManager
import de.muenchen.appcenter.nimux.util.faceRecognitionPrefKey
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.util.standbyBoolPrefKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    @Inject
    lateinit var sessionManager: UserSessionManager
    private lateinit var auth: FirebaseAuth
    private lateinit var timer: CountDownTimer
    val countDownTime = 45000
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        prepareEnterTransition()
        super.onCreate(savedInstanceState)
        goFullScreen()
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.main_act_content).setOnApplyWindowInsetsListener { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsets.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom
            }
            windowInsets
        }

        timer = object : CountDownTimer((countDownTime).toLong(), 1000) {
            override fun onTick(p0: Long) {
                Timber.Forest.d(p0.toString())
                if (p0 in 9001..9999)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.standby_ten_seconds_toast),
                        Toast.LENGTH_SHORT
                    ).show()
            }

            override fun onFinish() {
                val intent = Intent(this@MainActivity, InActivity::class.java)
                startActivity(intent)
            }
        }
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController


        // after successful login the tenantId the user belongs to should be available
        val tenantId = sessionManager.getTenantId()
        // Check if user is signed in (non-null) and update UI accordingly.
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        //User is logged in
        if (currentUser != null && tenantId != null) {
            Timber.Forest.d("User with uid ${currentUser.uid} and email ${currentUser.email} belongs to tenant: $tenantId")
            buildUI()
            lifecycleScope.launch(Dispatchers.IO) {
                // user is logged in but has no access or admin role set
                if (!sessionManager.hasAdminRole() && !sessionManager.hasAccessRole()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        navController.navigate(R.id.noAccessFragment)
                    }
                }
            }
        } else {
            Timber.Forest.d("user not logged in")
            //user not logged in, show custom login ui
            buildLogIn()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val metrics = resources.displayMetrics
        val yInches = metrics.heightPixels / metrics.ydpi
        val xInches = metrics.widthPixels / metrics.xdpi
        val diagonalInches = sqrt((xInches * xInches + yInches * yInches).toDouble())
        if (diagonalInches >= 7)
            menuInflater.inflate(R.menu.app_bar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isLoggedIn =
            sessionManager.getTenantId() != null // user is logged in if tenant id is set

        menu.findItem(R.id.menu_action_help)?.isVisible = isLoggedIn
        menu.findItem(R.id.menu_action_feedback)?.isVisible = isLoggedIn

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // if the lock symbol inside the app is pressed, show InActivity and hide all system controls
            R.id.menu_action_lock -> {
                val intent = Intent(this@MainActivity, InActivity::class.java)
                startActivity(intent)
            }

            R.id.menu_action_help -> {
                navController.navigate(R.id.helpFragment)
            }

            R.id.menu_action_feedback -> navController.navigate(R.id.nav_feedback)
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("CutPasteId")
    private fun buildUI() {
        val metrics = resources.displayMetrics
        val yInches = metrics.heightPixels / metrics.ydpi
        val xInches = metrics.widthPixels / metrics.xdpi
        val diagonalInches = sqrt((xInches * xInches + yInches * yInches).toDouble())
        if (diagonalInches >= 7) {
            // 6.5inch device or bigger
            //val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
            val navGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
            if (PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(faceRecognitionPrefKey, false)
            ) {
                navGraph.setStartDestination(R.id.nav_home_auto)
            } else navGraph.setStartDestination(R.id.nav_home_manual)

            navController.graph = navGraph

            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_home_auto,
                    R.id.nav_home_manual,
                    R.id.nav_overview,
                    R.id.nav_statistics,
                    R.id.nav_user_store,
                    R.id.nav_manage_stuff,
                    R.id.nav_suggest_users
                )
            )

            setupActionBarWithNavController(navController, appBarConfiguration)

            val navRail = findViewById<NavigationRailView>(R.id.nav_view)
            navRail.setOnItemSelectedListener { menuItem ->
                val navBuilder = NavOptions.Builder()
                val options = navBuilder.setPopUpTo(
                    navController.graph.id,
                    inclusive = true,
                    saveState = false
                ).build()
                when (menuItem.itemId) {
                    R.id.nav_home -> {

                        val sharedPref =
                            PreferenceManager.getDefaultSharedPreferences(this)

                        val faceRecognitionEnabled =
                            sharedPref.getBoolean(faceRecognitionPrefKey, false)

                        val destination = if (faceRecognitionEnabled) {
                            R.id.nav_home_auto
                        } else {
                            R.id.nav_home_manual
                        }

                        if (navController.currentDestination?.id != destination) {
                            navController.navigate(destination, null, options)
                        }
                    }

                    R.id.nav_overview -> {
                        navController.navigate(R.id.nav_overview, null, options)
                    }

                    R.id.nav_statistics -> {
                        navController.navigate(R.id.nav_statistics, null, options)
                    }

                    R.id.nav_manage_stuff -> {
                        navController.navigate(R.id.nav_manage_stuff, null, options)
                    }

                    R.id.nav_settings -> {
                        navController.navigate(R.id.nav_settings)
                    }

                    R.id.nav_user_store -> {
                        navController.navigate(R.id.nav_user_store, null, options)
                    }

                    R.id.nav_suggest_users -> {
                        navController.navigate(R.id.nav_suggest_users, null, options)
                    }
                }
                true
            }

            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.nav_home_auto)
                    navRail.menu[0].isChecked = true
                if (destination.id == R.id.nav_home_manual)
                    navRail.menu[0].isChecked = true
                if (destination.id == R.id.nav_overview)
                    navRail.menu[1].isChecked = true
                if (destination.id == R.id.nav_statistics)
                    navRail.menu[2].isChecked = true
                if (destination.id == R.id.nav_user_store)
                    navRail.menu[3].isChecked = true
                if (destination.id == R.id.nav_suggest_users)
                    navRail.menu[4].isChecked = true
                if (destination.id == R.id.nav_manage_stuff)
                    navRail.menu[5].isChecked = true
                if (destination.id == R.id.nav_settings)
                    navRail.menu[6].isChecked = true
                timerRestart()
            }

        } else {
            // smaller device
            val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
            val navView: NavigationView = findViewById(R.id.nav_view)
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            val navGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
            navGraph.setStartDestination(R.id.suggUserPasswordFragment)
            navController.graph = navGraph

            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.suggestedUsersFragment,
                    R.id.suggUserPasswordFragment,
                    R.id.nav_overview,
                    R.id.nav_statistics,
                    R.id.nav_user,
                    R.id.nav_products,
                    R.id.manageUsersFragment,
                    R.id.manageProductsFragment,
                    R.id.nav_history,
                    R.id.historyListFragment,
                    R.id.nav_user_store
                ), drawerLayout
            )

            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)

            navController.addOnDestinationChangedListener { _, destination, _ ->
                markCorrectNavDrawerItems(destination.id, navView)
                timerRestart()
            }
        }
        //TODO: check if necessary
        val db = Firebase.firestore
        db.enableNetwork()
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        hideKeyboard(this)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("MainActivity is started")
    }

    /**
     * If the user is not logged in, set a new navgraph and navigate to the SignInFragment.
     */
    private fun buildLogIn() {
        val navGraph = navController.navInflater.inflate(R.navigation.signin_navigation)
        navController.graph = navGraph
        setupActionBarWithNavController(navController)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        timerRestart()
    }

    override fun onResume() {
        super.onResume()
        timerRestart()

    }

    private fun timerRestart() {
        timer.cancel()
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(standbyBoolPrefKey, false)
        )
            timer.start()
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }


    private fun goFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun markCorrectNavDrawerItems(destId: Int, navView: NavigationView) {
        val overviewNum = 0
        val statisticNum = 1
        val suggestionNum = 2
        val manageUserNum = 3
        val manageProdNum = 4
        val historyNum = 5
        val storeNum = 6
        val settingsNum = 7

        when (destId) {

            R.id.suggUserPasswordFragment -> navView.menu[suggestionNum].isChecked = true
            R.id.suggestedUsersFragment -> navView.menu[suggestionNum].isChecked = true
            R.id.suggestedUserManageFragment -> navView.menu[suggestionNum].isChecked = true

            R.id.nav_overview -> navView.menu[overviewNum].isChecked = true

            R.id.nav_statistics -> navView.menu[statisticNum].isChecked = true

            R.id.nav_user -> navView.menu[manageUserNum].isChecked = true
            R.id.manageUsersFragment -> navView.menu[manageUserNum].isChecked = true
            R.id.manageUserItem -> navView.menu[manageUserNum].isChecked = true
            R.id.addUserFragment -> navView.menu[manageUserNum].isChecked = true

            R.id.nav_products -> navView.menu[manageProdNum].isChecked = true
            R.id.manageProductsFragment -> navView.menu[manageProdNum].isChecked = true
            R.id.productItemFragment -> navView.menu[manageProdNum].isChecked = true
            R.id.editProductFragment -> navView.menu[manageProdNum].isChecked = true
            R.id.addProductFragment -> navView.menu[manageProdNum].isChecked = true

            R.id.nav_history -> navView.menu[historyNum].isChecked = true
            R.id.historyListFragment -> navView.menu[historyNum].isChecked = true

            R.id.nav_user_store -> navView.menu[storeNum].isChecked = true

            R.id.nav_settings -> navView.menu[settingsNum].isChecked = true
        }
    }

    private fun prepareEnterTransition() {
        findViewById<View>(android.R.id.content).transitionName =
            "inactivity_to_main_transition"
        window.sharedElementEnterTransition = MaterialFadeThrough().apply {
            addTarget(android.R.id.content)
            duration = resources.getInteger(R.integer.motion_medium).toLong()
            interpolator = FastOutSlowInInterpolator()
        }
    }

}