package de.muenchen.appcenter.nimux.view.main.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialContainerTransform
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentHelpDetailBinding

class HelpDetailFragment : Fragment() {

    private var _binding: FragmentHelpDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enter Transition
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = resources.getInteger(R.integer.motion_medium).toLong()
            scrimColor = requireContext().getColor(android.R.color.transparent)
            setAllContainerColors(requireContext().getColor(android.R.color.transparent))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHelpDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val helpItem = HelpDetailFragmentArgs.fromBundle(requireArguments()).helpItem
        binding.helpTitleTv.text = helpItem.title.replace("\\n", "\n", true)
        binding.helpBodyTv.text = helpItem.body.replace("\\n", "\n", true)

        binding.parentLayout.setOnClickListener { findNavController().popBackStack() }
        binding.helpDetailCard.setOnClickListener { findNavController().popBackStack() }
        binding.helpTitleTv.setOnClickListener { findNavController().popBackStack() }
        binding.helpBodyTv.setOnClickListener { findNavController().popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}