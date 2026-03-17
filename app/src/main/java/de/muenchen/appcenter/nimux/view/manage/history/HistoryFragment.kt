package de.muenchen.appcenter.nimux.view.manage.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.HistoryFragmentBinding
import de.muenchen.appcenter.nimux.util.UserSessionManager
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.viewmodel.PasswordViewModel
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private val viewModel: PasswordViewModel by viewModels()
    @Inject lateinit var sessionManager: UserSessionManager
    private lateinit var binding: HistoryFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        lifecycleScope.launchWhenStarted {
            if (sessionManager.hasAdminRole()) {
                findNavController().navigate(HistoryFragmentDirections.actionNavHistoryToHistoryListFragment())
            }
        }
        binding = DataBindingUtil.inflate(inflater, R.layout.history_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.pwCorrect.observe(viewLifecycleOwner) {
            when (it) {
                0 -> {
                    binding.passwordInputLayout.error = getString(R.string.pw_error)
                }
                1 -> {
                    hideKeyboard(requireActivity())
                    findNavController().navigate(HistoryFragmentDirections.actionNavHistoryToHistoryListFragment())
                    viewModel.pwWasCorrect()
                }
                2 -> {
                    binding.passwordInputLayout.error = null
                }
            }
        }
    }

}