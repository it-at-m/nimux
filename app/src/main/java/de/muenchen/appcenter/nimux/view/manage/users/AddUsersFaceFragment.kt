package de.muenchen.appcenter.nimux.view.manage.users

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.face.FaceDetector
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.databinding.FragmentAddUsersFaceBinding
import de.muenchen.appcenter.nimux.util.recognition.CameraController
import de.muenchen.appcenter.nimux.util.recognition.FaceProcessingAnalyzer
import de.muenchen.appcenter.nimux.util.recognition.FaceRegistry
import de.muenchen.appcenter.nimux.util.recognition.tflite.SimilarityClassifier
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Fragment is used for adding user face data to the shared preferences. The data is necessary
 * for face recognition at startup (if enabled in the app-wide settings).
 */
@AndroidEntryPoint
class AddUsersFaceFragment : Fragment() {

    private var _binding: FragmentAddUsersFaceBinding? = null
    private val binding get() = _binding!!

    private var cameraProvider: ProcessCameraProvider? = null

    // samples which are collected for face recognition of one face
    private val requiredSamples = 20
    private val collectedEmbeddings = mutableListOf<FloatArray>()
    private var isProcessing = false

    @Inject
    lateinit var cameraController: CameraController

    @Inject
    lateinit var analyzer: FaceProcessingAnalyzer

    @Inject
    lateinit var faceNet: SimilarityClassifier

    @Inject
    lateinit var faceRegistry: FaceRegistry

    private var lastSampleTime = 0L

    // time between each sample get collected
    private val sampleDelay = 800L // 0.8 seconds

    @Inject
    lateinit var detector: FaceDetector

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddUsersFaceBinding.inflate(inflater, container, false)
        binding.cancelButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraController.stopCamera()
        _binding = null
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    private fun startCamera() {

        analyzer.onFacesUpdated = { faces, w, h ->
            _binding?.faceoverlay?.setFaces(faces, w, h)
        }

        analyzer.onFaceCropped = { faceBitmap ->
            registerFace(faceBitmap)
            analyzer.resetProcessing()
        }

        cameraController.startCamera(
            viewLifecycleOwner,
            binding.previewView,
            analyzer
        )
    }

    /**
     * Register Face and make multi sample registration.
     */
    private fun registerFace(faceBitmap: Bitmap?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSampleTime < sampleDelay) {
            return
        }
        lastSampleTime = currentTime

        val results = faceNet.recognizeImage(faceBitmap, true)
        if (results.isEmpty()) return

        // 🔹 Normalisiertes Embedding
        val rawEmbedding = results[0].extra[0]
        val normalizedEmbedding = normalizeEmbedding(rawEmbedding)

        collectedEmbeddings.add(normalizedEmbedding)

        binding.sampleCounter.text =
            "Gesicht wird erfasst ${collectedEmbeddings.size} / $requiredSamples"

        Timber.d("Sample ${collectedEmbeddings.size} collected, embedding normed")

        if (collectedEmbeddings.size >= requiredSamples) {

            isProcessing = true
            cameraProvider?.unbindAll()

            val averaged = averageEmbeddings(collectedEmbeddings)
            val normalizedAveraged = normalizeEmbedding(averaged)

            val user =
                AddUsersFaceFragmentArgs.fromBundle(requireArguments())
                    .currentUser

            faceRegistry.registerUser(user.stringSortID, normalizedAveraged)

            Timber.d("Embedding registered for UserID: ${user.stringSortID}")

            Toast.makeText(
                requireContext(),
                "Gesicht erfolgreich gespeichert",
                Toast.LENGTH_LONG
            ).show()

            findNavController().popBackStack()
        }
    }

    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        val norm = sqrt(embedding.sumOf { (it * it).toDouble() })
        if (norm == 0.0) return embedding
        return embedding.map { (it / norm).toFloat() }.toFloatArray()
    }

    private fun averageEmbeddings(
        embeddings: List<FloatArray>
    ): FloatArray {

        val length = embeddings[0].size
        val avg = FloatArray(length)

        for (i in 0 until length) {
            var sum = 0f
            for (embedding in embeddings) {
                sum += embedding[i]
            }
            avg[i] = sum / embeddings.size
        }

        return avg
    }
}