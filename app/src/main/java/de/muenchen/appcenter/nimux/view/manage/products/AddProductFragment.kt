package de.muenchen.appcenter.nimux.view.manage.products

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentAddProductBinding
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.util.product_icon_bottle
import de.muenchen.appcenter.nimux.util.product_icon_can
import de.muenchen.appcenter.nimux.util.product_icon_cookie
import de.muenchen.appcenter.nimux.util.product_icon_cup
import de.muenchen.appcenter.nimux.util.product_icon_egg
import de.muenchen.appcenter.nimux.util.product_icon_fastfood
import de.muenchen.appcenter.nimux.util.product_icon_fridge
import de.muenchen.appcenter.nimux.util.product_icon_icecream
import de.muenchen.appcenter.nimux.util.product_icon_none
import de.muenchen.appcenter.nimux.util.product_icon_pizza
import de.muenchen.appcenter.nimux.util.product_icon_tea
import de.muenchen.appcenter.nimux.util.product_icon_water
import de.muenchen.appcenter.nimux.util.showNetworkHint
import de.muenchen.appcenter.nimux.viewmodel.manage.products.AddProductViewModel

@AndroidEntryPoint
class AddProductFragment : Fragment() {

    private lateinit var binding: FragmentAddProductBinding
    private val viewModel: AddProductViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_product, container, false)

        binding.addProductChooseIcon.setOnClickListener { view ->
            showPopUp(view)
        }

        return binding.root
    }

    private fun setButtonHelper(btnView: MaterialButton, title: Int, iconLeft: Int?) {
        btnView.text = getString(title)
        btnView.setCompoundDrawablesWithIntrinsicBounds(
            if (iconLeft != null) {
                AppCompatResources.getDrawable(
                    requireContext(),
                    iconLeft
                )
            } else {
                null
            },
            null,
            AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.ic_round_arrow_drop_down_24
            ),
            null
        )
    }

    private fun showPopUp(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.product_icon_selection_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val btnView = view.findViewById<MaterialButton>(R.id.add_product_choose_icon)
            when (menuItem.itemId) {
                R.id.product_icon_none -> {
                    setButtonHelper(btnView, R.string.none, null)
                    viewModel.productIcon.value = product_icon_none
                }
                R.id.product_icon_cup -> {
                    setButtonHelper(
                        btnView,
                        R.string.cup,
                        R.drawable.ic_round_free_breakfast_24
                    )
                    viewModel.productIcon.value = product_icon_cup
                }
                R.id.product_icon_bottle -> {
                    setButtonHelper(
                        btnView,
                        R.string.bottle,
                        R.drawable.ic_bottle_wine
                    )
                    viewModel.productIcon.value = product_icon_bottle
                }
                R.id.product_icon_water -> {
                    setButtonHelper(
                        btnView,
                        R.string.water,
                        R.drawable.ic_round_local_drink_24
                    )
                    viewModel.productIcon.value = product_icon_water
                }
                R.id.product_icon_fastfood -> {
                    setButtonHelper(
                        btnView,
                        R.string.fastfood,
                        R.drawable.ic_round_fastfood_24
                    )
                    viewModel.productIcon.value = product_icon_fastfood
                }
                R.id.product_icon_fridge -> {
                    setButtonHelper(
                        btnView,
                        R.string.fridge,
                        R.drawable.ic_round_kitchen_24
                    )
                    viewModel.productIcon.value = product_icon_fridge
                }
                R.id.product_icon_cookie -> {
                    setButtonHelper(
                        btnView,
                        R.string.cookie,
                        R.drawable.ic_round_cookie_24
                    )
                    viewModel.productIcon.value = product_icon_cookie
                }
                R.id.product_icon_egg -> {
                    setButtonHelper(
                        btnView,
                        R.string.egg_icon_name,
                        R.drawable.ic_baseline_egg_24
                    )
                    viewModel.productIcon.value = product_icon_egg
                }
                R.id.product_icon_tea -> {
                    setButtonHelper(
                        btnView,
                        R.string.tea_icon_name,
                        R.drawable.ic_baseline_emoji_food_beverage_24
                    )
                    viewModel.productIcon.value = product_icon_tea
                }
                R.id.product_icon_pizza -> {
                    setButtonHelper(
                        btnView,
                        R.string.pizza_icon_name,
                        R.drawable.ic_baseline_local_pizza_24
                    )
                    viewModel.productIcon.value = product_icon_pizza
                }
                R.id.product_icon_icecream -> {
                    setButtonHelper(
                        btnView,
                        R.string.ice_cream_icon_name,
                        R.drawable.ic_baseline_icecream_24
                    )
                    viewModel.productIcon.value = product_icon_icecream
                }
                R.id.product_icon_can -> {
                    setButtonHelper(
                        btnView,
                        R.string.product_icon_can_name,
                        R.drawable.ic_coke_can
                    )
                    viewModel.productIcon.value = product_icon_can
                }
            }
            true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupMenu.setForceShowIcon(true)
        }
        popupMenu.show()
        hideKeyboard(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setups()
    }

    private fun setups() {

        viewModel.refillable.observe(viewLifecycleOwner) {
            val refillLayout =
                requireView().findViewById<ConstraintLayout>(R.id.refill_views_layout)
            if (it) {
                refillLayout.apply {
                    alpha = 0f
                    visibility = View.VISIBLE

                    animate()
                        .alpha(1f)
                        .setDuration(resources.getInteger(R.integer.motion_short)
                            .toLong())
                        .setListener(null)

                }
            } else {
                if (refillLayout.visibility != View.GONE) {
                    refillLayout.animate()
                        .alpha(0f)
                        .setDuration(resources.getInteger(R.integer.motion_short)
                            .toLong())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                refillLayout.visibility = View.GONE
                            }
                        })
                }
            }
        }
        viewModel.addProductDone.observe(viewLifecycleOwner) {
            if (it) {
                findNavController().popBackStack()
            }
        }
        viewModel.performHapticFeedback.observe(viewLifecycleOwner) {
            if (it) requireView().performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
        viewModel.hideKeyboard.observe(viewLifecycleOwner) { hideKeyboard ->
            if (hideKeyboard) {
                hideKeyboard(requireActivity())
                viewModel.doneHideKeyboard()
            }
        }

        viewModel.productNameEmpty.observe(viewLifecycleOwner) {
            if (it) {
                binding.addProductInputName.error = getString(R.string.field_cant_be_empty)
            } else {
                binding.addProductInputName.error = null
            }
        }
        viewModel.productPriceEmpty.observe(viewLifecycleOwner) {
            if (it) {
                binding.addProductInputPrice.error = getString(R.string.field_cant_be_empty)
            } else {
                binding.addProductInputPrice.error = null
            }
        }
        viewModel.productStockEmpty.observe(viewLifecycleOwner) {
            if (it) {
                binding.currentStockInputLayout.error = getString(R.string.field_cant_be_empty)
            } else {
                binding.currentStockInputLayout.error = null
            }
        }
        viewModel.productRefillSizeEmpty.observe(viewLifecycleOwner) {
            if (it) {
                binding.refillSizeLayout.error = getString(R.string.field_cant_be_empty)
            } else {
                binding.refillSizeLayout.error = null
            }
        }
        viewModel.productPriceWrong.observe(viewLifecycleOwner) {
            if (it) {
                binding.addProductInputPrice.error = getString(R.string.value_wrong_error)
            } else {
                binding.addProductInputPrice.error = null
            }
        }
        viewModel.productStockWrong.observe(viewLifecycleOwner) {
            if (it) {
                binding.currentStockInputLayout.error = getString(R.string.value_wrong_error)
            } else {
                binding.currentStockInputLayout.error = null
            }
        }
        viewModel.productRefillSizeWrong.observe(viewLifecycleOwner) {
            if (it) {
                binding.refillSizeLayout.error = getString(R.string.value_wrong_error)
            } else {
                binding.refillSizeLayout.error = null
            }
        }
        viewModel.productNameExists.observe(viewLifecycleOwner) {
            if (it) {
                binding.addProductInputName.error = getString(R.string.product_exists_error)
            } else {
                binding.addProductInputName.error = null
            }
        }
        viewModel.networkHint.observe(viewLifecycleOwner) {
            if (it) {
                showNetworkHint(requireContext(),
                    { _, _ -> viewModel.addProduct() },
                    { viewModel.networkHintShown() })
            }
        }
        viewModel.showProgressBar.observe(viewLifecycleOwner) {
            if (it) {
                binding.progressBar.show()
                binding.addProductButton.isEnabled = false
            } else {
                binding.progressBar.hide()
                binding.addProductButton.isEnabled = true
            }
        }
        viewModel.productAdded.observe(viewLifecycleOwner) {
            if (it) {
                hideKeyboard(requireActivity())
                findNavController().navigate(AddProductFragmentDirections.actionAddProductFragmentToManageProductsFragment())
            }
        }
    }
}