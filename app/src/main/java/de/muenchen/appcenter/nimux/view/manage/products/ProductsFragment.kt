package de.muenchen.appcenter.nimux.view.manage.products

import android.app.Activity
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
import de.muenchen.appcenter.nimux.databinding.FragmentProductsBinding
import de.muenchen.appcenter.nimux.util.UserSessionManager
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.viewmodel.PasswordViewModel
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class ProductsFragment : Fragment() {

    private val viewModel: PasswordViewModel by viewModels()
    @Inject lateinit var sessionManager: UserSessionManager
    private lateinit var binding: FragmentProductsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        lifecycleScope.launchWhenStarted {
            if (sessionManager.hasAdminRole()) {
                findNavController().navigate(ProductsFragmentDirections.actionNavProductsToManageProductsFragment())
            }
        }

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_products,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.pwCorrect.observe(viewLifecycleOwner) { pwCorrect ->
            when (pwCorrect) {
                0 -> {
                    binding.productsPasswordInputLayout.error = getString(R.string.pw_error)

                }
                1 -> {
                    hideKeyboard(activity as Activity)
                    findNavController().navigate(ProductsFragmentDirections.actionNavProductsToManageProductsFragment())
                    viewModel.pwWasCorrect()
                }
                2 -> {
                    binding.productsPasswordInputLayout.error = null
                }
            }

        }

        return binding.root
    }

}