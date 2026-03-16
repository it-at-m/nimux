package de.muenchen.appcenter.nimux.viewmodel.manage.users

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.muenchen.appcenter.nimux.datasources.UserSuggestionDataSource
import de.muenchen.appcenter.nimux.model.User
import javax.inject.Inject

@HiltViewModel
class ManageUsersViewModel @Inject constructor(
    private val userSuggestionDataSource: UserSuggestionDataSource
) : ViewModel() {
    private val _navToAddUser = MutableLiveData<Boolean>()
    val navToAddUser: LiveData<Boolean>
        get() = _navToAddUser

    fun performNavAddUser() {
        _navToAddUser.value = true
    }

    fun deleteUserSuggestion(userId: String) {
        userSuggestionDataSource.deleteUserSuggestion(userId)
    }

    suspend fun userSuggestionApproved(user: User): Boolean {
        return userSuggestionDataSource.userSuggestionApproved(user)
    }
}