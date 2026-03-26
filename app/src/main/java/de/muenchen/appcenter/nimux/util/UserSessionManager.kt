package de.muenchen.appcenter.nimux.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore

class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences
    private val firestore = Firebase.firestore

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(tenantId: String, role: String, email: String) {
        prefs.edit() {
            putString("tenantId", tenantId)
                .putString("role", role)
                .putString("user", email)
        }
    }

    suspend fun syncSessionInfoWithFirebase(uid: String): String {
        val doc = com.google.firebase.ktx.Firebase.firestore.collection("userTenants").document(uid).get().await()
        return doc.getString("tenantId").toString()
    }

    fun getTenantRef(): DocumentReference? {
        val tenantId = getTenantId() ?: return null
        return firestore.collection("tenants").document(tenantId)
    }

    fun getTenantId(): String? = prefs.getString("tenantId", null)

    fun getUserEMail(): String? = prefs.getString("user", null)

    fun getRole(): String? = prefs.getString("role", null)

    fun hasAdminRole(): Boolean {
        val role = prefs.getString("role", null)
        return role != null && role == "admin"
    }

    fun hasAccessRole(): Boolean {
        val role = prefs.getString("role", null)
        return role != null && role == "access"
    }

    fun clearSession() {
        prefs.edit() { clear() }
    }
}
