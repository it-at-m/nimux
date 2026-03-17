package de.muenchen.appcenter.nimux.datasources

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import dagger.internal.Provider
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.util.collection_suggest_users
import de.muenchen.appcenter.nimux.util.stringToStringSortID
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject

class UserSuggestionDataSource @Inject constructor(
    private val tenantRefProvider: Provider<DocumentReference>
) {
    @Inject
    lateinit var userDataSource: UserDataSource

    private val userSuggestionsRef
        get() = tenantRefProvider.get()
            ?.collection(collection_suggest_users)
            ?: throw IllegalStateException("Tenant missing – user is not logged in")

    fun addUserSuggestion(user: User) {
        userSuggestionsRef.document(user.stringSortID).set(user)
    }

    suspend fun getUserSuggestionCount(): Int {
        val res = CompletableDeferred<Int>()
        var count = 0
        userSuggestionsRef.get().addOnSuccessListener {
            it.forEach { _ ->
                count++
            }
            res.complete(count)
        }.addOnFailureListener { res.complete(count) }
        return res.await()
    }

    suspend fun userSuggestionExistsCheck(userName: String): Boolean {
        val res = CompletableDeferred<Boolean>()
        var userExists = false
        userSuggestionsRef.get().addOnSuccessListener { result ->
            for (document in result) {
                if (document.id == stringToStringSortID(userName))
                    userExists = true
            }
            res.complete(userExists)
        }.addOnFailureListener {
            res.complete(false)
        }
        return res.await()
    }

    fun getUserSuggestionsQuery(): Query =
        userSuggestionsRef.orderBy("stringSortID", Query.Direction.ASCENDING)

    fun deleteUserSuggestion(id: String) {
        userSuggestionsRef.document(id).delete()
    }

    suspend fun userSuggestionApproved(user: User): Boolean {
        val res = CompletableDeferred<Boolean>()
        if (userDataSource.userExistsCheck(user.name)) res.complete(false)
        else {
            userDataSource.addUser(user)
            deleteUserSuggestion(user.stringSortID)
            res.complete(true)
        }
        return res.await()
    }
}