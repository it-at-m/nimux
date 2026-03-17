package de.muenchen.appcenter.nimux.view.manage.users

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.EditUserFragmentBinding
import de.muenchen.appcenter.nimux.datasources.ProductDataSource
import de.muenchen.appcenter.nimux.util.hideKeyboard
import de.muenchen.appcenter.nimux.util.recognition.FaceRecognitionModule.reloadFromEncryptedPrefs
import de.muenchen.appcenter.nimux.util.recognition.tflite.SimilarityClassifier
import de.muenchen.appcenter.nimux.util.showNetworkHint
import de.muenchen.appcenter.nimux.viewmodel.manage.users.EditUserViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EditUserFragment : Fragment() {

    @Inject
    lateinit var productDataSource: ProductDataSource

    private lateinit var binding: EditUserFragmentBinding

    private val viewModel: EditUserViewModel by viewModels()

    @Inject
    lateinit var faceNet: SimilarityClassifier

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.edit_user_fragment,
            container,
            false
        )
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setUpFacialRecognitionButtons()
        setupObservers()
    }

    private fun setUpFacialRecognitionButtons() {
        val userId = viewModel.user.stringSortID

        val masterKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            requireContext(),
            "secure_face_embeddings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val faceAdded = encryptedPrefs.getString(userId, null)

        if (faceAdded != null) {
            binding.editUserAddFacialDataButton.visibility = View.GONE
            binding.editUserDeleteFacialDataButton.visibility = View.VISIBLE

            binding.editUserDeleteFacialDataButton.setOnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.delete_facial_data_dialog_title))
                    .setMessage(getString(R.string.delete_facial_data_dialog_message))
                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                        encryptedPrefs.edit().remove(userId).apply()
                        faceNet.reloadFromEncryptedPrefs(requireContext())
                        setUpFacialRecognitionButtons()
                    }
                    .setNegativeButton(getString(R.string.no), null)
                    .show()
            }
            if (viewModel.user.pin != null) {
                binding.faceReplacesPinSwitch.visibility = View.VISIBLE
                binding.faceReplacesPinHintTv.visibility = View.VISIBLE
            }
        } else {
            binding.editUserDeleteFacialDataButton.visibility = View.GONE
            binding.editUserAddFacialDataButton.visibility = View.VISIBLE
            binding.editUserAddFacialDataButton.setOnClickListener {
                val user = binding.viewModel!!.user
                val action =
                    EditUserFragmentDirections.actionEditUserFragmentToAddUsersFaceFragment(user)
                findNavController().navigate(action)
            }
            binding.faceReplacesPinSwitch.visibility = View.GONE
            binding.faceReplacesPinHintTv.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        viewModel.showProgressBar.observe(viewLifecycleOwner) {
            if (it) {
                binding.progressBar.show()
                binding.editUserSaveChanges.isEnabled = false
            } else {
                binding.progressBar.hide()
                binding.editUserSaveChanges.isEnabled = true
            }
        }
        viewModel.canceled.observe(viewLifecycleOwner) {
            if (it) findNavController().popBackStack()
        }
        viewModel.hideKeyboard.observe(viewLifecycleOwner) { hideKeyboard ->
            if (hideKeyboard) {
                hideKeyboard(requireActivity())
                viewModel.doneHideKeyboard()
            }
        }
        viewModel.oldPinEmptyOrFalse.observe(viewLifecycleOwner) {
            if (it) binding.editUserConfirmOldPin.error = getString(R.string.enter_old_pin_error)
            else binding.editUserConfirmOldPin.error = null
        }
        viewModel.showNetworkHint.observe(viewLifecycleOwner) {
            if (it) {
                showNetworkHint(
                    requireContext(),
                    { _, _ -> viewModel.saveChanges() },
                    { viewModel.networkHintShown() })
            }
        }
        viewModel.newPinEmptyOrFalse.observe(viewLifecycleOwner) {
            if (it) {
                if (viewModel.makeNewPIN.value!!) binding.editUserEnterFirstPin.error =
                    getString(R.string.add_user_pin_error)
                else binding.editUserEnterNewPin.error = getString(R.string.add_user_pin_error)
            } else {
                binding.editUserEnterFirstPin.error = null
                binding.editUserEnterNewPin.error = null
            }
        }
        viewModel.confirmPinEmptyOrFalse.observe(viewLifecycleOwner) {
            if (it) {
                if (viewModel.makeNewPIN.value!!) binding.editUserConfirmFirstPin.error =
                    getString(R.string.add_user_confirm_pin_error)
                else binding.editUserConfirmNewPin.error =
                    getString(R.string.add_user_confirm_pin_error)
            } else {
                binding.editUserConfirmFirstPin.error = null
                binding.editUserConfirmNewPin.error = null
            }
        }
        viewModel.enterPinToDeactivate.observe(viewLifecycleOwner) {
            if (it) {
                val pinInputDialog = MaterialAlertDialogBuilder(requireContext())
                val view = layoutInflater.inflate(R.layout.pin_enter_dialog_layout, null)
                pinInputDialog.setTitle(getString(R.string.remove_pin_confirmation_dialog_title))
                    .setView(view)
                    .setPositiveButton(getString(R.string.confirm), null)
                    .setNegativeButton(
                        getString(R.string.cancel)
                    ) { _, _ ->
                        viewModel.pinEnterShown()
                    }
                    .setOnDismissListener {
                        viewModel.pinEnterShown()
                    }
                val dialog = pinInputDialog.show()

                val pButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                pButton.setOnClickListener {
                    val et = view.findViewById<TextInputLayout>(R.id.pin_enter_dialog_edittext)

                    if (viewModel.checkPin(et.editText?.text.toString())
                    ) {
                        viewModel.pinEnterShown()
                        dialog.dismiss()
                        viewModel.saveChanges(true)
                    } else {
                        et.error = getString(R.string.wrong_pin_input)
                    }
                }
            }
        }
        viewModel.updated.observe(viewLifecycleOwner) {
            if (it) {
                if (EditUserFragmentArgs.fromBundle(requireArguments()).comingFromOverview)
                    findNavController().navigate(EditUserFragmentDirections.actionEditUserFragmentToNavOverview())
                else
                    findNavController().navigate(
                        EditUserFragmentDirections.actionEditUserFragmentToManageUserItem(
                            viewModel.user
                        )
                    )

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_user_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.history_action -> {
                findNavController().navigate(
                    EditUserFragmentDirections.actionEditUserFragmentToUserPersonalHistoryFragment(
                        viewModel.user
                    )
                )
            }

            R.id.delete_stats_action -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.delete_user_stats_alert_title))
                    .setMessage(getString(R.string.delete_user_stats_alert_message))
                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                        productDataSource.getUserStatQuery(viewModel.user.stringSortID).get()
                            .addOnSuccessListener {
                                it.documents.forEach { doc ->
                                    doc.reference.delete()
                                }
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.user_stats_delete_success_toast) + " " + viewModel.user.name,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_delete_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }.setNegativeButton(getString(R.string.no), null)
                    .show()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}