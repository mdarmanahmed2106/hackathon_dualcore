package com.example.hackathon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import khttp.post

class AiHelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_help)

        val etSymptoms = findViewById<TextInputEditText>(R.id.etSymptoms)
        val btnGetAdvice = findViewById<MaterialButton>(R.id.btnGetAdvice)
        val tvResponse = findViewById<TextView>(R.id.tvResponse)

        btnGetAdvice.setOnClickListener {
            val prompt = etSymptoms.text?.toString().orEmpty().trim()
            if (prompt.isEmpty()) {
                Snackbar.make(btnGetAdvice, "Please describe your symptoms", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            tvResponse.text = "Thinking..."
            CoroutineScope(Dispatchers.IO).launch {
                val advice = getHealthAdvice(prompt)
                runOnUiThread { tvResponse.text = advice }
            }
        }
    }

    // Calls Gemini API using khttp. Replace YOUR_GEMINI_API_KEY with your real key.
    private fun getHealthAdvice(userPrompt: String): String {
        return try {
            val apiKey = System.getenv("GEMINI_API_KEY") ?: "AIzaSyCAsVbQaj8dy6pI9Mx1WPq-Eiy_8998oVY"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

            val payload = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply { put("text", "You are a community health assistant. Provide concise, friendly guidance with next steps. User input: $userPrompt") })
                        })
                    })
                })
            }

            val response = post(
                url = url,
                headers = mapOf("Content-Type" to "application/json"),
                data = payload.toString()
            )

            if (response.statusCode in 200..299) {
                val json = response.jsonObject
                val candidates = json.optJSONArray("candidates")
                val content = candidates?.optJSONObject(0)?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text").orEmpty()
                if (text.isNotBlank()) text else "No response. Try again."
            } else {
                "API error: ${response.statusCode}. Check your key and network."
            }
        } catch (e: Exception) {
            "Failed to fetch advice: ${e.message}"
        }
    }
}


