package de.muenchen.appcenter.nimux.util.recognition

import android.content.Context
import android.graphics.RectF
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import de.muenchen.appcenter.nimux.util.recognition.tflite.SimilarityClassifier
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val faceNet: SimilarityClassifier
) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_face_embeddings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val gson = Gson()

    fun registerUser(userId: String, embedding: FloatArray) {
        val json = gson.toJson(embedding.toList())
        encryptedPrefs.edit() { putString(userId, json) }

        val rec = SimilarityClassifier.Recognition(userId, userId, 0f, RectF())
        rec.extra = arrayOf(embedding)
        faceNet.register(userId, rec)

        Timber.d("User registered: $userId")
    }

    fun unregisterUser(userId: String) {
        encryptedPrefs.edit().remove(userId).apply()
        reloadFromPrefs()
        Timber.d("User deleted: $userId")
    }

    fun reloadFromPrefs() {
        for ((key, value) in encryptedPrefs.all) {
            val list = gson.fromJson(value as String, Array<Float>::class.java)
            val floatArray = list.toFloatArray()
            val rec = SimilarityClassifier.Recognition(key, key, 0f, RectF())
            rec.extra = arrayOf(floatArray)
            faceNet.register(key, rec)
            Timber.d("Reloaded key: $key")
        }
    }

    fun getRegisteredUserIds(): List<String> {
        return encryptedPrefs.all.keys.toList()
    }
}