package de.muenchen.appcenter.nimux.util.recognition

import android.content.Context
import android.graphics.RectF
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

    //similar embeddings sorted out using filterdEmbedding function
    fun registerUser(userId: String, embeddings: List<FloatArray>) {
        val filtered = filteredEmbeddings(embeddings)

        val json = gson.toJson(filtered)
        encryptedPrefs.edit() { putString(userId, json) }

        val rec = SimilarityClassifier.Recognition(userId, userId, 0f, RectF())
        rec.extra = filtered.toTypedArray()

        faceNet.register(userId, rec)

        Timber.d("User registered: $userId with ${filtered.size} embeddings (filtered)")
    }

    fun filteredEmbeddings(embeddings: List<FloatArray>): MutableList<FloatArray>{
        val filtered = mutableListOf<FloatArray>()
        val threshold = 0.5f

        for (emb in embeddings) {

            var isDifferent = true

            for (existing in filtered) {
                if (findDistance(existing, emb) < threshold) {
                    isDifferent = false
                    break
                }
            }

            if (isDifferent) {
                filtered.add(emb)
            }
        }
        return filtered
    }

    fun findDistance(emb: FloatArray, knownEmb: FloatArray): Float {
        var distance = 0f

        for (i in emb.indices) {
            val diff = emb[i] - knownEmb[i]
            distance += diff * diff
        }

        return kotlin.math.sqrt(distance)
    }


    fun unregisterUser(userId: String) {
        encryptedPrefs.edit().remove(userId).apply()
        reloadFromPrefs()
        Timber.d("User deleted: $userId")
    }

    fun reloadFromPrefs() {
        for ((key, value) in encryptedPrefs.all) {
            try {
                val embeddings: Array<FloatArray> = gson.fromJson(value as String, Array<FloatArray>::class.java)

                val rec = SimilarityClassifier.Recognition(key, key, 0f, RectF())

                rec.setExtra(embeddings)

                faceNet.register(key, rec)

                Timber.d("Reloaded key: $key with ${embeddings.size} embeddings")

            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Laden von Key: $key. Übersprungen.")
            }
        }
    }

    fun getRegisteredUserIds(): List<String> {
        return encryptedPrefs.all.keys.toList()
    }
}