package de.muenchen.appcenter.nimux.view.manage.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.muenchen.appcenter.nimux.datasources.UserLog

class UserLogDetailViewModel(val userLog: UserLog) : ViewModel()

class UserLogDetailVMFactory(private val userLog: UserLog) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserLogDetailViewModel::class.java))
            return UserLogDetailViewModel(userLog) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}