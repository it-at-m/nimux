package de.muenchen.appcenter.nimux.view.main.specialorder

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentAusgebenConfirmBinding
import de.muenchen.appcenter.nimux.datasources.DonateItemDataSource
import de.muenchen.appcenter.nimux.model.DonateItem
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.util.faceRecognitionPrefKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import javax.inject.Inject

/**
 * This functionality is currently not used.
 */
@AndroidEntryPoint
class DonationConfirmFragment : Fragment() {

    @Inject
    lateinit var donateItemDataSource: DonateItemDataSource

    private var _binding: FragmentAusgebenConfirmBinding? = null
    private val binding get() = _binding!!
    private lateinit var timer: CountDownTimer

    private lateinit var user: User
    private lateinit var donateItem: DonateItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.motion_medium).toLong()
            scrimColor = requireContext().getColor(android.R.color.transparent)
            setAllContainerColors(requireContext().getColor(android.R.color.transparent))
        }
        _binding = FragmentAusgebenConfirmBinding.inflate(layoutInflater, container, false)
        donateItem = DonationConfirmFragmentArgs.fromBundle(requireArguments()).donateItem
        user = DonationConfirmFragmentArgs.fromBundle(requireArguments()).user
        binding.item = donateItem
        binding.user = user
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val donateItem = binding.item!!
        if (donateItem.timeLimit > 0) {
            binding.timeLimitTv.text = getString(
                R.string.available_until_title,
                SimpleDateFormat("dd.MM.yyyy HH:mm").format(donateItem.timeLimit)
            )
        }

        binding.buttonConfirm.setOnClickListener { confirmDono() }
        binding.buttonCancel.setOnClickListener { findNavController().popBackStack() }
        binding.progressBar.visibility = View.VISIBLE
        startTimer(6, 1)
    }

    private fun confirmDono() {
        timer.cancel()
        val moveToAuto =
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
                faceRecognitionPrefKey, false
            )
        lifecycleScope.launch(Dispatchers.IO) {
            donateItemDataSource.addDonationItem(donateItem)
            lifecycleScope.launch(Dispatchers.Main) {
                val cardTransName = getString(R.string.home_dono_card_trans_name)
                val nameTransName = getString(R.string.home_dono_name_trans_name)
//                val extras = FragmentNavigatorExtras(binding.parentCardview to cardTransName,
//                    binding.nameTitle to nameTransName)
                if (moveToAuto) {
                    val action =
                        DonationConfirmFragmentDirections.actionAusgebenConfirmFragmentToNavHomeAuto()
                    findNavController().navigate(action)
                } else {
                    val action =
                        DonationConfirmFragmentDirections.actionAusgebenConfirmFragmentToNavHomeManual()
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun startTimer(secs: Int, delay: Int) {
        binding.progressBar.max = secs * 1000
        timer = object : CountDownTimer(((secs + delay) * 1000).toLong(), 30) {
            override fun onTick(leftTimeInMilliseconds: Long) {
                if (secs.times(1000).plus(
                        delay.times(1000)
                            .div(2)
                    ) > leftTimeInMilliseconds && !binding.progressBar.isVisible
                )
                    binding.progressBar.visibility =
                        View.VISIBLE
                binding.progressBar.setProgressCompat(
                    secs * 1000 - (leftTimeInMilliseconds).toInt(),
                    true
                )
                binding.progressBar.setProgressCompat(
                    secs * 1000 - (leftTimeInMilliseconds).toInt(),
                    true
                )
            }

            override fun onFinish() {
                confirmDono()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel()
        _binding = null
    }
}