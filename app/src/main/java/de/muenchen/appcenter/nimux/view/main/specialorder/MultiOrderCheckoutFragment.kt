package de.muenchen.appcenter.nimux.view.main.specialorder

import android.animation.Animator
import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentMultiOrderCheckoutBinding
import de.muenchen.appcenter.nimux.model.MultiOrderProductListWithProduct
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.MultiOrderOverviewAdapter
import de.muenchen.appcenter.nimux.util.faceRecognitionPrefKey
import de.muenchen.appcenter.nimux.util.round
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MultiOrderCheckoutFragment : Fragment() {

    @Inject
    lateinit var usersRepository: UsersRepository

    private lateinit var timer: CountDownTimer
    private var _binding: FragmentMultiOrderCheckoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var overviewAdapter: MultiOrderOverviewAdapter
    private var totalSum = 0.0
    private var boughtProducts = listOf<MultiOrderProductListWithProduct>()

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
        _binding = FragmentMultiOrderCheckoutBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.user = MultiOrderCheckoutFragmentArgs.fromBundle(requireArguments()).currentUser
        boughtProducts =
            MultiOrderCheckoutFragmentArgs.fromBundle(requireArguments()).boughtProducts.toList()
        overviewAdapter = MultiOrderOverviewAdapter()
        binding.overviewRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = overviewAdapter
        }
        overviewAdapter.submitList(boughtProducts)
        boughtProducts.forEach {
            if (it.multiOrderProductList.amount != 0) {
                totalSum += it.multiOrderProductList.amount.toDouble() * it.multiOrderProductList.price
                totalSum = totalSum.round(2)
            }
        }
        binding.totalAmount.text =
            String.format("%.2f", totalSum) + " €"
        binding.newCredit.text =
            String.format("%.2f", binding.user!!.toPay - totalSum) + " €"
        if (binding.user!!.toPay - totalSum < 0) binding.homeCheckoutLowCreditWarnSoft.visibility =
            View.VISIBLE

        if (binding.user?.toPay!!.minus(totalSum) < 0.0) {
            startTimer(7, 1)
        } else {
            startTimer(6, 0)
        }
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.confirmButton.setOnClickListener {
            purchaseProducts()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel()
        _binding = null
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
                purchaseProducts()
            }
        }.start()
    }

    private fun purchaseProducts() {
        timer.cancel()

        val moveToAuto =
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
                faceRecognitionPrefKey, false
            )

        val metrics = resources.displayMetrics
        val px = metrics.heightPixels
//        Log.d("PixelLog", "${metrics.widthPixels} ${metrics.heightPixels} $px")

        binding.multiOrderCheckoutCard.animate().translationY(px.toFloat())
            .setDuration(resources.getInteger(R.integer.motion_short).toLong())
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        usersRepository.buyMultipleProducts(
                            binding.user!!.stringSortID,
                            boughtProducts
                        )
                    }
                }

                override fun onAnimationEnd(p0: Animator) {
                    if (moveToAuto) {
                        val action =
                            MultiOrderCheckoutFragmentDirections.actionMultiOrderCheckoutFragmentToNavHomeAuto()
                        findNavController().navigate(action)
                    } else {
                        val action =
                            MultiOrderCheckoutFragmentDirections.actionMultiOrderCheckoutFragmentToNavHomeManual()
                        findNavController().navigate(action)
                    }
                }

                override fun onAnimationCancel(p0: Animator) {
                }

                override fun onAnimationRepeat(p0: Animator) {
                }

            })
    }

}