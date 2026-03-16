package de.muenchen.appcenter.nimux.view.manage.users

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentUsersBinding
import de.muenchen.appcenter.nimux.util.UserSessionManager
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.viewmodel.PasswordViewModel
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class UsersFragment : Fragment() {
    private val viewModel: PasswordViewModel by viewModels()
    private lateinit var binding: FragmentUsersBinding
    @Inject lateinit var sessionManager: UserSessionManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        lifecycleScope.launchWhenStarted {
            if (sessionManager.hasAdminRole()) {
                findNavController().navigate(UsersFragmentDirections.actionNavUserToManageUsersFragment())
            }
        }

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_users,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        viewModel.pwCorrect.observe(viewLifecycleOwner) { pwCorrect ->
            when (pwCorrect) {
                0 -> {
                    binding.usersPasswordInputLayout.error = getString(R.string.pw_error)
                }
                1 -> {
                    hideKeyboard(requireActivity())
                    findNavController().navigate(UsersFragmentDirections.actionNavUserToManageUsersFragment())
                    viewModel.pwWasCorrect()
                }
                2 -> {
                    binding.usersPasswordInputLayout.error = null
                }
            }

        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.usersPasswordInputLayout.clearFocus()
    }

}

