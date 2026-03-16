package de.muenchen.appcenter.nimux.repositories

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import de.muenchen.appcenter.nimux.datasources.OtherDataSource
import de.muenchen.appcenter.nimux.util.LogInLogOutLog
import java.util.Date
import javax.inject.Inject

class OtherRepository @Inject constructor() {

    @Inject lateinit var otherDataSource: OtherDataSource

    fun addLoginLogoutEntry(loginLogoutEntry: LogInLogOutLog): Task<DocumentReference?> {
        return otherDataSource.addLoginLogoutEntry(loginLogoutEntry)
    }

    fun getLoginLogoutLogsQueryWithTimerangeAndOrderByTimestamp(timeRange: Date, order: Query.Direction): Query {
        return otherDataSource.getLoginLogoutLogsQueryWithTimerangeAndOrderByTimestamp(timeRange, order)
    }

    fun getManagePWDocContent(): DocumentReference {
        return otherDataSource.getManagePWDocument()
    }



}
