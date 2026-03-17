package de.muenchen.appcenter.nimux.view.main.store

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentEditUserCustomizationBinding
import de.muenchen.appcenter.nimux.model.NameColors
import de.muenchen.appcenter.nimux.util.getFirstEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class EditUserCustomizationFragment : Fragment() {

    private var _binding: FragmentEditUserCustomizationBinding? = null
    private val binding get() = _binding!!
    private val mViewModel: EditUserCustomizationViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditUserCustomizationBinding.inflate(inflater, container, false)
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpUserObservers()
        setBuyListeners()
        setUpEmojiListener()
        setButtonLayoutListeners()
        setUpOtherObservers()
        setupRadioButtons()
    }


    private fun setButtonLayoutListeners() {
        binding.buttonSaveChanges.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                if (mViewModel.saveChanges()) lifecycleScope.launch(Dispatchers.Main) {
                    findNavController().popBackStack()
                }
                else Toast.makeText(
                    requireContext(),
                    getString(R.string.error_saving_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setUpOtherObservers() {
        mViewModel.showProgressbar.observe(viewLifecycleOwner) {
            if (it) binding.progressBar.show()
            else binding.progressBar.hide()
        }
    }

    private fun setUpEmojiListener() {
        binding.enterEmojiInputLayout.editText?.doOnTextChanged { text, start, before, count ->
            Timber.d("EmojiText $before $start $count")
            if (text != null && text.isNotEmpty()) {
                if (start == 0) {
                    val filteredString = getFirstEmoji(text.toString())
                    mViewModel.emojiTextChanged(filteredString)
                }
            } else mViewModel.emojiTextChanged("")
        }
    }

    private fun setBuyListeners() {
        binding.buttonUnlockBold.setOnClickListener {
            showBuyDialog {
                mViewModel.buyBoldText()
            }
        }
        binding.buttonUnlockEmoji.setOnClickListener {
            showBuyDialog {
                mViewModel.buyEmoji()
            }
        }
        binding.buttonUnlockColor.setOnClickListener {
            showBuyDialog {
                mViewModel.buyNameColors()
            }
        }
    }

    private fun showBuyDialog(onPositive: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_purchase_title))
            .setMessage(getString(R.string.confirm_purchase_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                onPositive()
            }
            .show()
    }

    private fun setUpUserObservers() {
        mViewModel.fakeUser.observe(viewLifecycleOwner) { user ->
            binding.userPreviewCard.user = user
        }
    }

    private fun setupRadioButtons() {
        when (mViewModel.realUser.value?.nameColor) {
            NameColors.PURPLE -> binding.radioColorAmethyst.isChecked = true
            NameColors.PINK -> binding.radioColorMagenta.isChecked = true
            NameColors.ORANGE -> binding.radioColorOrange.isChecked = true
            NameColors.BLUE -> binding.radioColorCapri.isChecked = true
            NameColors.CYAN -> binding.radioColorSeaGreen.isChecked = true
            else -> binding.radioColorDefault.isChecked = true
        }
        binding.nameColorRadioGroup.setOnCheckedChangeListener { radioGroup, _ ->
            when (radioGroup.checkedRadioButtonId) {
                R.id.radio_color_amethyst -> mViewModel.setNewNameColor(NameColors.PURPLE)
                R.id.radio_color_magenta -> mViewModel.setNewNameColor(NameColors.PINK)
                R.id.radio_color_orange -> mViewModel.setNewNameColor(NameColors.ORANGE)
                R.id.radio_color_capri -> mViewModel.setNewNameColor(NameColors.BLUE)
                R.id.radio_color_sea_green -> mViewModel.setNewNameColor(NameColors.CYAN)
                else -> mViewModel.setNewNameColor(NameColors.DEFAULT)
            }
        }
    }
}