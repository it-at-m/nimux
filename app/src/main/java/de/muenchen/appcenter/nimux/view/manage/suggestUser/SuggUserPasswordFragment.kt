package de.muenchen.appcenter.nimux.view.manage.suggestUser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentSuggUserPasswordBinding
import de.muenchen.appcenter.nimux.util.UserSessionManager
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.viewmodel.PasswordViewModel
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class SuggUserPasswordFragment : Fragment() {

    private val viewModel: PasswordViewModel by viewModels()
    private var _binding : FragmentSuggUserPasswordBinding? = null
    @Inject lateinit var sessionManager: UserSessionManager
    private val binding get() = _binding!!
    val action = SuggUserPasswordFragmentDirections.actionSuggUserPasswordFragmentToSuggestedUsersFragment()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launchWhenStarted {
            if (sessionManager.hasAdminRole()) {
                findNavController().navigate(action)
            }
        }
        _binding = FragmentSuggUserPasswordBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel

        viewModel.pwCorrect.observe(viewLifecycleOwner) {
            when (it) {
                0 -> {
                    binding.passwordInputLayout.error = getString(R.string.pw_error)
                }
                1 -> {
                    hideKeyboard(requireActivity())
                    findNavController().navigate(action)
                    viewModel.pwWasCorrect()
                }
                2 -> {
                    binding.passwordInputLayout.error = null
                }
            }
        }
    }
}