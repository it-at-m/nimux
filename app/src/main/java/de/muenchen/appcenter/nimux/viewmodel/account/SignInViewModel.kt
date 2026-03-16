package de.muenchen.appcenter.nimux.viewmodel.account

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import de.muenchen.appcenter.nimux.util.UserSessionManager
import de.muenchen.appcenter.nimux.util.await
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(private val userSessionManager: UserSessionManager) :
    ViewModel() {

    private lateinit var auth: FirebaseAuth

    val emailInput = MutableLiveData("")
    val pwInput = MutableLiveData("")

    val emailEmpty = MutableLiveData<Boolean>()
    val pwEmpty = MutableLiveData<Boolean>()
    lateinit var toastMessage: String

    private val _createAccError = MutableLiveData<Boolean>()
    val createAccError: LiveData<Boolean>
        get() = _createAccError

    private val _loginError = MutableLiveData<Boolean>()
    val loginError: LiveData<Boolean>
        get() = _loginError

    private val _showAccountCreationHint = MutableLiveData<Boolean>()
    val showAccountCreationHint: LiveData<Boolean>
        get() = _showAccountCreationHint

    private val _showAccountCreationIsDisabledHint = MutableLiveData<Boolean>()
    val showAccountCreationIsDisabledHint: LiveData<Boolean>
        get() = _showAccountCreationIsDisabledHint

    private val _userLoggedIn = MutableLiveData<Boolean>()
    val userLoggedIn: LiveData<Boolean>
        get() = _userLoggedIn

    val showPrivacyDialog = MutableLiveData<Boolean>()

    private val _showProgressBar = MutableLiveData<Boolean>()
    val showProgressBar: LiveData<Boolean>
        get() = _showProgressBar

    private fun loggedIn() {
        _userLoggedIn.value = true
    }

    fun createClick() {
        emailEmpty.value = emailInput.value?.isEmpty()
        pwEmpty.value = pwInput.value?.isEmpty()

        if (!emailEmpty.value!! && !pwEmpty.value!!) {
            viewModelScope.launch {
                tryCreate()
            }
        }
    }

    private suspend fun tryCreate() {
        _showProgressBar.value = true
        val db = Firebase.firestore
        db.collection("UtilCollection").document("AllowCreateAccount").get(Source.SERVER)
            .addOnSuccessListener {
                Log.d("doc received", it.toString())
                if (it["allow"] == true) {
                    viewModelScope.launch {
                        tryActualCreate()
                    }
                } else {
                    _showAccountCreationIsDisabledHint.value = true
                }
            }
        _showProgressBar.value = false
    }

    private suspend fun tryActualCreate() {
        try {
            auth = Firebase.auth
            auth.createUserWithEmailAndPassword(
                emailInput.value.toString(),
                pwInput.value.toString()
            )
                .addOnSuccessListener {
                    FirebaseFirestore.getInstance()
                    _showAccountCreationHint.value = true
                }.await()
        } catch (e: Exception) {
            toastMessage = e.localizedMessage!!
            _createAccError.value = true
        }
    }

    fun resetAccountCreationHintToFalse() {
        _showAccountCreationHint.value = false;
    }

    fun resetAccountCreationIsDisabledHintToFalse() {
        _showAccountCreationIsDisabledHint.value = false;
    }

    fun loginClick() {

        emailEmpty.value = emailInput.value?.isEmpty()
        pwEmpty.value = pwInput.value?.isEmpty()

        if (!emailEmpty.value!! && !pwEmpty.value!!) {
            showPrivacyDialog.value = true
        }
    }

    fun loginConfirmed() {
        viewModelScope.launch {
            trySignIn()
        }
    }

    private suspend fun trySignIn() {
        _showProgressBar.value = true
        auth = Firebase.auth
        val db = Firebase.firestore

        try {
            val authResult = auth
                .signInWithEmailAndPassword(
                    emailInput.value.toString(),
                    pwInput.value.toString()
                )
                .await()

            val uid = authResult.user!!.uid

            val doc = db.collection("userTenants").document(uid).get().await()

            val tenantId = doc.getString("tenantId")
            val role = doc.getString("role")

            Timber.d("SignIn with $tenantId")
            Timber.d("SignIn role=$role")
            if (tenantId != null && role != null) {
                userSessionManager.saveSession(tenantId, role, authResult.user!!.email!!)
                loggedIn()
            } else {
                throw Exception("User has no department. Contact admin!")
            }
        } catch (e: Exception) {
            Timber.e("SignIn: Login fehlgeschlagen mit Fehlermeldung $e")
            toastMessage = e.localizedMessage ?: "Login fehlgeschlagen"
            _loginError.value = true
        } finally {
            _showProgressBar.value = false
        }
    }


    fun loginErrorShown() {
        _loginError.value = false
    }

    fun createErrorShown() {
        _createAccError.value = false
    }
}