package de.muenchen.appcenter.nimux.viewmodel.manage.users

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.md5
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditUserViewModel @Inject constructor(
    private val usersRepository: UsersRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var user: User = savedStateHandle.get<User>("currentUser")
        ?: error("User missing in SavedStateHandle")

    private val _showCredit = MutableLiveData(user.showCredit)
    val showCredit: LiveData<Boolean>
        get() = _showCredit
    private val _processData = MutableLiveData(user.collectData)
    val processData: LiveData<Boolean>
        get() = _processData
    private val _requirePin = MutableLiveData(user.pin != null)
    val requirePin: LiveData<Boolean>
        get() = _requirePin
    private val _makeNewPin = MutableLiveData(false)
    val makeNewPIN: LiveData<Boolean>
        get() = _makeNewPin
    private val _changePin = MutableLiveData(user.pin != null)
    val changePin: LiveData<Boolean>
        get() = _changePin

    private val _useProductAI: MutableLiveData<Boolean> =
        if (user.useProductAI != null) MutableLiveData(user.useProductAI)
        else MutableLiveData(false)
    val useProductAi get():LiveData<Boolean> = _useProductAI
    private val _faceSkipsPin = MutableLiveData(user.faceSkipsPin)
    val faceSkipsPin get():LiveData<Boolean> = _faceSkipsPin

    var oldPinText: String = ""
    var newPinText: String = ""
    var confirmNewPinText: String = ""

    private val _showProgressBar = MutableLiveData(false)
    val showProgressBar: LiveData<Boolean>
        get() = _showProgressBar

    private val _showNetworkHint = MutableLiveData(false)
    val showNetworkHint: LiveData<Boolean>
        get() = _showNetworkHint

    private val _oldPinEmptyOrFalse = MutableLiveData<Boolean>()
    val oldPinEmptyOrFalse: LiveData<Boolean>
        get() = _oldPinEmptyOrFalse
    private val _newPinEmptyOrFalse = MutableLiveData<Boolean>()
    val newPinEmptyOrFalse: LiveData<Boolean>
        get() = _newPinEmptyOrFalse
    private val _confirmPinEmptyOrFalse = MutableLiveData<Boolean>()
    val confirmPinEmptyOrFalse: LiveData<Boolean>
        get() = _confirmPinEmptyOrFalse


    private val _enterPinToDeactivate = MutableLiveData<Boolean>()
    val enterPinToDeactivate: LiveData<Boolean>
        get() = _enterPinToDeactivate

    private val _updated = MutableLiveData<Boolean>()
    val updated: LiveData<Boolean>
        get() = _updated

    fun switchRequirePin() {
        _requirePin.value = !requirePin.value!!
        if (requirePin.value!!) {
            if (user.pin == null) {
                _makeNewPin.value = true
            } else {
                _changePin.value = true
            }
        } else {
            doHideKeyboard()
            _makeNewPin.value = false
            _changePin.value = false
            oldPinText = ""
            newPinText = ""
            confirmNewPinText = ""
        }
    }

    fun saveChanges(pinConfirmed: Boolean = false) {
        var pinIsConfirmed = pinConfirmed
        val stateShowCredit = showCredit.value
        val stateProcessData = processData.value
        val stateRequirePin = requirePin.value
        val stateNewPin = makeNewPIN.value
        val stateChangePin = changePin.value
        val useProdAi = useProductAi.value
        val facePinSkipper = faceSkipsPin.value
        val stateOldPinInput = oldPinText
        val stateNewPinInput = newPinText
        val stateConfirmPinInput = confirmNewPinText
        var newPin: String? = null

        if (stateRequirePin!!) {
            pinIsConfirmed = true
            if (stateNewPin!!) {
                _oldPinEmptyOrFalse.value = false
                _newPinEmptyOrFalse.value =
                    stateNewPinInput.length != 4 || stateNewPinInput.toIntOrNull() == null
                _confirmPinEmptyOrFalse.value =
                    stateConfirmPinInput.length != 4 || stateConfirmPinInput.toIntOrNull() == null || stateConfirmPinInput != stateNewPinInput
            } else if (stateChangePin!!) {
                if (stateOldPinInput.isNotBlank()) {
                    _oldPinEmptyOrFalse.value =
                        stateOldPinInput.length != 4 || stateOldPinInput.toIntOrNull() == null || md5(
                            stateOldPinInput
                        ) != user.pin
                    _newPinEmptyOrFalse.value =
                        stateNewPinInput.length != 4 || stateNewPinInput.toIntOrNull() == null
                    _confirmPinEmptyOrFalse.value =
                        stateConfirmPinInput.length != 4 || stateConfirmPinInput.toIntOrNull() == null || stateConfirmPinInput != stateNewPinInput
                } else {
                    _oldPinEmptyOrFalse.value = false
                    _newPinEmptyOrFalse.value = false
                    _confirmPinEmptyOrFalse.value = false
                }
            }
            if (!oldPinEmptyOrFalse.value!! && !newPinEmptyOrFalse.value!! && !confirmPinEmptyOrFalse.value!!) {
                newPin = if (stateChangePin!! && stateOldPinInput.isEmpty()) user.pin
                else md5(stateNewPinInput)
                pinIsConfirmed = true
            }
        } else {
            if (!pinIsConfirmed && user.pin != null) {
                _enterPinToDeactivate.value = true
            } else {
                pinIsConfirmed = true
                _oldPinEmptyOrFalse.value = false
                _newPinEmptyOrFalse.value = false
                _confirmPinEmptyOrFalse.value = false
            }
        }
        if (pinIsConfirmed && !oldPinEmptyOrFalse.value!! && !newPinEmptyOrFalse.value!! && !confirmPinEmptyOrFalse.value!!) {
            viewModelScope.launch {
                _showProgressBar.value = true
                if (usersRepository.connectedOnline()) {
                    _showNetworkHint.value = false
                    usersRepository.updateUser(
                        user.stringSortID,
                        stateShowCredit!!,
                        stateProcessData!!,
                        newPin,
                        useProdAi ?: false,
                        facePinSkipper ?: false
                    )
                } else _showNetworkHint.value = true
                _showProgressBar.value = false
                val getUser = usersRepository.getUser(user.stringSortID)
                if (getUser != null) {
                    user = getUser
                    _updated.value = true
                }
            }
        }
    }

    fun pinEnterShown() {
        _enterPinToDeactivate.value = false
    }

    fun checkPin(pinToProof: String): Boolean = md5(pinToProof) == user.pin

    fun switchShowCredit() {
        _showCredit.value = !showCredit.value!!
    }

    fun switchProcessData() {
        _processData.value = !processData.value!!
    }

    fun switchUseProdAI() {
        _useProductAI.value = !useProductAi.value!!
    }

    fun switchFaceSkipsPin() {
        _faceSkipsPin.value = !faceSkipsPin.value!!
    }

    private val _canceled = MutableLiveData<Boolean>()
    val canceled: LiveData<Boolean>
        get() = _canceled

    fun cancel() {
        _canceled.value = true
    }


    private val _hideKeyboard = MutableLiveData<Boolean>()
    val hideKeyboard: LiveData<Boolean>
        get() = _hideKeyboard

    private fun doHideKeyboard() {
        _hideKeyboard.value = true
    }

    fun doneHideKeyboard() {
        _hideKeyboard.value = false
    }

    fun networkHintShown() {
        _showNetworkHint.value = false
    }
}