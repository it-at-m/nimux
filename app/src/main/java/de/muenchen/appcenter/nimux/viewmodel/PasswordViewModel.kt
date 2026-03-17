package de.muenchen.appcenter.nimux.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.internal.Provider
import javax.inject.Inject

@HiltViewModel
class PasswordViewModel @Inject constructor(
    private val tenantRefProvider: Provider<DocumentReference>
) : ViewModel() {



    private val passwordColl: CollectionReference?
        get() = tenantRefProvider.get()?.collection("ManagePWCollection")

    private fun requirePasswordColl(): CollectionReference {
        return passwordColl
            ?: throw IllegalStateException("Tenant missing – User is not logged in")
    }


    var pwText: String = ""

    private val _pwCorrect = MutableLiveData<Int>()
    val pwCorrect: LiveData<Int>
        get() = _pwCorrect

    fun checkPW() {
        requirePasswordColl().document("ManagePWDoc").get(Source.SERVER).addOnSuccessListener {
            if (it != null) {
                val rcvdPW = it.toObject(PsWrd::class.java)
                if (rcvdPW!!.pw == pwText)
                    _pwCorrect.value = 1
                else
                    _pwCorrect.value = 0
            }
        }
    }

    fun pwWasCorrect() {
        _pwCorrect.value = 2
    }

    data class PsWrd(
        val pw: String = "",
    )
}