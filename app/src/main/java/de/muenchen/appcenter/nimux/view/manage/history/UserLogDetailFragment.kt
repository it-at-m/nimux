package de.muenchen.appcenter.nimux.view.manage.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.muenchen.appcenter.nimux.databinding.FragmentUserLogDetailBinding

class UserLogDetailFragment : Fragment() {

    private lateinit var binding: FragmentUserLogDetailBinding
    private val viewModel: UserLogDetailViewModel by viewModels {
        UserLogDetailVMFactory(UserLogDetailFragmentArgs.fromBundle(requireArguments()).userlogitem)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentUserLogDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this.viewLifecycleOwner
        binding.viewModel = viewModel
    }

}