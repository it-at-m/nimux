package de.muenchen.appcenter.nimux.hilt

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.internal.Provider
import de.muenchen.appcenter.nimux.util.UserSessionManager

@Module
@InstallIn(SingletonComponent::class)
object TenantModule {

    @Provides
    fun provideTenantRefProvider(
        sessionManager: UserSessionManager,
        firestore: FirebaseFirestore
    ): Provider<DocumentReference?> {
        return Provider {
            val tenantId = sessionManager.getTenantId()
            tenantId?.let { firestore.collection("tenants").document(it) }
        }
    }
}

