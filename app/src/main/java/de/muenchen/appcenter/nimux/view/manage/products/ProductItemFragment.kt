package de.muenchen.appcenter.nimux.view.manage.products

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentProductItemBinding
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.util.showKeyboard
import de.muenchen.appcenter.nimux.util.showNetworkHint
import de.muenchen.appcenter.nimux.viewmodel.manage.products.ProductItemViewModel

@AndroidEntryPoint
class ProductItemFragment : Fragment() {

    private lateinit var binding: FragmentProductItemBinding
    private val viewModel: ProductItemViewModel by viewModels()

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_manage_product_item, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit_action -> {
                hideKeyboard(requireActivity())
                viewModel.resetChip()
                findNavController().navigate(
                    ProductItemFragmentDirections.actionProductItemFragmentToEditProductFragment(
                        viewModel.product)
                )
            }
            R.id.delete_action -> {
                hideKeyboard(requireActivity())
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.delete_product_alert_title,
                        viewModel.product.name))
                    .setMessage(getString(R.string.delete_product_alert_message))
                    .setPositiveButton(
                        getString(R.string.delete)
                    ) { _, _ ->
                        viewModel.deleteCurrentProduct()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_product_item,
            container,
            false
        )

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.manageProductCancelButton.setOnClickListener {
            findNavController().popBackStack()
            hideKeyboard(requireActivity())
        }
        binding.productItemIcon.setImageResource(viewModel.productItemResource)

        viewModel.wrongInput.observe(viewLifecycleOwner) {
            if (it)
                binding.manageProductCustomAmountLayout.error = getString(R.string.wrong_input_type)
            else
                binding.manageProductCustomAmountLayout.error = null
        }

        val customInputLayout =
            requireView().findViewById<TextInputLayout>(R.id.manage_product_custom_amount_layout)
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

        viewModel.deleted.observe(viewLifecycleOwner) {
            if (it) {
                hideKeyboard(requireActivity())
                findNavController().navigate(ProductItemFragmentDirections.actionDeleteProduct())
            }
        }
        viewModel.stockAdded.observe(viewLifecycleOwner) {
            if (it) {
                hideKeyboard(requireActivity())
                findNavController().popBackStack()
            }
        }
        viewModel.networkHint.observe(viewLifecycleOwner) {
            if (it) {
                showNetworkHint(requireContext(),
                    { _, _ -> viewModel.addStock() },
                    { viewModel.networkHintShown() })
            }
        }
        viewModel.noChipSelected.observe(viewLifecycleOwner) {
            if (it) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.no_value_alert_title))
                    .setMessage(getString(R.string.no_value_alert_message))
                    .setPositiveButton(R.string.ok, null)
                    .show().setOnDismissListener {
                        viewModel.noChipHintShown()
                    }
            }
        }

        viewModel.showProgressBar.observe(viewLifecycleOwner) {
            if (it) {
                binding.progressBar.show()
                binding.manageProductAddButton.isEnabled = false
            } else {
                binding.progressBar.hide()
                binding.manageProductAddButton.isEnabled = true
            }
        }
    }
}