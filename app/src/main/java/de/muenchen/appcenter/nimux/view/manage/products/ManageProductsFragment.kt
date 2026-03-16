package de.muenchen.appcenter.nimux.view.manage.products

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentManageProductsBinding
import de.muenchen.appcenter.nimux.model.Product
import de.muenchen.appcenter.nimux.repositories.ProductsRepository
import de.muenchen.appcenter.nimux.util.getProductIcon
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ManageProductsFragment : Fragment(), ManageProductsAdapter.OnItemClickListener {

    @Inject
    lateinit var productsRepository: ProductsRepository

    private lateinit var adapter: ManageProductsAdapter
    private var _binding: FragmentManageProductsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_manage_products, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.addProductFab.setOnClickListener {
            findNavController().navigate(ManageProductsFragmentDirections.actionManageProductsFragmentToAddProductFragment())
        }

        binding.productListRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                var fabVis = binding.addProductFab.isVisible
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && fabVis) {
                        fabVis = false
                        binding.addProductFab.hide()
                    } else if (dy < 0 && !fabVis) {
                        fabVis = false
                        binding.addProductFab.show()
                    }
                }
            })
        }
        val query = productsRepository.getProductQuery()
        val options = FirestoreRecyclerOptions.Builder<Product>().setQuery(
            query,
            Product::class.java
        ).build()

        adapter = ManageProductsAdapter(options)
        binding.productListRv.adapter = adapter
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        adapter.setOnItemClickListener(this)

        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_manage_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_check_network_action -> {
                lifecycleScope.launch {
                    if (productsRepository.connectedOnline())
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.network_check_successful),
                            Toast.LENGTH_SHORT
                        ).show()
                    else
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.network_check_failed))
                            .setPositiveButton(R.string.ok, null)
                            .show()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
        val currentProduct: Product = documentSnapshot.toObject()!!
        binding.productListRv.isVerticalScrollBarEnabled = false
        findNavController().navigate(
            ManageProductsFragmentDirections.actionManageProductsFragmentToProductItemFragment(
                currentProduct
            )
        )
    }
}

class ManageProductsAdapter internal constructor(options: FirestoreRecyclerOptions<Product>) :
    FirestoreRecyclerAdapter<Product, ManageProductsAdapter.ManageProductsViewHolder>(options) {

    private lateinit var listener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageProductsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_manage_product_view, parent, false)
        return ManageProductsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ManageProductsViewHolder, position: Int, model: Product) {
        holder.setAttrs(
            model.name,
            model.price,
            model.currentStock,
            model.refillSize,
            model.productIcon
        )
    }

    inner class ManageProductsViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {
        internal fun setAttrs(
            name: String,
            price: Double,
            currentAmount: Int,
            refillSize: Int,
            productIcon: Int,
        ) {
            view.findViewById<TextView>(R.id.list_manage_product_name).text = name
            view.findViewById<TextView>(R.id.list_manage_product_price).text =
                String.format(view.context.getString(R.string.money), price)
            view.findViewById<TextView>(R.id.list_manage_product_amount).apply {
                text = currentAmount.toString()
                if (currentAmount <= 0) {
                    val a = TypedValue()
                    context.theme.resolveAttribute(R.attr.colorError, a, true)
                    if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                        setTextColor(a.data)
                        view.findViewById<TextView>(R.id.list_manage_product_amount_text)
                            .setTextColor(a.data)
                    }
                } else {
                    val a = TypedValue()
                    context.theme.resolveAttribute(R.attr.colorOnBackground, a, true)
                    if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                        setTextColor(a.data)
                        view.findViewById<TextView>(R.id.list_manage_product_amount_text)
                            .setTextColor(a.data)
                    }
                }
            }


            //Doesn't work with a boolean value from the database, so i'm using the refillsize attribute for this
            view.findViewById<LinearLayout>(R.id.list_manage_product_amount_layout).visibility =
                if (refillSize == 0) {
                    View.GONE
                } else View.VISIBLE

            view.findViewById<ImageView>(R.id.list_manage_product_icon)
                .setImageResource(getProductIcon(productIcon))

            view.findViewById<MaterialCardView>(R.id.manage_product_list_card).setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(snapshots.getSnapshot(position), position)
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.listener = onItemClickListener
    }
}