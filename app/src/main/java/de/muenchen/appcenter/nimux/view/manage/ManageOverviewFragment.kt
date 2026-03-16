package de.muenchen.appcenter.nimux.view.manage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.muenchen.appcenter.nimux.databinding.FragmentManageOverviewBinding

class ManageOverviewFragment : Fragment() {

    private var _binding : FragmentManageOverviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentManageOverviewBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity)
            .supportActionBar
            ?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.manageOverviewSuggestedUserCard.setOnClickListener {
            findNavController().navigate(ManageOverviewFragmentDirections.actionManageOverviewFragmentToSuggestedUsersFragment())
        }

        binding.manageOverviewUserCard.setOnClickListener {
            findNavController().navigate(ManageOverviewFragmentDirections.actionManageOverviewFragmentToManageUsersFragment())
        }

        binding.manageOverviewProductCard.setOnClickListener {
            findNavController().navigate(ManageOverviewFragmentDirections.actionManageOverviewFragmentToManageProductsFragment())
        }

        binding.manageOverviewHistoryCard.setOnClickListener {
            findNavController().navigate(ManageOverviewFragmentDirections.actionManageOverviewFragmentToHistoryListFragment())
        }
    }
}