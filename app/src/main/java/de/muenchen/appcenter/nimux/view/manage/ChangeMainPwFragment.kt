package de.muenchen.appcenter.nimux.view.manage

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentChangeMainPwBinding
import de.muenchen.appcenter.nimux.repositories.OtherRepository
import javax.inject.Inject

@AndroidEntryPoint
class ChangeMainPwFragment : Fragment() {

    @Inject lateinit var otherDataRepository: OtherRepository

    private var _binding: FragmentChangeMainPwBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChangeMainPwBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.confirmButton.setOnClickListener {
            checkAndSetNewMainPw()
        }
    }

    private fun checkAndSetNewMainPw() {
        binding.confirmButton.isEnabled =false
        val oldPwInput = binding.oldPwInput.editText?.text.toString()
        val newPwInput = binding.newPwInput.editText?.text.toString()
        val confirmPwInput = binding.confirmNewPwInput.editText?.text.toString()

        if (oldPwInput.isBlank()) binding.oldPwInput.error = getString(R.string.field_cant_be_empty)
        else binding.oldPwInput.error = null

        if (newPwInput.isBlank()) binding.newPwInput.error = getString(R.string.field_cant_be_empty)
        else binding.newPwInput.error = null

        if (confirmPwInput.isBlank()) binding.confirmNewPwInput.error =
            getString(R.string.field_cant_be_empty)
        else binding.confirmNewPwInput.error = null

        if (oldPwInput.isNotBlank() && newPwInput.isNotBlank() && confirmPwInput.isNotBlank()) {
            if (newPwInput != confirmPwInput) binding.confirmNewPwInput.error =
                getString(R.string.pw_no_match_error)
            else {
                binding.confirmNewPwInput.error = null
                otherDataRepository.getManagePWDocContent().get().addOnCompleteListener {
                    if (it.isSuccessful && it.result.get("pw") == oldPwInput) {
                        binding.oldPwInput.error = null
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.change_pw_alert_title))
                            .setMessage(getString(R.string.change_pw_alert_message))
                            .setNegativeButton(getString(R.string.no), null)
                            .setPositiveButton(getString(R.string.yes),
                                DialogInterface.OnClickListener { _, _ ->
                                    otherDataRepository.getManagePWDocContent().set(PassWordDoc(newPwInput))
                                        .addOnCompleteListener { newPwTask ->
                                            if (newPwTask.isSuccessful) {
                                                Toast.makeText(requireContext(),
                                                    getString(R.string.pw_change_success_toast),
                                                    Toast.LENGTH_SHORT).show()
                                                findNavController().popBackStack()
                                            } else {
                                                MaterialAlertDialogBuilder(requireContext())
                                                    .setTitle(getString(R.string.pw_change_failed_title))
                                                    .setMessage(getString(R.string.pw_change_failed_message))
                                                    .show()
                                            }
                                        }
                                })
                            .show()
                    } else binding.oldPwInput.error = getString(R.string.password_incorrect_error)
                }
            }
        }
        binding.confirmButton.isEnabled = true
    }
}

data class PassWordDoc(
    val pw : String=""
)