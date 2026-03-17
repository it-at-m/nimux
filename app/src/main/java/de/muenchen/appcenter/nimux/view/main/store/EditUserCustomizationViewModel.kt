package de.muenchen.appcenter.nimux.view.main.store

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.muenchen.appcenter.nimux.datasources.UserDataSource
import de.muenchen.appcenter.nimux.model.NameColors
import de.muenchen.appcenter.nimux.model.User
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EditUserCustomizationViewModel @Inject constructor(
    private val userDataSource: UserDataSource, private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vmUser: User = savedStateHandle.get<User>("user")
        ?: error("User missing in SavedStateHandle")


    val realUser: LiveData<User> = userDataSource.getObservableUser(vmUser.stringSortID)
    private val _fakeUser = MutableLiveData(vmUser)
    val fakeUser: LiveData<User> get() = _fakeUser
    private val _showProgressbar = MutableLiveData<Boolean>(false)
    val showProgressbar: LiveData<Boolean> get() = _showProgressbar

    fun switchBoldStyle() {
        if (fakeUser.value?.boldEnabled != null) {
            _fakeUser.value?.boldEnabled = !fakeUser.value?.boldEnabled!!
            _fakeUser.value =
                _fakeUser.value // We call this so the observer gets notified about the change in the variable
            Timber.d("BoldCheck: bold should now be ${fakeUser.value?.boldEnabled}")
        }
    }

    fun buyBoldText() {
        viewModelScope.launch(Dispatchers.IO) {
            realUser.value?.stringSortID?.let { userDataSource.buyBoldText(it) }
        }
        _fakeUser.value?.boldEnabled = false
    }

    fun emojiTextChanged(newText: String) {
        _fakeUser.value?.emojiIcon = newText
        _fakeUser.value =
            _fakeUser.value // We call this so the observer gets notified about the change in the variable
    }

    fun buyEmoji() {
        viewModelScope.launch(Dispatchers.IO) {
            realUser.value?.stringSortID?.let { userDataSource.buyEmojiIcon(it) }
        }
        _fakeUser.value?.emojiIcon = ""
    }

    fun buyNameColors() {
        viewModelScope.launch(Dispatchers.IO) {
            realUser.value?.stringSortID?.let { userDataSource.buyNameColor(it) }
        }
        _fakeUser.value?.nameColor = NameColors.DEFAULT
    }

    suspend fun saveChanges(): Boolean {
        val result = CompletableDeferred<Boolean>()

        withContext(Dispatchers.Main) {
            _showProgressbar.value = true
        }
        result.invokeOnCompletion {
            viewModelScope.launch(Dispatchers.Main) {
                _showProgressbar.value = false
            }
        }

        if (realUser.value == null || fakeUser.value == null) result.complete(false)
        viewModelScope.launch(Dispatchers.IO) {
            val savedSuccessfully = userDataSource.saveCustomizationChanges(
                userID = realUser.value!!.stringSortID ?: "",
                boldEnabled = fakeUser.value!!.boldEnabled,
                emojiIcon = fakeUser.value!!.emojiIcon,
                nameColor = fakeUser.value!!.nameColor
            )
            if (savedSuccessfully) result.complete(true)
            else result.complete(false)
        }
        return result.await()
    }

    fun setNewNameColor(color: NameColors) {
        _fakeUser.value?.nameColor = color
        _fakeUser.value = _fakeUser.value
    }
}