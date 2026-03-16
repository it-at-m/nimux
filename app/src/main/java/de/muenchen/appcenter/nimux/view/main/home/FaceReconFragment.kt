package de.muenchen.appcenter.nimux.view.main.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import com.google.mlkit.vision.face.FaceDetector
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentFaceReconBinding
import de.muenchen.appcenter.nimux.datasources.DonateItemDataSource
import de.muenchen.appcenter.nimux.model.User
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

    @Inject
    lateinit var donateItemDataSource: DonateItemDataSource

    private var _binding: FragmentFaceReconBinding? = null
    private val binding get() = _binding!!

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

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
        }

    lateinit var sessionsManager: UserSessionManager

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
            _binding?.overlay?.setFaces(faces, w, h)
        }

        analyzer.onFaceCropped = onFaceCropped@{ faceBitmap ->

            val registeredUsers = faceRegistry.getRegisteredUserIds()
            if (registeredUsers.isEmpty()) {
                analyzer.resetProcessing()
                return@onFaceCropped
            }

            val results = faceNet.recognizeImage(faceBitmap, true)

            if (results.isNotEmpty()) {
                val bestMatch = results[0]
                val threshold = 0.6f

                if (bestMatch.distance < threshold) {
                    moveToProductFragment(bestMatch.title)
                }
            }

            analyzer.resetProcessing()
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
                        findNavController().navigate(
                            FaceReconFragmentDirections.actionNavHomeAutoToHomeProductFragment(
                                user,
                                true
                            ), extras
                        )
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
}