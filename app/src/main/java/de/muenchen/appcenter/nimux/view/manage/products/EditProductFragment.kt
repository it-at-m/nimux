package de.muenchen.appcenter.nimux.view.manage.products

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
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
import de.muenchen.appcenter.nimux.databinding.FragmentEditProductBinding
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.util.showNetworkHint
import de.muenchen.appcenter.nimux.viewmodel.manage.products.EditProductViewModel

@AndroidEntryPoint
class EditProductFragment : Fragment() {

    private lateinit var binding : FragmentEditProductBinding

    private val viewModel: EditProductViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_edit_product,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.editProductTitle.text = getString(R.string.edit_title, viewModel.product.name)
        binding.editProductChooseIconButton.setOnClickListener {
            showIconSelect(it)
        }
        when(viewModel.product.productIcon){
            product_icon_none->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.none, null)
            }
            product_icon_cup->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.cup, R.drawable.ic_round_free_breakfast_24)
            }
            product_icon_bottle->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.bottle, R.drawable.ic_bottle_wine)
            }
            product_icon_water->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.water, R.drawable.ic_round_local_drink_24)
            }
            product_icon_fastfood->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.fastfood, R.drawable.ic_round_fastfood_24)
            }
            product_icon_fridge->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.fridge, R.drawable.ic_round_kitchen_24)
            }
            product_icon_cookie->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.cookie, R.drawable.ic_round_cookie_24)
            }
            product_icon_egg->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.egg_icon_name, R.drawable.ic_baseline_egg_24)
            }
            product_icon_tea->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.tea_icon_name, R.drawable.ic_baseline_emoji_food_beverage_24)
            }
            product_icon_pizza->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.pizza_icon_name, R.drawable.ic_baseline_local_pizza_24)
            }
            product_icon_icecream->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.ice_cream_icon_name, R.drawable.ic_baseline_icecream_24)
            }
            product_icon_can->{
                setButtonHelper(binding.editProductChooseIconButton, R.string.product_icon_can_name, R.drawable.ic_coke_can)
            }
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.priceInputWrong.observe(viewLifecycleOwner) {
            if (it) binding.editProductPriceLayout.error = getString(R.string.value_wrong_error)
            else binding.editProductPriceLayout.error = null
        }
        viewModel.stockInputWrong.observe(viewLifecycleOwner) {
            if (it) binding.currentStockInputLayout.error = getString(R.string.value_wrong_error)
            else binding.currentStockInputLayout.error = null
        }
        viewModel.refillInputWrong.observe(viewLifecycleOwner) {
            if (it) binding.refillSizeLayout.error = getString(R.string.value_wrong_error)
            else binding.refillSizeLayout.error = null
        }

        viewModel.canceled.observe(viewLifecycleOwner) {
            if (it) {
                findNavController().popBackStack()
            }
        }
        viewModel.updated.observe(viewLifecycleOwner) {
            if (it) {
                findNavController().navigate(EditProductFragmentDirections.actionEditProductFragmentToProductItemFragment(
                    viewModel.product))
            }
        }
        viewModel.networkHint.observe(viewLifecycleOwner) {
            if (it) {
                showNetworkHint(requireContext(),
                    { _, _ -> viewModel.updateProduct() },
                    { viewModel.networkHintShown() })
            }
        }
        viewModel.showProgressBar.observe(viewLifecycleOwner) {
            if (it) {
                binding.progressBar.show()
                binding.saveChangesButton.isEnabled = false
            } else {
                binding.progressBar.hide()
                binding.saveChangesButton.isEnabled = true
            }
        }

        viewModel.hideKeyboard.observe(viewLifecycleOwner) { hideKeyboard ->
            if (hideKeyboard) {
                hideKeyboard(requireActivity())
                viewModel.doneHideKeyboard()
            }
        }
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

    private fun showIconSelect(view:View){
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.product_icon_selection_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val btnView = view.findViewById<MaterialButton>(R.id.edit_product_choose_icon_button)
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
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
        hideKeyboard(requireActivity())
    }
}