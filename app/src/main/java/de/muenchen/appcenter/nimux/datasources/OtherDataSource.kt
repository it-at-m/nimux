package de.muenchen.appcenter.nimux.datasources

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import dagger.internal.Provider
import de.muenchen.appcenter.nimux.util.LogInLogOutLog
import de.muenchen.appcenter.nimux.util.collection_loginlogoutlogs
import de.muenchen.appcenter.nimux.util.collection_passwordManage
import java.util.Date
import javax.inject.Inject

class OtherDataSource @Inject constructor(
    private val tenantRefProvider: Provider<DocumentReference>
) {

    private val collectionLoginLogoutsRef: CollectionReference?
        get() = tenantRefProvider.get()?.collection(collection_loginlogoutlogs)

    private val passwordCollectionRef
        get() = tenantRefProvider.get()
            ?.collection(collection_passwordManage)
            ?: throw IllegalStateException("Tenant missing – user is not logged in")

    private fun requireCollectionLoginLogoutsRef(): CollectionReference {
        return collectionLoginLogoutsRef
            ?: throw IllegalStateException("Tenant missing – user is not logged in")
    }

    fun addLoginLogoutEntry(loginLogoutEntry: LogInLogOutLog): Task<DocumentReference?> {
        return requireCollectionLoginLogoutsRef().add(loginLogoutEntry)
    }

    fun getLoginLogoutLogsQueryWithTimerangeAndOrderByTimestamp(
        timeRange: Date,
        order: Query.Direction
    ): Query {
        return requireCollectionLoginLogoutsRef().whereGreaterThan("timestamp", timeRange)
            .orderBy("timestamp", order)
    }

    fun getManagePWDocument(): DocumentReference {
        return passwordCollectionRef.document("ManagePWDoc")
    }


}