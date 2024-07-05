package com.example.findobject.fragment

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.findobject.*
import com.example.findobject.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.Locale
import java.util.concurrent.Executors

class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private val viewModel: MainViewModel by activityViewModels()

    private val backgroundExecutor by lazy { Executors.newSingleThreadExecutor() }
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private var isOcrActive: Boolean = false
    private var isDetectionActive: Boolean = false
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent

    // CameraX variables
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObjectDetector()
        setupSpeechRecognizer()
        fragmentCameraBinding.viewFinder.post { setUpCamera() }
        initBottomSheetControls()
        fragmentCameraBinding.overlay.setRunningMode(RunningMode.LIVE_STREAM)

        fragmentCameraBinding.startOcrButton.setOnClickListener {
            toggleOcr()
        }
    }

    private fun setupObjectDetector() {
        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            threshold = viewModel.currentThreshold,
            currentDelegate = viewModel.currentDelegate,
            currentModel = viewModel.currentModel,
            maxResults = viewModel.currentMaxResults,
            objectDetectorListener = this,
            runningMode = RunningMode.LIVE_STREAM
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentCameraBinding = null
        backgroundExecutor.shutdown()
        cameraExecutor.shutdown()
        speechRecognizer.destroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onResults(resultBundle: ObjectDetectorHelper.ResultBundle) {
        if (!isDetectionActive) return

        activity?.runOnUiThread {
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text = String.format("%d ms", resultBundle.inferenceTime)
            val detectionResult = resultBundle.results[0]
            if (isAdded) {
                fragmentCameraBinding.overlay.setResults(
                    detectionResult,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    resultBundle.inputImageRotation
                )
                fragmentCameraBinding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == ObjectDetectorHelper.GPU_ERROR) {
                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(ObjectDetectorHelper.DELEGATE_CPU, false)
            }
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "SpeechRecognizer: Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "SpeechRecognizer: Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "SpeechRecognizer: End of speech")
            }

            override fun onError(error: Int) {
                Log.d(TAG, "SpeechRecognizer error: $error")
                startListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "SpeechRecognizer results: $matches")
                matches?.let {
                    for (result in it) {
                        Log.d(TAG, "Recognized command: $result")
                        if (result.equals("start button", ignoreCase = true)) {
                            toggleOcr()
                            break
                        }
                        if (result.equals("stop", ignoreCase = true)) {
                            stopDetectionImmediately()
                            break
                        }
                    }
                }
                startListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startListening()
    }

    private fun startListening() {
        Log.d(TAG, "SpeechRecognizer: Start listening")
        speechRecognizer.startListening(speechRecognizerIntent)
    }

    private fun toggleOcr() {
        isOcrActive = !isOcrActive
        isDetectionActive = isOcrActive
        if (!isOcrActive) {
            stopDetectionImmediately()
        } else {
            startDetection()
        }
        Toast.makeText(requireContext(), "OCR is now ${if (isOcrActive) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
    }

    private fun startDetection() {
        isDetectionActive = true
    }

    private fun stopDetectionImmediately() {
        isDetectionActive = false
        activity?.runOnUiThread {
            fragmentCameraBinding.detectedObjectDescription.text = ""
            fragmentCameraBinding.overlay.clear()
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(backgroundExecutor) { imageProxy ->
                        if (!isDetectionActive) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        // Analyzing for object detection
                        try {
                            if (isOcrActive) {
                                objectDetectorHelper.detectLivestreamFrameWithTextDetection(imageProxy)
                            } else {
                                objectDetectorHelper.detectLivestreamFrame(imageProxy)
                            }
                            imageProxy.close()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing image: ${e.message}")
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider!!.unbindAll()
                camera = cameraProvider!!.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun initBottomSheetControls() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text = viewModel.currentMaxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text = String.format("%.2f", viewModel.currentThreshold)

        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(viewModel.currentDelegate, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                try {
                    objectDetectorHelper.currentDelegate = position
                    updateControlsUi()
                } catch (e: UninitializedPropertyAccessException) {
                    Log.e(TAG, "ObjectDetectorHelper has not been initialized yet.")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(viewModel.currentModel, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                try {
                    objectDetectorHelper.currentDelegate = position
                    updateControlsUi()
                    objectDetectorHelper.currentModel = position
                } catch (e: UninitializedPropertyAccessException) {
                    Log.e(TAG, "ObjectDetectorHelper has not been initialized yet.")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text = objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text = String.format("%.2f", objectDetectorHelper.threshold)
        backgroundExecutor.execute {
            objectDetectorHelper.clearObjectDetector()
            objectDetectorHelper.setupObjectDetector()
        }
        fragmentCameraBinding.overlay.clear()
    }

    override fun onTextResults(text: String) {
        activity?.runOnUiThread {
            // Update UI to display the recognized text
            fragmentCameraBinding.detectedObjectDescription.text = text
        }
    }
}
