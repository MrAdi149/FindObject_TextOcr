package com.example.findobject.fragment

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

var textDone="Recognising..."

class TextAnalysis  : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        Log.d("TEXT", "image analysed")

        imageProxy.image?.let {
            val inputImage = InputImage.fromMediaImage(
                it,
                imageProxy.imageInfo.rotationDegrees
            )

            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    text.textBlocks.forEach { block ->
                        block.recognizedLanguage
                        Log.d(
                            "TEXT", """
                            LINES = ${block.lines.joinToString("\n") { it.text }}
                            LANGUAGE=${block.recognizedLanguage}                            
                        """.trimIndent()
                        )
                        textDone="recognising..."
                        textDone = block.text.toString()
                    }
                }
                .addOnFailureListener { ex ->
                    Log.e("TEXT", "Detection failed", ex)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }

        } ?: imageProxy.close() // close if image not found either

    }
}