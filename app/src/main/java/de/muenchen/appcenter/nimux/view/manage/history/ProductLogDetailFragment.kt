package de.muenchen.appcenter.nimux.view.manage.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.muenchen.appcenter.nimux.databinding.FragmentProductLogDetailBinding

class ProductLogDetailFragment : Fragment() {

    private lateinit var binding: FragmentProductLogDetailBinding
    private val viewModel: ProductLogDetailViewModel by viewModels {
        ProductLogDetailVMFactory(ProductLogDetailFragmentArgs.fromBundle(requireArguments()).productlogitem)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        binding = FragmentProductLogDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this.viewLifecycleOwner
        binding.viewModel = viewModel
    }
}