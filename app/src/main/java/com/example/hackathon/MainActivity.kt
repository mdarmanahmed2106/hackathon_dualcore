package com.example.hackathon

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.btnAskAi).setOnClickListener {
            startActivity(Intent(this, AiHelpActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnFindHospital).setOnClickListener {
            startActivity(Intent(this, NearbyHospitalsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnCommunity).setOnClickListener {
            startActivity(Intent(this, CommunityActivity::class.java))
        }
    }
}