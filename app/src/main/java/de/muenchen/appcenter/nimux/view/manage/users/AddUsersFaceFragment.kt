package de.muenchen.appcenter.nimux.view.manage.users

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
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
    private val binding get() = _binding!! // non-null only between onCreateView and onDestroyView

    private var cameraProvider: ProcessCameraProvider? = null


    // samples which are collected for face recognition of one face
    private val requiredSamples = 40
    private val collectedEmbeddings = mutableListOf<FloatArray>()
    private var isProcessing = false
    private var lastSampleTime = 0L
    private val sampleDelay = 400L// 0.4s between samples 400L

    @Inject lateinit var cameraController: CameraController
    @Inject lateinit var analyzer: FaceProcessingAnalyzer
    @Inject lateinit var faceNet: SimilarityClassifier
    @Inject lateinit var faceRegistry: FaceRegistry

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
            findNavController().popBackStack()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
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
            var warningShown = false
            analyzer.onFacesUpdated = { faces, w, h, rotation ->
                _binding?.faceoverlay?.setFaces(faces, w, h, rotation, true)
                if (faces.size > 1 && !warningShown) {
                    warningShown = true
                    cameraController.stopCamera()
                    collectedEmbeddings.clear()
                    _binding?.sampleCounter?.text =
                        "${getString(R.string.getting_face)} ${collectedEmbeddings.size} / $requiredSamples"
                    requireActivity().runOnUiThread {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.face_detection_warning))
                            .setMessage(getString(R.string.face_detection_warning_text)+"\n"+getString(R.string.face_detection_warning_register))
                            .setPositiveButton("OK") { _, _ ->
                                warningShown = false; cameraController.startCamera(
                                viewLifecycleOwner,
                                binding.previewView,
                                analyzer
                            )
                            }
                            .show()
                    }
                }
            }

        analyzer.onFaceCropped = { faceBitmap ->
            if (isAdded && _binding != null) {
                registerFace(faceBitmap)
            }
            analyzer.resetProcessing()
        }

        cameraController.startCamera(
            viewLifecycleOwner,
            _binding!!.previewView,
            analyzer
        )
    }

    /**
     * Register Face and make multi sample registration.
     */
    private fun registerFace(faceBitmap: Bitmap?) {
        if (!isAdded || faceBitmap == null) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSampleTime < sampleDelay) return
        lastSampleTime = currentTime

        val results = faceNet.recognizeImage(faceBitmap, true)
        if (results.isEmpty()) return

        val rawEmbedding = results[0].extra[0]
        val normalizedEmbedding = normalizeEmbedding(rawEmbedding)
        collectedEmbeddings.add(normalizedEmbedding)

        _binding?.sampleCounter?.text =
            "${getString(R.string.getting_face)} ${collectedEmbeddings.size} / $requiredSamples"

        Timber.d("Sample ${collectedEmbeddings.size} collected, embedding normalized")

        if (collectedEmbeddings.size >= requiredSamples) {
            isProcessing = true
            cameraProvider?.unbindAll()


            val user = AddUsersFaceFragmentArgs.fromBundle(requireArguments()).currentUser
            faceRegistry.registerUser(user.stringSortID, collectedEmbeddings)

            Timber.d("Embedding registered for UserID: ${user.stringSortID}")

            Toast.makeText(requireContext(), "Gesicht erfolgreich gespeichert", Toast.LENGTH_LONG)
                .show()

            findNavController().popBackStack()
        }
    }

    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        val norm = sqrt(embedding.sumOf { (it * it).toDouble() })
        if (norm == 0.0) return embedding
        return embedding.map { (it / norm).toFloat() }.toFloatArray()
    }

    private fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
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