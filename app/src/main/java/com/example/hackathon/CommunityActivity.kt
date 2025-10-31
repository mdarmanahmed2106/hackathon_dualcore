package com.example.hackathon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class HelpPost(val title: String, val details: String)

class CommunityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community)

        val rv = findViewById<RecyclerView>(R.id.rvCommunity)
        rv.layoutManager = LinearLayoutManager(this)
        val sample = listOf(
            HelpPost("Need O+ blood donor", "City Hospital, tomorrow 10am"),
            HelpPost("Medicine pickup", "Help an elderly neighbor pick up meds"),
            HelpPost("Wheelchair ramp help", "Looking for volunteers this weekend")
        )
        rv.adapter = object : RecyclerView.Adapter<Text2Holder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Text2Holder {
                val v = android.view.LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
                return Text2Holder(v)
            }
            override fun getItemCount(): Int = sample.size
            override fun onBindViewHolder(holder: Text2Holder, position: Int) {
                val item = sample[position]
                holder.title.text = item.title
                holder.subtitle.text = item.details
            }
        }
    }
}


