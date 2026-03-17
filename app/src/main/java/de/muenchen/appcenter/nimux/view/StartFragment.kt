package de.muenchen.appcenter.nimux.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.util.faceRecognitionPrefKey

/**
 * This fragment is used as entrypoint for the navigation if a user is logged in. Depending on the app-wide settings the user
 * is redirected to the face recognition fragment or directly to the user selection (home products).
 */
class StartFragment : Fragment(R.layout.fragment_start) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val root = view.findViewById<View>(R.id.rootLayout)

        root.animate()
            .alpha(1f)
            .setDuration(400)
            .start()

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val faceRecognitionEnabled =
            sharedPref.getBoolean(faceRecognitionPrefKey, false)

        val navController = findNavController()

        if (faceRecognitionEnabled) {
            navController.navigate(R.id.action_start_to_auto)
        } else {
            navController.navigate(R.id.action_start_to_manual)
        }
    }
}