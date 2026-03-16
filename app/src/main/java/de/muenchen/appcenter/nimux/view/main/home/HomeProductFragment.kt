package de.muenchen.appcenter.nimux.view.main.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.doOnPreDraw
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.HomeProductFragmentBinding
import de.muenchen.appcenter.nimux.databinding.HomeProductRvLayoutBinding
import de.muenchen.appcenter.nimux.datasources.DonateItemDataSource
import de.muenchen.appcenter.nimux.model.DonateItem
import de.muenchen.appcenter.nimux.model.MultiOrderProductList
import de.muenchen.appcenter.nimux.model.MultiOrderProductListWithProduct
import de.muenchen.appcenter.nimux.model.Product
import de.muenchen.appcenter.nimux.repositories.ProductsRepository
import de.muenchen.appcenter.nimux.util.MultiOrderOverviewAdapter
import de.muenchen.appcenter.nimux.util.MultiOrderProductAdapter
import de.muenchen.appcenter.nimux.util.RangeValidator
import de.muenchen.appcenter.nimux.util.getProductIcon
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.util.round
import de.muenchen.appcenter.nimux.util.showEnterUserPin
import de.muenchen.appcenter.nimux.util.showKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class HomeProductFragment : Fragment() {

    @Inject
    lateinit var productsRepository: ProductsRepository

    @Inject
    lateinit var donateItemDataSource: DonateItemDataSource

    private lateinit var binding: HomeProductFragmentBinding

    private var currentSelectedOption = 0

    private lateinit var productQuery: Query
    private lateinit var options: FirestoreRecyclerOptions<Product>
    private lateinit var productAdapter: ProductHomeAdapter

    private lateinit var multiOrderAdapter: MultiOrderProductAdapter
    private lateinit var multiOrderOverViewAdapter: MultiOrderOverviewAdapter
    private var productAmountList = mutableListOf<MultiOrderProductListWithProduct>()
    private var productAmountOverviewList = mutableListOf<MultiOrderProductListWithProduct>()

    private val selectedDateTime = Calendar.getInstance()
    private var selectedAmount = 0.0
    private var customAmountSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.motion_medium).toLong()
            scrimColor = requireContext().getColor(android.R.color.transparent)
            setAllContainerColors(requireContext().getColor(android.R.color.transparent))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        currentSelectedOption = R.id.menu_action_single_buy
        binding = HomeProductFragmentBinding.inflate(inflater, container, false)
        binding.user = HomeProductFragmentArgs.fromBundle(requireArguments()).currentUser
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        productQuery = productsRepository.getProductQuery()
        options =
            FirestoreRecyclerOptions.Builder<Product>().setQuery(productQuery, Product::class.java)
                .build()
        productAdapter = ProductHomeAdapter(options)
        postponeEnterTransition()
        view.doOnPreDraw {
            startPostponedEnterTransition()
        }

        binding.otherOptionsButtonHomeProduct.setOnClickListener { fab ->
            showFabPopUpMenu(fab, R.menu.home_product_other_options_menu)
        }
        binding.otherOptionsButtonHomeProduct.text = getString(R.string.single_buy_menu_title)
        setUpSingleBuy()
        binding.lifecycleOwner = viewLifecycleOwner
        (activity as AppCompatActivity)
            .supportActionBar
            ?.setDisplayHomeAsUpEnabled(false)
    }

    @SuppressLint("RestrictedApi")
    private fun showFabPopUpMenu(v: View, @MenuRes homeProductOtherOptionsMenu: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(homeProductOtherOptionsMenu, popup.menu)
        if (popup.menu is MenuBuilder) {
            val menuBuilder = popup.menu as MenuBuilder
            try {
                menuBuilder.setOptionalIconsVisible(true)
                for (item in menuBuilder.visibleItems) {
                    val iconMarginPx =
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 8.toFloat(), resources.displayMetrics
                        )
                            .toInt()
                    if (item.icon != null) {
                        item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeProductFragment", "menubuilder error: ${e.printStackTrace()}")
            }

        }
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.menu_action_single_buy -> {
                    binding.otherOptionsButtonHomeProduct.text =
                        getString(R.string.single_buy_menu_title)
                    if (currentSelectedOption != R.id.menu_action_single_buy)
                        switchToSingleBuy()
                }

                R.id.menu_action_multi_buy -> {
                    binding.otherOptionsButtonHomeProduct.text = getString(R.string.multi_buy_title)
                    if (currentSelectedOption != R.id.menu_action_multi_buy)
                        switchToMultiBuy()
                }
                // could be integrated if needed
//                R.id.menu_action_dono -> {
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        val item = donateItemDataSource.getDonationItem(0.0)
//                        lifecycleScope.launch(Dispatchers.Main) {
//                            if (item == null) {
//                                binding.otherOptionsButtonHomeProduct.text =
//                                    getString(R.string.donate_amount_chip_text)
//                                switchToDonoBuy()
//                            } else {
//                                Toast.makeText(requireContext(),
//                                    getString(R.string.donation_already_active_toast),
//                                    Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    }
//                }
            }
            currentSelectedOption = menuItem.itemId
            return@setOnMenuItemClickListener false
        }
        popup.show()
    }

    private fun switchToDonoBuy() {
        setUpDonoBuy()
        val fadeThrough = MaterialFadeThrough()
        TransitionManager.beginDelayedTransition((view) as ViewGroup, fadeThrough)
        binding.singleBuyLayout.visibility = View.GONE
        binding.multiOrderLayout.visibility = View.GONE
        binding.donoLayout.visibility = View.VISIBLE
    }

    private fun switchToMultiBuy() {
        if (binding.productRv.isEmpty()) setUpMultiBuy()
        val fadeThrough = MaterialFadeThrough()
        TransitionManager.beginDelayedTransition((view) as ViewGroup, fadeThrough)
        binding.singleBuyLayout.visibility = View.GONE
        binding.donoLayout.visibility = View.GONE
        binding.multiOrderLayout.visibility = View.VISIBLE
    }

    private fun setUpDonoBuy() {
        setUpDonoChipListener()
        binding.switchTimeLimit.setOnCheckedChangeListener { _, b ->
            if (b) {
                binding.timeLimitLayout.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .setDuration(
                            resources.getInteger(R.integer.motion_short)
                                .toLong()
                        )
                        .setListener(null)
                }
            } else {
                if (binding.timeLimitLayout.isVisible)
                    binding.timeLimitLayout.animate()
                        .alpha(0f)
                        .setDuration(
                            resources.getInteger(R.integer.motion_short)
                                .toLong()
                        )
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                binding.timeLimitLayout.visibility = View.GONE
                            }
                        })
            }
        }
        binding.buttonDonoConfirm.setOnClickListener {
            addDonationClick()
        }
    }

    private fun addDonationClick() {
        val anon = binding.switchDisplayName.isChecked
        val timeLimitBool = binding.switchTimeLimit.isChecked
        if (customAmountSelected) {
            selectedAmount =
                binding.customAmountEditText.text.toString().replace(",", ".").toDoubleOrNull()
                    ?: 0.0
        }
        if (selectedAmount != 0.0) {
            if (!timeLimitBool) {
                val item = DonateItem(
                    binding.user!!.stringSortID,
                    binding.user!!.name,
                    selectedAmount,
                    anon
                )
                proceedToConfirmDonation(item)
            } else {
                val timeLimit = getSelectedTime()
                val reAddUnused = binding.switchReaddAmount.isChecked
                val item =
                    DonateItem(
                        binding.user!!.stringSortID,
                        binding.user!!.name,
                        selectedAmount,
                        anon,
                        timeLimit,
                        reAddUnused
                    )
                proceedToConfirmDonation(item)
            }
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.donate_no_amount_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getSelectedTime(): Long {
        val calendar = Calendar.getInstance()
        when (binding.selectTimeGroup.checkedChipId) {
            binding.selectTimeChipOneHour.id -> {
                calendar.add(Calendar.HOUR_OF_DAY, 1)
            }

            binding.selectTimeChipTwoHours.id -> {
                calendar.add(Calendar.HOUR_OF_DAY, 2)
            }

            binding.selectTimeChipEndOfDay.id -> {
                calendar.set(Calendar.HOUR_OF_DAY, 22)
            }

            binding.selectTimeChipEndOfWeek.id -> {
                calendar.set(Calendar.DAY_OF_WEEK, 7)
            }

            binding.selectTimeChipManually.id -> return selectedDateTime.timeInMillis
            else -> Toast.makeText(
                requireContext(),
                getString(R.string.donate_no_time_selected_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
        return calendar.timeInMillis
    }

    private fun proceedToConfirmDonation(donateItem: DonateItem) {
        val comingFromFace =
            HomeProductFragmentArgs.fromBundle(requireArguments()).fromFaceRecon
        val skipPinWithFace = comingFromFace && binding.user?.faceSkipsPin ?: false
        if (binding.user!!.pin == null || skipPinWithFace) {
            moveToConfirm(donateItem)
        } else {
            showEnterUserPin(binding.user!!, requireContext(), requireView()) {
                moveToConfirm(donateItem)
            }
        }
    }

    private fun moveToConfirm(donateItem: DonateItem) {
        val cardTransName = getString(R.string.trans_card_ausgeben_confirm)
        val nameTransName = getString(R.string.trans_name_ausgeben_confirm)

        val extras = FragmentNavigatorExtras(
            binding.homeProductCompleteLayout as View to cardTransName,
            binding.homeProductUserName as View to nameTransName
        )
        val action =
            HomeProductFragmentDirections.actionHomeProductFragmentToAusgebenConfirmFragment(
                donateItem,
                binding.user!!,
                HomeProductFragmentArgs.fromBundle(requireArguments()).fromFaceRecon
            )
        findNavController().navigate(action, extras)
    }

    private fun setUpDonoChipListener() {
        binding.selectDonateGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.first() == R.id.select_donate_chip_custom) {
                customAmountSelected = true
                binding.customAmountEditTextLayout.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .setDuration(
                            resources.getInteger(R.integer.motion_short)
                                .toLong()
                        )
                        .setListener(null)
                }
                binding.customAmountEditTextLayout.requestFocus()
                showKeyboard(requireActivity())
            } else {
                customAmountSelected = false
                if (binding.customAmountEditTextLayout.visibility != View.GONE) {
                    binding.customAmountEditTextLayout.animate()
                        .alpha(0f)
                        .setDuration(
                            resources.getInteger(R.integer.motion_short)
                                .toLong()
                        )
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                binding.customAmountEditTextLayout.visibility = View.GONE
                            }
                        })
                }
                hideKeyboard(requireActivity())
            }
            when (checkedIds.first()) {
                binding.selectDonateChip10.id -> selectedAmount = 10.0
                binding.selectDonateChip20.id -> selectedAmount = 20.0
                binding.selectDonateChip30.id -> selectedAmount = 30.0
                binding.selectDonateChip40.id -> selectedAmount = 40.0
                binding.selectDonateChip50.id -> selectedAmount = 50.0
                binding.selectDonateChip75.id -> selectedAmount = 75.0
                binding.selectDonateChip100.id -> selectedAmount = 100.0
            }
        }
        binding.selectTimeGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.first() == binding.selectTimeChipManually.id) {
                val today = MaterialDatePicker.todayInUtcMilliseconds()
                val calendar = Calendar.getInstance()

                calendar.timeInMillis = today

                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val minDate = calendar.timeInMillis

                calendar.add(Calendar.WEEK_OF_YEAR, 3)
                val maxDate = calendar.timeInMillis

                val calConstraints = CalendarConstraints.Builder()
                    .setStart(minDate)
                    .setEnd(maxDate)
                    .setValidator(RangeValidator(minDate, maxDate))
                    .build()

                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select end date")
                    .setCalendarConstraints(calConstraints)
                    .build()
                val timePicker = MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setTitleText("Select end time")
                    .build()

                datePicker.addOnPositiveButtonClickListener {
                    selectedDateTime.timeInMillis = it
                    timePicker.show(
                        (activity as AppCompatActivity).supportFragmentManager,
                        "ausgeben_time_tag"
                    )
                }
                datePicker.addOnNegativeButtonClickListener {
                    binding.selectTimeChipOneHour.isChecked = true
                }
                datePicker.addOnCancelListener {
                    binding.selectTimeChipOneHour.isChecked = true
                }

                timePicker.addOnPositiveButtonClickListener {
                    selectedDateTime[Calendar.HOUR] = timePicker.hour
                    selectedDateTime[Calendar.MINUTE] = timePicker.minute
                    val selectedDateTimeString =
                        SimpleDateFormat("dd.MM.yyyy HH:mm").format(selectedDateTime.timeInMillis)
                    binding.selectTimeChipManually.text = selectedDateTimeString
                }
                timePicker.addOnNegativeButtonClickListener {
                    binding.selectTimeChipOneHour.isChecked = true
                }
                timePicker.addOnCancelListener {
                    binding.selectTimeChipOneHour.isChecked = true
                }
                datePicker.show(
                    (activity as AppCompatActivity).supportFragmentManager,
                    "ausgeben_date_tag"
                )
            } else {
                binding.selectTimeChipManually.text = getString(R.string.chip_time_manual)
            }
        }
    }

    private fun setUpMultiBuy() {
        binding.confirmPurchaseButton.setOnClickListener {
            val comingFromFace =
                HomeProductFragmentArgs.fromBundle(requireArguments()).fromFaceRecon
            val skipPinWithFace = comingFromFace && binding.user?.faceSkipsPin ?: false
            if (binding.user?.pin == null || skipPinWithFace)
                navToMultiBuyCheckout()
            if (binding.user?.pin != null) {
                showEnterUserPin(binding.user!!, requireContext(), requireView()) {
                    navToMultiBuyCheckout()
                }
            }
        }
        multiOrderOverViewAdapter = MultiOrderOverviewAdapter()
        multiOrderAdapter = MultiOrderProductAdapter({
            if (it.multiOrderProductList.amount < 300) {
                it.multiOrderProductList.amount
                ++it.multiOrderProductList.amount
                plusMinusClicked(it)
            }
        }, {
            if (it.multiOrderProductList.amount > 0) {
                it.multiOrderProductList.amount
                --it.multiOrderProductList.amount
                plusMinusClicked(it)
            }
        })
        binding.productRv.apply {
            adapter = multiOrderAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
        }
        binding.overviewRv.apply {
            adapter = multiOrderOverViewAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
        }
        plusMinusClicked(null)
        lifecycleScope.launch(Dispatchers.IO) {
            if (productAmountList.isEmpty()) {
                val prods = productsRepository.getAllProducts()
                prods.forEach { prod ->
                    productAmountList.add(
                        MultiOrderProductListWithProduct(
                            MultiOrderProductList(
                                prod.stringSortID,
                                prod.name,
                                prod.price,
                                0
                            ),
                            prod
                        )
                    )
                }
            }
            multiOrderAdapter.submitList(productAmountList)
            multiOrderOverViewAdapter.submitList(productAmountOverviewList)
        }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun plusMinusClicked(
        item: MultiOrderProductListWithProduct?,
    ) {
        if (item != null) {
            multiOrderAdapter.notifyItemChanged(multiOrderAdapter.currentList.indexOf(item))
            productAmountOverviewList.clear()
            productAmountList.forEach {
                if (it.multiOrderProductList.amount != 0) {
                    productAmountOverviewList.add(it)
                }
            }
            multiOrderOverViewAdapter.notifyDataSetChanged()
        }
        var totalPay = 0.0
        productAmountOverviewList.forEach {
            if (it.multiOrderProductList.amount != 0) {
                totalPay += it.multiOrderProductList.amount.toDouble() * it.multiOrderProductList.price
                totalPay = totalPay.round(2)
            }
        }
        binding.totalAmount.text =
            String.format("%.2f", totalPay) + " €"
    }

    private fun navToMultiBuyCheckout() {
        if (productAmountOverviewList.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.no_products_selected_toast),
                Toast.LENGTH_LONG
            ).show()
        }
        if (productAmountOverviewList.isNotEmpty()) {
            val checkoutTransName = getString(R.string.multi_order_checkout_card_trans_name)
            val nameDetailName = getString(R.string.multi_order_checkout_user_name_trans_name)

            val extras = FragmentNavigatorExtras(
                (binding.homeProductCompleteLayout as View) to checkoutTransName,
                binding.homeProductUserName to nameDetailName,
            )
            val action =
                HomeProductFragmentDirections.actionHomeProductFragmentToMultiOrderCheckoutFragment(
                    productAmountOverviewList.toTypedArray(),
                    binding.user!!,
                    HomeProductFragmentArgs.fromBundle(requireArguments()).fromFaceRecon
                )
            findNavController().navigate(action, extras)
        }
    }


    private fun switchToSingleBuy() {
        val fadeThrough = MaterialFadeThrough()
        TransitionManager.beginDelayedTransition((view) as ViewGroup, fadeThrough)
        binding.multiOrderLayout.visibility = View.GONE
        binding.donoLayout.visibility = View.GONE
        binding.singleBuyLayout.visibility = View.VISIBLE

    }

    private fun setUpSingleBuy() {
        binding.homeProductRecyclerview.apply {
            setHasFixedSize(true)

            val metrics = resources.displayMetrics
            adapter = productAdapter

            val yInches = metrics.heightPixels / metrics.ydpi
            val xInches = metrics.widthPixels / metrics.xdpi
            val diagonalInches = sqrt((xInches * xInches + yInches * yInches).toDouble())
            layoutManager = if (diagonalInches >= 7) {
                // 6.5inch device or bigger

                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                    GridLayoutManager(requireContext(), 3)
                else
                    GridLayoutManager(requireContext(), 2)
            } else {
                // smaller device
                LinearLayoutManager(requireContext())
            }
        }

        productAdapter.setOnItemClickListener(object : ProductHomeAdapter.ProductItemClickListener {
            override fun onItemClick(product: Product, cardView: View) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val donoActive = donateItemDataSource.getDonationItem(product.price)
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (donoActive != null) {
                            goToCheckout(product)
                        } else {
                            val comingFromFace =
                                HomeProductFragmentArgs.fromBundle(requireArguments()).fromFaceRecon
                            val skipPinWithFace =
                                comingFromFace && binding.user?.faceSkipsPin ?: false
                            if (binding.user?.pin == null || skipPinWithFace) {
                                goToCheckout(product)
                            } else {
                                showEnterUserPin(binding.user!!, requireContext(), requireView()) {
                                    goToCheckout(product)
                                }
                            }
                        }
                    }
                }

            }
        })
    }

    private fun goToCheckout(product: Product) {
        val checkoutTransName = getString(R.string.checkout_card_trans_name)

        val extras = FragmentNavigatorExtras(binding.homeProductCompleteLayout to checkoutTransName)
        val fromFace = HomeProductFragmentArgs.fromBundle(requireArguments()).fromFaceRecon
        val action =
            HomeProductFragmentDirections.actionHomeProductFragmentToHomeCheckoutFragment(
                binding.user!!,
                product, fromFace, false
            )

        findNavController().navigate(action, extras)

    }

    override fun onStart() {
        super.onStart()
        productAdapter.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        productAdapter.stopListening()

    }


}

class ProductHomeAdapter internal constructor(options: FirestoreRecyclerOptions<Product>) :
    FirestoreRecyclerAdapter<Product, ProductHomeAdapter.ProductViewHolder>(options) {
    inner class ProductViewHolder(
        private val binding: HomeProductRvLayoutBinding,
        listener: ProductItemClickListener,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.run {
                this.listener = listener
            }
        }

        fun bind(product: Product) {
            binding.product = product
            binding.executePendingBindings()
            binding.homeProductItemIcon.setImageResource(getProductIcon(product.productIcon))
        }
    }

    private lateinit var listener: ProductItemClickListener

    interface ProductItemClickListener {
        fun onItemClick(product: Product, cardView: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(
            HomeProductRvLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), listener
        )
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int, model: Product) {
        holder.bind(getItem(position))
    }

    fun setOnItemClickListener(onItemClickListener: ProductItemClickListener) {
        this.listener = onItemClickListener
    }

}
