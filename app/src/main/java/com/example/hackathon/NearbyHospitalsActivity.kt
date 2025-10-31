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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
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

    private fun fetchNearbyHospitals(lat: Double, lng: Double): List<PlaceItem> {
        return try {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()

            // Search radius in meters (approximately 3km)
            val radius = 3000
            // Overpass QL query to find hospitals and clinics near the location
            val query = """
                [out:json];
                (
                  node["amenity"="hospital"](around:$radius,$lat,$lng);
                  way["amenity"="hospital"](around:$radius,$lat,$lng);
                  node["amenity"="clinic"](around:$radius,$lat,$lng);
                  way["amenity"="clinic"](around:$radius,$lat,$lng);
                );
                out body;
                >;
                out skel qt;
            """.trimIndent()

            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .post(query.toRequestBody("text/plain".toMediaTypeOrNull()!!))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return emptyList()

            val json = JSONObject(responseBody)
            val elements = json.optJSONArray("elements") ?: return emptyList()
            val out = mutableListOf<PlaceItem>()

            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                if (element.has("tags")) {
                    val tags = element.getJSONObject("tags")
                    val name = tags.optString("name", "Unnamed Hospital/Clinic")
                    val address = tags.optString("addr:full", 
                        tags.optString("addr:street", "Address not available"))
                    
                    // Get coordinates (handles both nodes and ways)
                    val (plat, plng) = if (element.has("lat") && element.has("lon")) {
                        Pair(element.getDouble("lat"), element.getDouble("lon"))
                    } else if (element.has("center")) {
                        val center = element.getJSONObject("center")
                        Pair(center.getDouble("lat"), center.getDouble("lon"))
                    } else {
                        Pair(lat, lng) // Fallback to user's location if no coords found
                    }
                    
                    out.add(PlaceItem(name, address, plat, plng))
                }
            }
            
            // If no hospitals found, add a helpful message
            if (out.isEmpty()) {
                out.add(PlaceItem("No hospitals found", "Try moving to a different location", lat, lng))
            }
            out
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(PlaceItem("Error loading hospitals", "Please check your internet connection", lat, lng))
        }
    }

    companion object { 
        private const val REQ_LOCATION = 1001 
    }
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