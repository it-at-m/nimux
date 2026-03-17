package de.muenchen.appcenter.nimux.view.main.home

import android.animation.Animator
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
import de.muenchen.appcenter.nimux.databinding.FragmentHomeCheckoutBinding
import de.muenchen.appcenter.nimux.datasources.DonateItemDataSource
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.faceRecognitionPrefKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeCheckoutFragment : Fragment() {

    @Inject lateinit var donateItemDataSource: DonateItemDataSource
    @Inject lateinit var usersRepository: UsersRepository

    private var _binding: FragmentHomeCheckoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var timer: CountDownTimer

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
        _binding = FragmentHomeCheckoutBinding.inflate(inflater, container, false)
        binding.user = HomeCheckoutFragmentArgs.fromBundle(requireArguments()).user
        val product = HomeCheckoutFragmentArgs.fromBundle(requireArguments()).product
        binding.product = product
        return binding.root
    }


    private fun startTimer(secs: Int, delay: Int) {
        binding.checkoutProgressStart.max = secs * 1000
        binding.checkoutProgressEnd.max = secs * 1000
        timer = object : CountDownTimer(((secs + delay) * 1000).toLong(), 30) {
            override fun onTick(leftTimeInMilliseconds: Long) {
                if (secs.times(1000).plus(delay.times(1000).div(2)) > leftTimeInMilliseconds) {
                    if (!binding.checkoutProgressStart.isVisible) binding.checkoutProgressStart.visibility =
                        View.VISIBLE
                    if (!binding.checkoutProgressEnd.isVisible) binding.checkoutProgressEnd.visibility =
                        View.VISIBLE
                }
                binding.checkoutProgressStart.setProgressCompat(secs * 1000 - (leftTimeInMilliseconds).toInt(),
                    true)
                binding.checkoutProgressEnd.setProgressCompat(secs * 1000 - (leftTimeInMilliseconds).toInt(),
                    true)
            }

            override fun onFinish() {
                purchaseProduct()
            }
        }.start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (HomeCheckoutFragmentArgs.fromBundle(requireArguments()).productDetected) {
            setupButtons()
            binding.homeCheckoutTitle.text =
                getString(R.string.confirm_payment_detected_title, binding.product!!.name)
        } else {
            binding.homeCheckoutTitle.text =
                getString(R.string.checkout_title_thanks, binding.user!!.name)
            if (binding.user?.toPay!!.minus(binding.product!!.price) < 0.0) {
                startTimer(6, 1)
            } else {
                startTimer(5, 0)
            }
        }

        binding.homeCheckoutButtonCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.homeCheckoutButtonClose.setOnClickListener {
            purchaseProduct()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val donateItem = donateItemDataSource.getDonationItem(binding.product?.price!!)
            if (donateItem != null) {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.homeCheckoutUserCreditTitle.text = getString(R.string.credit)
                    binding.homeCheckoutProductCost.visibility = View.GONE
                    binding.homeCheckoutUserNewCredit.visibility = View.GONE
                    binding.homeCheckoutUserNewCreditTitle.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        if (::timer.isInitialized)
            timer.cancel()
        super.onDestroy()
    }

    fun purchaseProduct() {
        if (::timer.isInitialized)
            timer.cancel()
        val metrics = resources.displayMetrics
        val px = metrics.heightPixels

        binding.checkoutCard.animate().translationY(px.toFloat())
            .setDuration(resources.getInteger(R.integer.motion_short).toLong())
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val args = HomeCheckoutFragmentArgs.fromBundle(requireArguments())
                        usersRepository.payProduct(binding.user!!.stringSortID,
                            binding.product!!,
                            faceDetected = args.faceDetected,
                            productDetected = args.productDetected)
                    }
                }

                override fun onAnimationEnd(p0: Animator) {
                    if (!PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
                            faceRecognitionPrefKey, false)
                    ) findNavController().navigate(HomeCheckoutFragmentDirections.actionHomeCheckoutFragmentToNavHomeManual())
                    else
                        findNavController().navigate(HomeCheckoutFragmentDirections.actionHomeCheckoutFragmentToNavHomeAuto())
                }
                override fun onAnimationCancel(p0: Animator) {}
                override fun onAnimationRepeat(p0: Animator) {}
            })

    }

    private fun setupButtons() {
        val fromFaceRecon = HomeCheckoutFragmentArgs.fromBundle(requireArguments()).faceDetected
        if(fromFaceRecon){
            binding.buttonManualUserSelect.visibility = View.VISIBLE
            binding.buttonManualUserSelect.setOnClickListener {
                findNavController().navigate(HomeCheckoutFragmentDirections.actionHomeCheckoutFragmentToNavHomeManual())
            }
        }
        binding.buttonManualProductSelect.visibility = View.VISIBLE
        binding.buttonManualProductSelect.setOnClickListener {
            findNavController().navigate(HomeCheckoutFragmentDirections.actionHomeCheckoutFragmentToHomeProductFragment(
                binding.user!!, fromFaceRecon))
        }
    }


}