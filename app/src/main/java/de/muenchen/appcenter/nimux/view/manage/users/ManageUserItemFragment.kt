package de.muenchen.appcenter.nimux.view.manage.users

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentManageUserItemBinding
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.util.showKeyboard
import de.muenchen.appcenter.nimux.util.showNetworkHint
import de.muenchen.appcenter.nimux.viewmodel.manage.users.ManageUserItemViewModel

@AndroidEntryPoint
class ManageUserItem : Fragment() {

    private lateinit var binding: FragmentManageUserItemBinding
    private val viewModel: ManageUserItemViewModel by viewModels()

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_manage_user_item, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_action -> {
                hideKeyboard(requireActivity())

                if (viewModel.user.toPay > 0) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.delete_user_remaining_credit_title))
                        .setMessage(getString(R.string.delete_user_remaining_credit_message, String.format("%.2f", viewModel.user.toPay) + "€"))
                        .setPositiveButton(
                            getString(R.string.yes)
                        ) { _, _ ->
                            showDeleteAlert()
                        }
                        .setNegativeButton(getString(R.string.no), null)
                        .show()
                } else {
                    showDeleteAlert()
                }
            }
            R.id.edit_action -> {
                hideKeyboard(requireActivity())
                viewModel.resetChip()
                findNavController().navigate(ManageUserItemDirections.actionManageUserItemToEditUserFragment(
                    viewModel.user, false))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDeleteAlert() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_user_alert_title, viewModel.user.name))
            .setMessage(getString(R.string.delete_user_alert_message))
            .setPositiveButton(
                getString(R.string.delete)
            ) { _, _ ->
                viewModel.deleteCurrentUser()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding =
            FragmentManageUserItemBinding.inflate(inflater, container, false)


        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this.viewLifecycleOwner
        binding.viewModel = viewModel

        setupObservers()
    }

    private fun setupObservers() {

        viewModel.notADouble.observe(viewLifecycleOwner) { notADouble ->
            if (notADouble) {
                binding.customAmountEditTextLayout.error = getString(R.string.wrong_input_type)
            }
        }

        viewModel.startsWithMin.observe(viewLifecycleOwner) { startsWithMin ->
            if (startsWithMin) {
                binding.customAmountEditTextLayout.error = getString(R.string.wrong_input_value)
            }
        }

        viewModel.canceled.observe(viewLifecycleOwner) { canceled ->
            if (canceled) {
                findNavController().popBackStack()
                hideKeyboard(requireActivity())
            }
        }

        viewModel.payed.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                binding.customAmountEditTextLayout.error = null
                hideKeyboard(requireActivity())
                findNavController().popBackStack()
            }
        }

        viewModel.exactZeroToast.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(requireContext(),
                    getString(R.string.topay_below_zero_toast),
                    Toast.LENGTH_SHORT).show()
                viewModel.exactZeroShown()
            }
        }

        val customInputLayout =
            requireView().findViewById<TextInputLayout>(R.id.custom_amount_edit_text_layout)

        viewModel.customAmountChecked.observe(viewLifecycleOwner) {
            if (it) {
                customInputLayout.apply {
                    alpha = 0f
                    visibility = View.VISIBLE

                    animate()
                        .alpha(1f)
                        .setDuration(resources.getInteger(R.integer.motion_short)
                            .toLong())
                        .setListener(null)
                }
                customInputLayout.requestFocus()
                showKeyboard(requireActivity())
            } else {
                if (customInputLayout.visibility != View.GONE) {
                    customInputLayout.animate()
                        .alpha(0f)
                        .setDuration(resources.getInteger(R.integer.motion_short)
                            .toLong())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                customInputLayout.visibility = View.GONE
                            }
                        })
                }
                hideKeyboard(requireActivity())
            }
        }

        viewModel.networkHint.observe(viewLifecycleOwner) {
            if (it) {
                showNetworkHint(requireContext(),
                    { _, _ -> viewModel.pay() },
                    { viewModel.networkHintShown() })
            }
        }

        viewModel.noChipSelected.observe(viewLifecycleOwner) {
            if (it) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.no_chip_selected_alert_title))
                    .setMessage(getString(R.string.no_chip_selected_alert_message))
                    .setPositiveButton(R.string.ok, null)
                    .show().setOnDismissListener {
                        viewModel.noChipHintShown()
                    }
            }
        }

        viewModel.showProgressBar.observe(viewLifecycleOwner) {
            if (it) {
                binding.progressBar.show()
                binding.manageUserPayButton.isEnabled = false
            } else {
                binding.progressBar.hide()
                binding.manageUserPayButton.isEnabled = true
            }
        }

        viewModel.deleted.observe(viewLifecycleOwner) {
            if (it) {
                hideKeyboard(requireActivity())
                findNavController().navigate(ManageUserItemDirections.actionDeleteUser())
            }
        }
    }
}