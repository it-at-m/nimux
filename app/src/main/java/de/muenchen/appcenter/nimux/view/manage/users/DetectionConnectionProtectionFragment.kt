package de.muenchen.appcenter.nimux.view.manage.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentDetectionConnectionProtectionBinding
import de.muenchen.appcenter.nimux.util.UserSessionManager
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.viewmodel.PasswordViewModel
import javax.inject.Inject
import kotlin.getValue

class DetectionConnectionProtectionFragment : Fragment() {

    private val viewModel: PasswordViewModel by viewModels()
    @Inject lateinit var sessionManager: UserSessionManager
    private var _binding: FragmentDetectionConnectionProtectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launchWhenStarted {
            if (sessionManager.hasAdminRole()) {
                findNavController()
            }
        }
        _binding = FragmentDetectionConnectionProtectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel

        viewModel.pwCorrect.observe(viewLifecycleOwner) {
            when (it) {
                0 -> binding.passwordInputLayout.error = getString(R.string.pw_error)
                1 -> {
                    hideKeyboard(requireActivity())
                    viewModel.pwCorrect
                }
                2 -> binding.passwordInputLayout.error = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}