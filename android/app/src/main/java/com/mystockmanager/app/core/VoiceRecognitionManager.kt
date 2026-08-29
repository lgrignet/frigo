package com.mystockmanager.app.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening(language: String, onResult: (String) -> Unit, onError: (String) -> Unit, onStatusChange: (Boolean) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Reconnaissance vocale non disponible sur cet appareil.")
            return
        }

        stopListening() // Ensure any previous session is closed

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onStatusChange(true)
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    onStatusChange(false)
                }
                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
                        SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission manquante"
                        SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout réseau"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Non compris"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconnaissance occupée"
                        SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Aucune parole détectée"
                        else -> "Erreur inconnue ($error)"
                    }
                    onError(message)
                    onStatusChange(false)
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError(e.message ?: "Impossible de lancer l'écoute")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
