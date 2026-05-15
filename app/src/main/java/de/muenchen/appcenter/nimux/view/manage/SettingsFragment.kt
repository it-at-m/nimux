package de.muenchen.appcenter.nimux.view.manage

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.BuildConfig
import de.muenchen.appcenter.nimux.MainActivity
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentSettingsBinding
import de.muenchen.appcenter.nimux.repositories.OtherRepository
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.LogInLogOutLog
import de.muenchen.appcenter.nimux.util.UserSessionManager
import de.muenchen.appcenter.nimux.util.faceRecognitionPrefKey
import de.muenchen.appcenter.nimux.util.standbyBoolPrefKey
import de.muenchen.appcenter.nimux.util.systemColorPrefKey // <-- NEU
import javax.inject.Inject
import androidx.core.content.edit

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject
    lateinit var userSessionManager: UserSessionManager

    @Inject
    lateinit var usersRepository: UsersRepository

    @Inject
    lateinit var otherRepository: OtherRepository

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(layoutInflater, container, false)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsBuildNumber.text = "App Version " + BuildConfig.VERSION_NAME
        binding.settingsAccountLoggedIn.text =
            "Eingeloggt als " + userSessionManager.getUserEMail() + " mit Rolle " + userSessionManager.getRole() + " und Daten aus Tenant: " + userSessionManager.getTenantId()
        setClickListeners()
        getSavedData()
        setupColorSelection()
    }

    private fun setClickListeners() {
        binding.settingsLogoutCard.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sign_out)
                .setMessage(getString(R.string.sign_out_alert_message))
                .setPositiveButton(R.string.yes) { _, _ ->
                    otherRepository.addLoginLogoutEntry(LogInLogOutLog(false))
                        .addOnSuccessListener {
                            performLogOut()
                        }
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
        binding.settingsChangePw.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                if (userSessionManager.hasAdminRole()) {
                    findNavController().navigate(SettingsFragmentDirections.actionNavSettingsToChangeMainPwFragment())
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.no_admin_toast),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun performLogOut() {
        userSessionManager.clearSession()
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), MainActivity::class.java)
        requireActivity().finish()
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600

        if (isTablet) {
            (activity as AppCompatActivity)
                .supportActionBar
                ?.setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun getSavedData() {
        binding.settingsStandbySwitch.isChecked =
            sharedPrefs.getBoolean(standbyBoolPrefKey, false)
        binding.settingsScanFaceSwitch.isChecked =
            sharedPrefs.getBoolean(faceRecognitionPrefKey, false)

        binding.settingsStandbySwitch.setOnCheckedChangeListener { _, b ->
            with(sharedPrefs.edit()) {
                putBoolean(standbyBoolPrefKey, b)
                apply()
            }
        }
        binding.settingsScanFaceSwitch.setOnCheckedChangeListener { _, b ->
            with(sharedPrefs.edit()) {
                putBoolean(faceRecognitionPrefKey, b)
                apply()
            }
        }
    }

    private fun setupColorSelection() {
        val savedColor = sharedPrefs.getString(systemColorPrefKey, "blue") ?: "blue"
        updateColorSelectionUI(savedColor)

        binding.colorOptionBlue.setOnClickListener { selectColor("blue") }
        binding.colorOptionRed.setOnClickListener { selectColor("red") }
        binding.colorOptionGreen.setOnClickListener { selectColor("green") }
        binding.colorOptionOrange.setOnClickListener { selectColor("orange") }
        binding.colorOptionPurple.setOnClickListener { selectColor("purple") }
    }

    private fun selectColor(colorName: String) {
        sharedPrefs.edit {
            putString(systemColorPrefKey, colorName)
        }

        requireActivity().recreate()
    }

    private fun updateColorSelectionUI(selectedColor: String) {
        //calculate radius
        val strokeWidthActive = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            3f,
            resources.displayMetrics
        ).toInt()
        val strokeWidthInactive = 0

        binding.colorOptionBlue.strokeWidth = strokeWidthInactive
        binding.colorOptionRed.strokeWidth = strokeWidthInactive
        binding.colorOptionGreen.strokeWidth = strokeWidthInactive
        binding.colorOptionOrange.strokeWidth = strokeWidthInactive
        binding.colorOptionPurple.strokeWidth = strokeWidthInactive

        when (selectedColor) {
            "blue" -> binding.colorOptionBlue.strokeWidth = strokeWidthActive
            "red" -> binding.colorOptionRed.strokeWidth = strokeWidthActive
            "green" -> binding.colorOptionGreen.strokeWidth = strokeWidthActive
            "orange" -> binding.colorOptionOrange.strokeWidth = strokeWidthActive
            "purple" -> binding.colorOptionPurple.strokeWidth = strokeWidthActive
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}