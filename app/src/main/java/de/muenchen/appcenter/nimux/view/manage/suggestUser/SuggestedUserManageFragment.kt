package de.muenchen.appcenter.nimux.view.manage.suggestUser

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentSuggestedUserManageBinding
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.viewmodel.manage.users.ManageUsersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SuggestedUserManageFragment : Fragment() {

    private val viewModel: ManageUsersViewModel by viewModels()
    private var _binding: FragmentSuggestedUserManageBinding? = null
    private val binding get() = _binding!!
    private lateinit var user: User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSuggestedUserManageBinding.inflate(inflater, container, false)
        user = SuggestedUserManageFragmentArgs.fromBundle(requireArguments()).suggestedUser
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textViewName.text = user.name
        binding.switchCredit.isChecked = user.showCredit
        binding.switchData.isChecked = user.collectData
        binding.switchPin.isChecked = user.pin != null

        binding.buttonDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete_suggestion_alert_title))
                .setMessage(getString(R.string.delete_suggestion_alert_message))
                .setPositiveButton(
                    getString(R.string.yes),
                    DialogInterface.OnClickListener { _, _ ->
                        viewModel.deleteUserSuggestion(user.stringSortID)
                        findNavController().popBackStack()
                    })
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }
        binding.buttonApprove.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                if (viewModel.userSuggestionApproved(user)) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        findNavController().popBackStack()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.user_suggestion_approved_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.user_approval_error_name_existstoast),
                            Toast.LENGTH_LONG
                        ).show()

                    }
                }
            }
        }
    }
}