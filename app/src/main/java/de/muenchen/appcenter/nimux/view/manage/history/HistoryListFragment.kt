package de.muenchen.appcenter.nimux.view.manage.history

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentHistoryListBinding
import de.muenchen.appcenter.nimux.repositories.OtherRepository
import de.muenchen.appcenter.nimux.repositories.ProductsRepository
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HistoryListFragment : Fragment() {

    @Inject lateinit var usersRepository: UsersRepository
    @Inject lateinit var productRepository: ProductsRepository
    @Inject lateinit var otherRepository: OtherRepository

    private var _binding : FragmentHistoryListBinding ? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        // Inflate the layout for this fragment
        setHasOptionsMenu(true)

        _binding = FragmentHistoryListBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_manage_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_check_network_action -> {
                lifecycleScope.launch {
                    if (usersRepository.connectedOnline())
                        Toast.makeText(requireContext(),
                            getString(R.string.network_check_successful),
                            Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val words = arrayListOf(
            getString(R.string.users_tab_title),
            getString(R.string.products_tab_title),
            getString(R.string.acc_tab_title)
        )
        binding.logPager.adapter = LogPagerAdapter(usersRepository, productRepository, otherRepository, words,findNavController())
        TabLayoutMediator(binding.logTabLayout, binding.logPager){ tab, position ->
            tab.text = words[position]
        }.attach()
    }

}