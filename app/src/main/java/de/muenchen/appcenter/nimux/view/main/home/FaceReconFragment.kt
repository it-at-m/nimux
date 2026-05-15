package de.muenchen.appcenter.nimux.view.main.home

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.face.FaceDetector
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentFaceReconBinding
import de.muenchen.appcenter.nimux.datasources.DonateItemDataSource
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.UserSessionManager
import de.muenchen.appcenter.nimux.util.collection_users
import de.muenchen.appcenter.nimux.util.recognition.CameraController
import de.muenchen.appcenter.nimux.util.recognition.FaceProcessingAnalyzer
import de.muenchen.appcenter.nimux.util.recognition.FaceRegistry
import de.muenchen.appcenter.nimux.util.recognition.tflite.SimilarityClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FaceReconFragment : Fragment() {

    enum class ReconState { SEARCHING, FACE_DETECTED, SUCCESS }

    @Inject
    lateinit var donateItemDataSource: DonateItemDataSource

    private var _binding: FragmentFaceReconBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var userRepository: UsersRepository

    private var cameraProvider: ProcessCameraProvider? = null

    @Inject
    lateinit var cameraController: CameraController

    @Inject
    lateinit var analyzer: FaceProcessingAnalyzer

    @Inject
    lateinit var faceNet: SimilarityClassifier

    @Inject
    lateinit var faceRegistry: FaceRegistry

    @Inject
    lateinit var detector: FaceDetector

    private var isNavigating = false
    private var currentState = ReconState.SEARCHING
    private var recognizedUserId: String? = null
    private var glitterAnimator: ValueAnimator? = null

    lateinit var sessionsManager: UserSessionManager

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionsManager = UserSessionManager(context.applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        faceRegistry.reloadFromPrefs()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFaceReconBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.overlay.showFaceBounds = false
        binding.buttonManualUserSelect.setOnClickListener {
            findNavController().navigate(FaceReconFragmentDirections.actionNavHomeToNavHomeManual())
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
        stopGlitter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopGlitter()
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
        isNavigating = false
        currentState = ReconState.SEARCHING
        recognizedUserId = null

        var hasTwoEyes = false

        analyzer.onFacesUpdated = onFacesUpdated@{ faces, w, h, rotation ->
            _binding?.overlay?.setFaces(
                faces,
                w,
                h,
                rotation,
                currentState == ReconState.FACE_DETECTED
            )

            if (currentState == ReconState.FACE_DETECTED && faces.isEmpty()) {
                Timber.i("Gesicht verloren")
                currentState = ReconState.SEARCHING
                recognizedUserId = null
                requireActivity().runOnUiThread {
                    stopGlitter()
                }
                analyzer.resetProcessing()
            }

            if (faces.size > 1 && !warningShown) {
                warningShown = true
                cameraController.stopCamera()
                requireActivity().runOnUiThread {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.face_detection_warning))
                        .setMessage(getString(R.string.face_detection_warning_text))
                        .setPositiveButton("OK") { _, _ ->
                            warningShown = false
                            cameraController.startCamera(
                                viewLifecycleOwner,
                                binding.previewView,
                                analyzer
                            )
                        }
                        .show()
                }
            }

            val face = faces.firstOrNull()

            if (face != null) {
                val hasEyesData = face.leftEyeOpenProbability != null && face.rightEyeOpenProbability != null

                val isLookingStraight = Math.abs(face.headEulerAngleY) < 12.0f && Math.abs(face.headEulerAngleZ) < 12.0f


                val marginX = w * 0.05f
                val marginY = h * 0.05f
                val bounds = face.boundingBox
                val isSafelyInsideFrame = bounds.left > marginX && bounds.right < (w - marginX) &&
                        bounds.top > marginY && bounds.bottom < (h - marginY)

                hasTwoEyes = hasEyesData && isLookingStraight && isSafelyInsideFrame
                Timber.i("Eyes: $hasTwoEyes HasEyes: $hasEyesData Straight: $isLookingStraight Frame: $isSafelyInsideFrame")

                if (!hasTwoEyes) {
                    Timber.i("Ungültiges Gesicht")

                    if (currentState == ReconState.FACE_DETECTED) {
                        currentState = ReconState.SEARCHING
                        recognizedUserId = null
                        requireActivity().runOnUiThread { stopGlitter() }
                    }

                    analyzer.resetProcessing()
                    return@onFacesUpdated
                }
            } else {
                hasTwoEyes = false
            }

            if (currentState == ReconState.FACE_DETECTED && face != null) {

                val leftEye = face.leftEyeOpenProbability ?: 1.0f
                val rightEye = face.rightEyeOpenProbability ?: 1.0f

                val oneEyeClosed = (leftEye < 0.1f && rightEye > 0.8f) || (rightEye < 0.1f && leftEye > 0.8f)

                Timber.i("L: $leftEye, R: $rightEye | Wink: $oneEyeClosed")

                if (oneEyeClosed) {
                    currentState = ReconState.SUCCESS
                    isNavigating = true

                    requireActivity().runOnUiThread {
                        stopGlitter()
                        recognizedUserId?.let { userId ->
                            moveToProductFragment(userId)
                        }
                    }
                }
            }
        }

        analyzer.onFaceCropped = onFaceCropped@{ faceBitmap ->
            if (isNavigating || currentState != ReconState.SEARCHING) {
                return@onFaceCropped
            }

            if (!hasTwoEyes) {
                analyzer.resetProcessing()
                return@onFaceCropped
            }

            val registeredUsers = faceRegistry.getRegisteredUserIds()
            if (registeredUsers.isEmpty()) {
                analyzer.resetProcessing()
                return@onFaceCropped
            }

            val results = faceNet.recognizeImage(faceBitmap, true)

            if (results.isNotEmpty()) {
                val bestMatch = results[0]
                val threshold = 0.7f

                if (bestMatch.distance < threshold) {
                    val userId = bestMatch.title
                    recognizedUserId = userId

                    isNavigating = true
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

                        val user = userRepository.getUser(userId)
                        val isGestureEnabled = user?.faceFeatureNeeded ?: false

                        withContext(Dispatchers.Main) {
                            if (isGestureEnabled) {
                                isNavigating = false
                                currentState = ReconState.FACE_DETECTED
                                showGlitterAndInstruction(userId)
                            } else {
                                currentState = ReconState.SUCCESS
                                moveToProductFragment(userId)
                            }
                        }
                    }
                    return@onFaceCropped
                }
            }

            if (currentState == ReconState.SEARCHING) {
                analyzer.resetProcessing()
            }
        }

        cameraController.startCamera(
            viewLifecycleOwner,
            binding.previewView,
            analyzer
        )
    }

    private fun moveToProductFragment(userId: String) {
        val tenantRef =
            sessionsManager.getTenantRef() ?: throw IllegalStateException("Kein Tenant gesetzt")
        val query = tenantRef.collection(collection_users)

        query.document(userId).get().addOnCompleteListener { task ->
            if (!isAdded) return@addOnCompleteListener
            Timber.d("navigate with user Id = $userId")
            if (findNavController().currentDestination?.id == R.id.nav_home_auto) {
                if (task.isSuccessful && task.result != null) {
                    val userObj = task.result!!.toObject(User::class.java)
                    userObj?.let { user ->
                        val extras =
                            FragmentNavigatorExtras(binding.container to getString(R.string.home_user_detail_transition_name))

                        val action =
                            FaceReconFragmentDirections.actionNavHomeAutoToHomeProductFragment(
                                user,
                                true
                            )
                        findNavController().navigate(action, extras)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(Dispatchers.IO) {
            val donateItem = donateItemDataSource.getDonationItem()
            withContext(Dispatchers.Main) {
                if (donateItem != null) {
                    binding.donateCardview.visibility = View.VISIBLE
                    if (!donateItem.anon) binding.donateCardName.text = donateItem.userName
                    else binding.donateCardName.visibility = View.GONE
                } else {
                    binding.donateCardview.visibility = View.GONE
                }
            }
        }
    }

    private fun showGlitterAndInstruction(userName: String?) {
        binding.instructionText.text = "${getString(R.string.eye_close)}\n($userName)"
        binding.instructionText.visibility = View.VISIBLE

        glitterAnimator?.cancel()
        glitterAnimator = ValueAnimator.ofFloat(0.2f, 1f).apply {
            duration = 400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float
                binding.overlay.setGlitterAlpha(alpha)
            }
            start()
        }
    }

    private fun stopGlitter() {
        binding.instructionText.visibility = View.GONE
        glitterAnimator?.cancel()
        binding.overlay.setGlitterAlpha(1f)
    }
}