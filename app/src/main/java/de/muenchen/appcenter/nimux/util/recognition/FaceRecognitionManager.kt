package de.muenchen.appcenter.nimux.util.recognition

import android.content.Context
import android.graphics.RectF
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.muenchen.appcenter.nimux.util.recognition.tflite.SimilarityClassifier
import de.muenchen.appcenter.nimux.util.recognition.tflite.TFLiteObjectDetectionAPIModel
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FaceRecognitionModule {

    @Provides
    @Singleton
    fun provideFaceClassifier(@ApplicationContext context: Context): SimilarityClassifier {
        val classifier = TFLiteObjectDetectionAPIModel.create(
            context.assets,
            "mobile_face_net.tflite",
            "file:///android_asset/labelmap.txt",
            112,
            false,
            context,
            false
        )

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "secure_face_embeddings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val gson = Gson()
        for ((key, value) in encryptedPrefs.all) {
            val list = gson.fromJson(value as String, Array<Float>::class.java)
            val floatArray = list.toFloatArray()
            val rec = SimilarityClassifier.Recognition(key, key, 0f, RectF())
            rec.setExtra(arrayOf(floatArray))
            classifier.register(key, rec)
            Timber.d("Key: $key")
        }

        return classifier
    }

    /**
     * Reload all embeddings for the classifier. Necessary after deletion of user face data.
     */
    fun SimilarityClassifier.reloadFromEncryptedPrefs(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "secure_face_embeddings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val gson = Gson()

        for ((key, value) in encryptedPrefs.all) {
            val list = gson.fromJson(value as String, Array<Float>::class.java)
            val floatArray = list.toFloatArray()
            val rec = SimilarityClassifier.Recognition(key, key, 0f, RectF())
            rec.setExtra(arrayOf(floatArray))
            this.register(key, rec)
            Timber.d("Reloaded key: $key")
        }
    }
}