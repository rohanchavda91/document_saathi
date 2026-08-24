package com.rohan.documentsaathi.feature.scanner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rohan.documentsaathi.R
import com.rohan.documentsaathi.databinding.FragmentScannerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

/**
 * ScannerFragment handles camera preview and photo capture.
 *
 * Flow:
 * 1. Request camera permission (runtime)
 * 2. Start CameraX preview
 * 3. Capture photo to cache directory
 * 4. Convert to Bitmap
 * 5. Pass to ViewModel for OCR processing
 * 6. Navigate to OcrResultFragment with extracted text (via Safe Args)
 */
@AndroidEntryPoint
class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScannerViewModel by viewModels()

    private lateinit var cameraProvider: ProcessCameraProvider
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                binding.permissionDeniedOverlay.visibility = View.VISIBLE
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEdgeToEdge()
        // Check permission and start camera
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Capture button listener
        binding.fabCapture.setOnClickListener {
            capturePhoto()
        }

        // Back button listener
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Flash toggle listener
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        // Retry permission listener
        binding.btnRetryPermission.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Observe ViewModel state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ScannerUiState.ProcessingPhoto -> {
                        binding.fabCapture.isEnabled = false
                        binding.loadingOverlay.visibility = View.VISIBLE
                    }
                    is ScannerUiState.OcrSuccess -> {
                        binding.fabCapture.isEnabled = true
                        binding.loadingOverlay.visibility = View.GONE
                        navigateToDocumentDetail(state.documentId)
                    }
                    is ScannerUiState.OcrError -> {
                        binding.fabCapture.isEnabled = true
                        binding.loadingOverlay.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "OCR Error: ${state.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is ScannerUiState.Idle -> {
                        binding.fabCapture.isEnabled = true
                        binding.loadingOverlay.visibility = View.GONE
                    }
                    is ScannerUiState.CameraReady -> {
                        binding.fabCapture.isEnabled = true
                    }
                }
            }
        }
    }

    private fun toggleFlash() {
        camera?.let {
            val isTorchOn = it.cameraInfo.torchState.value == TorchState.ON
            it.cameraControl.enableTorch(!isTorchOn)
        }
    }

    /**
     * Start CameraX preview.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = binding.cameraPreview.surfaceProvider
                }

            // Image Capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind previous if any
                cameraProvider.unbindAll()

                // Bind preview and image capture to lifecycle
                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                // Observe Torch State to update icon
                camera?.cameraInfo?.torchState?.observe(viewLifecycleOwner) { state ->
                    val iconRes = if (state == TorchState.ON) {
                        R.drawable.flashlight_on_24
                    } else {
                        R.drawable.flashlight_off_24
                    }
                    binding.btnFlash.setImageResource(iconRes)
                }

                viewModel.onCameraReady()
                binding.permissionDeniedOverlay.visibility = View.GONE
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed to start camera: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Capture photo and convert to Bitmap for OCR.
     */
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        // Create output file in cache directory
        val outputFile = File(
            requireContext().cacheDir,
            "IMG_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(outputFile)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Load bitmap from file
                    val bitmap = android.graphics.BitmapFactory.decodeFile(outputFile.absolutePath)

                    // Pass to ViewModel for OCR processing
                    viewModel.processPhoto(bitmap)

                    // Clean up temp file
                    outputFile.delete()
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        requireContext(),
                        "Photo capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    /**
     * Navigate to OcrResultFragment with extracted text via Safe Args.
     * This requires:
     * 1. Safe Args Gradle plugin in build.gradle.kts
     * 2. Navigation action in nav_graph.xml with arguments defined
     * 3. ScannerFragmentDirections class (auto-generated by Safe Args)
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun navigateToDocumentDetail(documentId: Long) {
        try {
            // Updated to navigate directly to DocumentDetailFragment
            val action = ScannerFragmentDirections.actionScannerToDocumentDetail(
                documentId = documentId
            )
            findNavController().navigate(action)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Navigation failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to toolbar
            binding.topToolbar.updatePadding(top = systemBars.top)
            binding.topToolbar.updateLayoutParams {
                height = resources.getDimensionPixelSize(R.dimen.app_bar_height) + systemBars.top
            }

            // Apply bottom padding to bottom action panel
            binding.bottomActionPanel.updatePadding(bottom = systemBars.bottom)
            
            insets
        }
    }
}
