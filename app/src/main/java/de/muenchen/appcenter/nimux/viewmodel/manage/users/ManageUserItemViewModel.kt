package de.muenchen.appcenter.nimux.viewmodel.manage.users


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageUserItemViewModel @Inject constructor(
    private val usersRepository: UsersRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val user: User = savedStateHandle.get<User>("currentUser")
        ?: error("User missing in SavedStateHandle")

    private var buttonAtClick = 0
    private var customValue = 0.0

    val zeroPayConst = "0,00€"
    val posNegCredit = String.format("%.2f", user.toPay).replace("-", "") + "€"

    private val _userName = MutableLiveData(user.name)
    val userName: LiveData<String> get() = _userName

    private val _userPay = MutableLiveData(String.format("%.2f", user.toPay) + "€")
    val userPay: LiveData<String> get() = _userPay

    val checkedButton = MutableLiveData<Int>(0)

    private val _customAmountChecked = MutableLiveData(false)
    val customAmountChecked: LiveData<Boolean> get() = _customAmountChecked

    val customAmount = MutableLiveData<String>("")

    private val _exactZeroToast = MutableLiveData<Boolean>()
    val exactZeroToast: LiveData<Boolean> get() = _exactZeroToast

    private val _canceled = MutableLiveData<Boolean>()
    val canceled: LiveData<Boolean> get() = _canceled

    private val _deleted = MutableLiveData<Boolean>()
    val deleted: LiveData<Boolean> get() = _deleted

    private val _payed = MutableLiveData<Boolean>()
    val payed: LiveData<Boolean> get() = _payed

    private val _notADouble = MutableLiveData<Boolean>()
    val notADouble: LiveData<Boolean> get() = _notADouble

    private val _startsWithMin = MutableLiveData<Boolean>()
    val startsWithMin: LiveData<Boolean> get() = _startsWithMin

    private val _networkHint = MutableLiveData<Boolean>()
    val networkHint: LiveData<Boolean> get() = _networkHint

    private val _noChipSelected = MutableLiveData<Boolean>()
    val noChipSelected: LiveData<Boolean> get() = _noChipSelected

    private val _showProgressBar = MutableLiveData<Boolean>()
    val showProgressBar: LiveData<Boolean> get() = _showProgressBar

    fun checkedChanged() {
        _customAmountChecked.value = checkedButton.value == R.id.chip_other
    }

    fun cancel() {
        _canceled.value = true
    }

    fun deleteCurrentUser() {
        viewModelScope.launch {
            _showProgressBar.value = true
            usersRepository.deleteUser(user.stringSortID)
            _showProgressBar.value = false
            _deleted.value = true
        }
    }

    fun payClickCheck() {
        val aCustomAmount = customAmount.value.orEmpty().replace(",", ".")
        _notADouble.value = aCustomAmount.toDoubleOrNull() == null
        _startsWithMin.value = aCustomAmount.contains('-')
        buttonAtClick = checkedButton.value ?: 0
        customValue =
            if (buttonAtClick == R.id.chip_other &&
                _notADouble.value == false &&
                _startsWithMin.value == false
            )
                aCustomAmount.toDouble()
            else 0.0
        _noChipSelected.value = buttonAtClick == 0
        if (_noChipSelected.value == false) {
            if (user.toPay == 0.0) {
                if (buttonAtClick == R.id.chip_exact) _exactZeroToast.value = true
                else pay()
            } else pay()
        }
    }

    fun pay() {
        viewModelScope.launch {
            _showProgressBar.value = true
            if (usersRepository.connectedOnline()) {
                when (buttonAtClick) {
                    R.id.chip_other -> {
                        if (_notADouble.value == false && _startsWithMin.value == false) {
                            usersRepository.addCredit(user.stringSortID, customValue)
                            _payed.value = true
                        }
                    }

                    R.id.chip_exact -> {
                        usersRepository.addCredit(user.stringSortID, user.toPay.unaryMinus())
                        _payed.value = true
                    }

                    R.id.chip_5 -> {
                        usersRepository.addCredit(user.stringSortID, 5.0)
                        _payed.value = true
                    }

                    R.id.chip_10 -> {
                        usersRepository.addCredit(user.stringSortID, 10.0)
                        _payed.value = true
                    }

                    R.id.chip_20 -> {
                        usersRepository.addCredit(user.stringSortID, 20.0)
                        _payed.value = true
                    }
                }
            } else _networkHint.value = true
            _showProgressBar.value = false
        }
    }

    fun networkHintShown() {
        _networkHint.value = false
    }

    fun noChipHintShown() {
        _noChipSelected.value = false
    }

    fun exactZeroShown() {
        _exactZeroToast.value = false
    }

    fun resetChip() {
        checkedButton.value = 0
    }
}
