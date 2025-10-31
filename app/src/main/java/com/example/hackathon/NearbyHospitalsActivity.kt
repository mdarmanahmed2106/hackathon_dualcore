package com.example.hackathon

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import khttp.get
import org.json.JSONObject

data class PlaceItem(val name: String, val vicinity: String, val lat: Double, val lng: Double)

class NearbyHospitalsActivity : AppCompatActivity() {
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private val places = mutableListOf<PlaceItem>()
    private lateinit var adapter: SimplePlaceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospitals)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        progressBar = findViewById(R.id.progress)
        recyclerView = findViewById(R.id.rvHospitals)
        adapter = SimplePlaceAdapter(places) { item -> openInMaps(item) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        ensureLocationAndLoad()
    }

    private fun ensureLocationAndLoad() {
        if (!PermissionUtils.hasFineLocation(this)) {
            PermissionUtils.requestFineLocation(this, REQ_LOCATION)
            return
        }
        loadNearbyHospitals()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION) ensureLocationAndLoad()
    }

    private fun openInMaps(item: PlaceItem) {
        val uri = Uri.parse("geo:${item.lat},${item.lng}?q=${Uri.encode(item.name)}")
        startActivity(Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") })
    }

    private fun loadNearbyHospitals() {
        progressBar.visibility = View.VISIBLE
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                Snackbar.make(recyclerView, "Location unavailable. Try again.", Snackbar.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                return@addOnSuccessListener
            }
            val lat = loc.latitude
            val lng = loc.longitude
            Thread {
                val list = fetchNearbyHospitals(lat, lng)
                runOnUiThread {
                    places.clear()
                    places.addAll(list)
                    adapter.notifyDataSetChanged()
                    progressBar.visibility = View.GONE
                }
            }.start()
        }.addOnFailureListener {
            Snackbar.make(recyclerView, "Failed to get location", Snackbar.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
        }
    }

    // Uses Places Web Service (Nearby Search) via simple HTTP request with khttp
    private fun fetchNearbyHospitals(lat: Double, lng: Double): List<PlaceItem> {
        return try {
            val apiKey = System.getenv("PLACES_API_KEY") ?: "YOUR_GOOGLE_PLACES_API_KEY"
            val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
            val params = mapOf(
                "location" to "$lat,$lng",
                "radius" to "3000",
                "type" to "hospital",
                "key" to apiKey
            )
            val response = get(url = url, params = params)
            if (response.statusCode in 200..299) {
                val json = response.jsonObject
                val results = json.optJSONArray("results")
                val out = mutableListOf<PlaceItem>()
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val o: JSONObject = results.getJSONObject(i)
                        val geometry = o.optJSONObject("geometry")?.optJSONObject("location")
                        val name = o.optString("name")
                        val vicinity = o.optString("vicinity")
                        val plat = geometry?.optDouble("lat") ?: 0.0
                        val plng = geometry?.optDouble("lng") ?: 0.0
                        out.add(PlaceItem(name, vicinity, plat, plng))
                    }
                }
                out
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object { private const val REQ_LOCATION = 1001 }
}

class SimplePlaceAdapter(
    private val items: List<PlaceItem>,
    private val onClick: (PlaceItem) -> Unit
) : RecyclerView.Adapter<Text2Holder>() {
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Text2Holder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return Text2Holder(view)
    }
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: Text2Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.subtitle.text = item.vicinity
        holder.itemView.setOnClickListener { onClick(item) }
    }
}

class Text2Holder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    val title: android.widget.TextView = itemView.findViewById(android.R.id.text1)
    val subtitle: android.widget.TextView = itemView.findViewById(android.R.id.text2)
}


