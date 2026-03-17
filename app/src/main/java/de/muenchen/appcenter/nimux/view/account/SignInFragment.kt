package de.muenchen.appcenter.nimux.view.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.MainActivity
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentSigninBinding
import de.muenchen.appcenter.nimux.repositories.OtherRepository
import de.muenchen.appcenter.nimux.util.LogInLogOutLog
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.viewmodel.account.SignInViewModel
import javax.inject.Inject
import androidx.core.net.toUri

@AndroidEntryPoint
class SignInFragment : Fragment() {

    private lateinit var binding: FragmentSigninBinding
    private val viewModel: SignInViewModel by viewModels()
    @Inject
    lateinit var otherRepository: OtherRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_signin, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        observeLogin()
        observeCreateError()
        observeLoginError()
        observeEmailEmpty()
        observePwEmpty()

        viewModel.showProgressBar.observe(viewLifecycleOwner) {
            if (it) {
                binding.progressBar.show()
                binding.createAccountButton.isEnabled = false
                binding.loginFragmentLoginButton.isEnabled = false
            } else {
                binding.progressBar.hide()
                binding.createAccountButton.isEnabled = true
                binding.loginFragmentLoginButton.isEnabled = true
            }
        }

        viewModel.showAccountCreationHint.observe(viewLifecycleOwner) {
            if (it) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Accounterstellung erfolgreich")
                    .setMessage("Bitte informiere die Admins, dass Du einen Account angelegt hast. Im Anschluss wird dir die richtige Abteilung und Rolle zugewiesen. Dann kannst du dich anmelden.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                viewModel.resetAccountCreationHintToFalse()
            }
        }

        viewModel.showAccountCreationIsDisabledHint.observe(viewLifecycleOwner) {
            if (it) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Accounterstellung deaktiviert")
                    .setMessage("Diese Funktion ist durch die Admins deaktivert.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                viewModel.resetAccountCreationIsDisabledHintToFalse()
            }
        }

        return binding.root
    }

    private fun observeLogin() {
        viewModel.userLoggedIn.observe(viewLifecycleOwner) {
            if (it) {
                otherRepository.addLoginLogoutEntry(LogInLogOutLog(true))
                Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                hideKeyboard(requireActivity())
                requireActivity().invalidateOptionsMenu()
                val intent = Intent(requireContext(), MainActivity::class.java)
                requireActivity().finish()
                startActivity(intent)
            }
        }
        viewModel.showPrivacyDialog.observe(viewLifecycleOwner) { show ->

            if (show) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Datenschutzhinweis")
                    .setMessage(
                        "Zur Anmeldung wird Firebase Authentication verwendet. " +
                                "Dabei können Ihre E-Mail-Adresse sowie technische Informationen " +
                                "wie IP-Adresse und Zeitpunkte von Anmeldeversuchen verarbeitet werden.\n\n" +
                                "Weitere Informationen finden Sie in der Datenschutzerklärung."
                    )
                    .setNegativeButton("Abbrechen") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNeutralButton("Datenschutzerklärung") { _, _ ->

                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://it‑at‑m.github.io/nimux/privacy.html".toUri()
                            )
                        )

                    }
                    .setPositiveButton("Weiter zum Login") { dialog, _ ->
                        dialog.dismiss()
                        viewModel.loginConfirmed()
                    }
                    .show()

            }

        }
    }


    private fun observeLoginError() {
        viewModel.loginError.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_error_login) + viewModel.toastMessage,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.loginErrorShown()
            }
        }
    }

    private fun observeCreateError() {
        viewModel.createAccError.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_error_create_acc) + viewModel.toastMessage,
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.createErrorShown()
            }
        }
    }

    private fun observeEmailEmpty() {
        viewModel.emailEmpty.observe(viewLifecycleOwner) {
            if (it) {
                binding.loginFragmentEmailInput.error = getString(R.string.field_cant_be_empty)
            } else {
                binding.loginFragmentEmailInput.error = null
            }
        }
    }

    private fun observePwEmpty() {
        viewModel.pwEmpty.observe(viewLifecycleOwner) {
            if (it) {
                binding.loginFragmentPwInput.error = getString(R.string.field_cant_be_empty)
            } else {
                binding.loginFragmentPwInput.error = null
            }
        }
    }
}

