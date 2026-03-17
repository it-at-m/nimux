package de.muenchen.appcenter.nimux.view.main.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.muenchen.appcenter.nimux.databinding.FragmentStoreBinding

class StoreFragment : Fragment() {

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        animateViewsIn()
        binding.selectUserButton.setOnClickListener {
            findNavController().navigate(StoreFragmentDirections.actionNavStoreToStoreUserFragment())
        }
    }

    private fun animateViewsIn() {
        val animTime = 160L
        val delayTime = 80L
        binding.titleCard.apply {
            alpha = 0f
            visibility = View.VISIBLE
            translationY = 40f
        }
        binding.nameColorCard.apply {
            alpha = 0f
            visibility = View.VISIBLE
            translationY = 40f
        }
        binding.nameBoldCard.apply {
            alpha = 0f
            visibility = View.VISIBLE
            translationY = 40f
        }
        binding.emojiCard.apply {
            alpha = 0f
            visibility = View.VISIBLE
            translationY = 40f
        }
        binding.selectUserButton.apply {
            alpha = 0f
            visibility = View.VISIBLE
            translationY = 40f
        }
        binding.supportCard.apply {
            alpha = 0f
            visibility = View.VISIBLE
            translationY = 40f
        }
        binding.titleCard.animate().alpha(1f).translationY(0f).setDuration(animTime)
            .setStartDelay(delayTime).start()
        binding.nameColorCard.animate().alpha(1f).translationY(0f).setDuration(animTime)
            .setStartDelay(delayTime.times(3)).start()
        binding.emojiCard.animate().alpha(1f).translationY(0f).setDuration(animTime)
            .setStartDelay(delayTime.times(5)).start()
        binding.nameBoldCard.animate().alpha(1f).translationY(0f).setDuration(animTime)
            .setStartDelay(delayTime.times(7)).start()
        binding.supportCard.animate().alpha(1f).translationY(0f).setDuration(animTime)
            .setStartDelay(delayTime.times(9)).start()
        binding.selectUserButton.animate().alpha(1f).translationY(0f).setDuration(animTime)
            .setStartDelay(delayTime.times(11)).start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}