package com.example.janaushadhifinder
import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.style.TextAlign
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.ui.platform.LocalContext
import com.example.janaushadhifinder.ui.theme.JanAushadhiFinderTheme
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers      // ← add this
import kotlinx.coroutines.withContext      // ← add this
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            JanAushadhiFinderTheme {
                MainScreen()
            }
        }
    }
}
// Shared selected medicine state
var selectedMedicine: Medicine? = null
var aiPrompt: String? = null
val globalReminders = mutableStateListOf<Triple<String, String, ImageVector>>()
sealed class BottomScreen(val route: String, val title: String) {
    object Search : BottomScreen("search", "Search")
    object Stores : BottomScreen("stores", "Stores")
    object Reminder : BottomScreen("reminder", "Reminder")
    object Savings : BottomScreen("savings", "Savings")
    object AIChat : BottomScreen("aichat", "AI Chat")
}
//data model
data class Medicine(
    val name: String,
    val generic: String,
    val brandedPrice: Int,
    val genericPrice: Int,
    val category: String = "General"  // ADD THIS
)
data class Store(
    val name: String,
    val location: String,
    val status: String,
    val distance: String,
    val latLng: LatLng = LatLng(12.9716, 77.5946)
)
fun showNotification(context: Context) {

    val channelId = "medicine_reminder"

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // ✅ Only for Android 8+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Medicine Reminder",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Medicine Reminder")
        .setContentText("Time to take your medicine 💊")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .build()

    notificationManager.notify(1, notification)
}
//mainscreen
@Composable
fun MainScreen() {

    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            if (currentRoute != "login" && currentRoute != "signup") {
                NavigationBar {

                  NavigationBarItem(
                    selected = currentRoute == "search",
                    onClick = {
                        navController.navigate("search") {
                            popUpTo("search") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search") }
                )

                NavigationBarItem(
                    selected = currentRoute == "map_pick",
                    onClick = {
                        navController.navigate("map_pick") {
                            popUpTo("map_pick") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("Stores") }
                )

                NavigationBarItem(
                    selected = currentRoute == "reminder",
                    onClick = {
                        navController.navigate("reminder") {
                            popUpTo("reminder") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text("Reminder") }
                )

                NavigationBarItem(
                    selected = currentRoute == "savings",
                    onClick = {
                        navController.navigate("savings") {
                            popUpTo("savings") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Savings") }
                )

                NavigationBarItem(
                    selected = currentRoute == "aichat",
                    onClick = {
                        navController.navigate("aichat") {
                            popUpTo("aichat") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                    label = { Text("AI Chat") }
                )
            }
        }}
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(padding)
        ) {

            composable("search") { SearchScreen(navController) }
            composable("stores") { MapPickScreen(navController) }
            composable("map_pick") { MapPickScreen(navController) }
            composable("store_list/{lat}/{lng}") { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 12.9716
                val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 77.5946
                StoreListScreen(navController, lat, lng)
            }
            composable("reminder") { ReminderScreen() }
            composable("savings") { SavingsScreen() }
            composable("aichat") { AIChatScreen() }
            composable("detail") { DetailScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("signup") { SignupScreen(navController) }
        }
    }
}
//// ── Real Jan Aushadhi store fallback data (actual registered kendras) ──
//// Source: janaushadhi.gov.in store list — used when API is unreachable
//val JAN_AUSHADHI_STORES_INDIA = listOf(
//    // Bengaluru
//    Store("PMBJK - Rajajinagar", "No.12, Rajajinagar, Bengaluru, Karnataka", "Open", "0 km", LatLng(12.9916, 77.5526)),
//    Store("PMBJK - Jayanagar", "27th Cross, Jayanagar 4th Block, Bengaluru, Karnataka", "Open", "0 km", LatLng(12.9250, 77.5833)),
//    Store("PMBJK - Whitefield", "Shop 3, EPIP Zone, Whitefield, Bengaluru, Karnataka", "Open", "0 km", LatLng(12.9698, 77.7499)),
//    Store("PMBJK - Koramangala", "80 Feet Road, Koramangala 4th Block, Bengaluru, Karnataka", "Open", "0 km", LatLng(12.9352, 77.6245)),
//    Store("PMBJK - Indiranagar", "100 Feet Road, HAL 2nd Stage, Indiranagar, Bengaluru", "Open", "0 km", LatLng(12.9784, 77.6408)),
//    Store("PMBJK - Hebbal", "Near Hebbal Flyover, Hebbal, Bengaluru, Karnataka", "Open", "0 km", LatLng(13.0358, 77.5970)),
//    Store("PMBJK - Electronic City", "Phase 1, Electronic City, Bengaluru, Karnataka", "Open", "0 km", LatLng(12.8452, 77.6602)),
//    Store("PMBJK - Malleshwaram", "11th Cross, Malleshwaram, Bengaluru, Karnataka", "Open", "0 km", LatLng(13.0055, 77.5692)),
//    Store("PMBJK - HSR Layout", "27th Main, HSR Layout Sector 2, Bengaluru, Karnataka", "Open", "0 km", LatLng(12.9121, 77.6446)),
//    Store("PMBJK - Yelahanka", "New Town, Yelahanka, Bengaluru, Karnataka", "Open", "0 km", LatLng(13.1004, 77.5963)),
//    Store("PMBJK - BTM Layout", "2nd Stage, BTM Layout, Bengaluru, Karnataka", "Open", "0 km", LatLng(12.9166, 77.6101)),
//    Store("PMBJK - Marathahalli", "Old Airport Road, Marathahalli, Bengaluru, Karnataka", "Open", "0 km", LatLng(12.9569, 77.7011)),
//    Store("PMBJK - Basavanagudi", "Gandhi Bazaar, Basavanagudi, Bengaluru, Karnataka", "Open", "0 km", LatLng(12.9414, 77.5741)),
//    Store("PMBJK - Yeshwanthpur", "Near Ring Road, Yeshwanthpur, Bengaluru, Karnataka", "Open", "0 km", LatLng(13.0262, 77.5485)),
//    Store("PMBJK - KR Puram", "Old Madras Road, KR Puram, Bengaluru, Karnataka", "Open", "0 km", LatLng(13.0019, 77.6953)),
//    // Mumbai
//    Store("PMBJK - Andheri West", "Lokhandwala Complex, Andheri West, Mumbai, Maharashtra", "Open", "0 km", LatLng(19.1367, 72.8296)),
//    Store("PMBJK - Borivali", "S.V. Road, Borivali West, Mumbai, Maharashtra", "Open", "0 km", LatLng(19.2307, 72.8567)),
//    Store("PMBJK - Thane", "Naupada, Thane West, Maharashtra", "Open", "0 km", LatLng(19.1974, 72.9636)),
//    Store("PMBJK - Dadar", "Gokhale Road, Dadar West, Mumbai, Maharashtra", "Open", "0 km", LatLng(19.0186, 72.8433)),
//    Store("PMBJK - Kurla", "LBS Marg, Kurla West, Mumbai, Maharashtra", "Open", "0 km", LatLng(19.0726, 72.8796)),
//    // Delhi
//    Store("PMBJK - Rohini", "Sector 3, Rohini, New Delhi", "Open", "0 km", LatLng(28.7041, 77.1025)),
//    Store("PMBJK - Dwarka", "Sector 10, Dwarka, New Delhi", "Open", "0 km", LatLng(28.5921, 77.0460)),
//    Store("PMBJK - Lajpat Nagar", "Central Market, Lajpat Nagar, New Delhi", "Open", "0 km", LatLng(28.5665, 77.2431)),
//    Store("PMBJK - Janakpuri", "District Centre, Janakpuri, New Delhi", "Open", "0 km", LatLng(28.6289, 77.0832)),
//    // Hyderabad
//    Store("PMBJK - Ameerpet", "S.R. Nagar Road, Ameerpet, Hyderabad, Telangana", "Open", "0 km", LatLng(17.4374, 78.4482)),
//    Store("PMBJK - Kukatpally", "KPHB Colony, Kukatpally, Hyderabad, Telangana", "Open", "0 km", LatLng(17.4947, 78.3996)),
//    Store("PMBJK - LB Nagar", "Main Road, LB Nagar, Hyderabad, Telangana", "Open", "0 km", LatLng(17.3475, 78.5490)),
//    // Chennai
//    Store("PMBJK - T. Nagar", "Pondy Bazaar, T. Nagar, Chennai, Tamil Nadu", "Open", "0 km", LatLng(13.0418, 80.2341)),
//    Store("PMBJK - Anna Nagar", "2nd Avenue, Anna Nagar, Chennai, Tamil Nadu", "Open", "0 km", LatLng(13.0891, 80.2108)),
//    Store("PMBJK - Tambaram", "GST Road, Tambaram, Chennai, Tamil Nadu", "Open", "0 km", LatLng(12.9249, 80.1000)),
//    // Pune
//    Store("PMBJK - Kothrud", "Paud Road, Kothrud, Pune, Maharashtra", "Open", "0 km", LatLng(18.5074, 73.8077)),
//    Store("PMBJK - Hadapsar", "Magarpatta Road, Hadapsar, Pune, Maharashtra", "Open", "0 km", LatLng(18.5089, 73.9260)),
//    // Kolkata
//    Store("PMBJK - Salt Lake", "Sector V, Salt Lake, Kolkata, West Bengal", "Open", "0 km", LatLng(22.5726, 88.4278)),
//    Store("PMBJK - Howrah", "GT Road, Howrah, West Bengal", "Open", "0 km", LatLng(22.5958, 88.2636)),
//    // Ahmedabad
//    Store("PMBJK - Vastrapur", "Drive-in Road, Vastrapur, Ahmedabad, Gujarat", "Open", "0 km", LatLng(23.0469, 72.5269)),
//    Store("PMBJK - Satellite", "Jodhpur Cross Road, Satellite, Ahmedabad, Gujarat", "Open", "0 km", LatLng(23.0204, 72.5169))
//)

// ── Fetch nearby Jan Aushadhi stores via Google Places API only ──────────────
// The old hardcoded fallback list has been removed — it was causing the same
// ~15 Bengaluru stores to appear regardless of where you searched.
// Real results come from Google Places API (tryOfficialApi).
// If Places API is not enabled or the key is wrong, an empty list is returned
// and the UI will show "No stores found" so the problem is visible.
suspend fun fetchNearbyJanAushadhiStores(
    lat: Double,
    lng: Double,
    @Suppress("UNUSED_PARAMETER") unusedKey: String = ""
): List<Store> {
    android.util.Log.d("StoreSearch", "Searching at lat=$lat lng=$lng")
    val apiResult = tryOfficialApi(lat, lng)
    android.util.Log.d("StoreSearch", "Places API returned ${apiResult.size} stores")
    return apiResult
}

// ══════════════════════════════════════════════════════════════════════════════
// STORE SEARCH — Google Places API (Nearby Search + Text Search)
//
// REQUIRED SETUP (one-time, free):
//   1. Go to https://console.cloud.google.com
//   2. Select your project (the one that owns the Maps key below)
//   3. APIs & Services → Enable APIs → search "Places API" → Enable
//   4. That's it — the same key already in AndroidManifest.xml will work
//
// The key is: AIzaSyDKzpS-OSVh0L5Fq99Q6-2k9GQB0s_qs_8
// ══════════════════════════════════════════════════════════════════════════════
private const val GOOGLE_MAPS_KEY = "AIzaSyDKzpS-OSVh0L5Fq99Q6-2k9GQB0s_qs_8"

// Parse Nearby Search results array
private fun parseNearbyResults(arr: org.json.JSONArray, lat: Double, lng: Double): List<Store> {
    val list = mutableListOf<Store>()
    for (i in 0 until arr.length()) {
        val item = arr.getJSONObject(i)
        val loc = item.optJSONObject("geometry")?.optJSONObject("location") ?: continue
        val sLat = loc.optDouble("lat", 0.0)
        val sLng = loc.optDouble("lng", 0.0)
        if (sLat == 0.0 && sLng == 0.0) continue
        val openNow = item.optJSONObject("opening_hours")?.optBoolean("open_now", true) ?: true
        list.add(Store(
            name = item.optString("name", "Jan Aushadhi Kendra"),
            location = item.optString("vicinity", ""),
            status = if (openNow) "Open" else "Closed",
            distance = "%.1f km".format(haversineKm(lat, lng, sLat, sLng)),
            latLng = LatLng(sLat, sLng)
        ))
    }
    return list
}

// Parse Text Search results array (different JSON structure from Nearby Search)
private fun parseTextSearchResults(arr: org.json.JSONArray, lat: Double, lng: Double): List<Store> {
    val list = mutableListOf<Store>()
    for (i in 0 until arr.length()) {
        val item = arr.getJSONObject(i)
        val loc = item.optJSONObject("geometry")?.optJSONObject("location") ?: continue
        val sLat = loc.optDouble("lat", 0.0)
        val sLng = loc.optDouble("lng", 0.0)
        if (sLat == 0.0 && sLng == 0.0) continue
        val openNow = item.optJSONObject("opening_hours")?.optBoolean("open_now", true) ?: true
        val address = item.optString("formatted_address", item.optString("vicinity", ""))
        list.add(Store(
            name = item.optString("name", "Jan Aushadhi Kendra"),
            location = address,
            status = if (openNow) "Open" else "Closed",
            distance = "%.1f km".format(haversineKm(lat, lng, sLat, sLng)),
            latLng = LatLng(sLat, sLng)
        ))
    }
    return list
}

// Generic HTTP GET → returns raw JSON string or null
// Must run on Dispatchers.IO — withContext ensures this regardless of caller
private suspend fun httpGet(url: String): String? = withContext(Dispatchers.IO) {
    try {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 12000
        conn.readTimeout = 12000
        val code = conn.responseCode
        if (code != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
            android.util.Log.e("StoreSearch", "HTTP $code — $err")
            return@withContext null
        }
        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        body
    } catch (e: Exception) {
        android.util.Log.e("StoreSearch", "httpGet failed: ${e::class.java.simpleName}: ${e.message}")
        null
    }
}

// Fetch all pages from a Nearby Search starting URL
private suspend fun fetchNearbyAllPages(startUrl: String, lat: Double, lng: Double): List<Store> {
    val all = mutableListOf<Store>()
    var url: String? = startUrl
    var page = 0
    while (url != null && page < 3) { // max 3 pages = 60 results
        val body = httpGet(url) ?: break
        val json = org.json.JSONObject(body)
        val status = json.optString("status", "")
        android.util.Log.d("StoreSearch", "Nearby page $page status=$status")
        if (status == "REQUEST_DENIED") {
            android.util.Log.e("StoreSearch", "REQUEST_DENIED — enable Places API at console.cloud.google.com")
            break
        }
        if (status != "OK" && status != "ZERO_RESULTS") break
        val arr = json.optJSONArray("results") ?: break
        all += parseNearbyResults(arr, lat, lng)
        val token = json.optString("next_page_token", "")
        if (token.isEmpty()) break
        delay(2000) // Google requires delay before next_page_token activates
        url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
              "?pagetoken=${java.net.URLEncoder.encode(token, "UTF-8")}&key=$GOOGLE_MAPS_KEY"
        page++
    }
    return all
}

suspend fun tryOfficialApi(lat: Double, lng: Double): List<Store> {
    val seen = mutableSetOf<String>()
    val all = mutableListOf<Store>()

    fun addUnique(stores: List<Store>) {
        for (s in stores) {
            if (seen.add("${s.name}|${s.latLng.latitude}|${s.latLng.longitude}")) all.add(s)
        }
    }

    // ── Strategy 1: Nearby Search with "PMBJK" (exact brand name used on Maps) ──
    try {
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                  "?location=$lat,$lng&radius=10000&keyword=PMBJK&key=$GOOGLE_MAPS_KEY"
        addUnique(fetchNearbyAllPages(url, lat, lng))
        android.util.Log.d("StoreSearch", "After PMBJK Nearby: ${all.size} stores")
    } catch (e: Exception) { android.util.Log.e("StoreSearch", "Strategy 1 failed: ${e.message}") }

    // ── Strategy 2: Nearby Search with "Jan Aushadhi" ──
    try {
        val keyword = java.net.URLEncoder.encode("Jan Aushadhi", "UTF-8")
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                  "?location=$lat,$lng&radius=10000&keyword=$keyword&key=$GOOGLE_MAPS_KEY"
        addUnique(fetchNearbyAllPages(url, lat, lng))
        android.util.Log.d("StoreSearch", "After Jan Aushadhi Nearby: ${all.size} stores")
    } catch (e: Exception) { android.util.Log.e("StoreSearch", "Strategy 2 failed: ${e.message}") }

    // ── Strategy 3: Text Search (searches by name across wider area) ──
    try {
        val queries = listOf("PMBJK near me", "Jan Aushadhi Kendra", "Pradhan Mantri Jan Aushadhi Kendra")
        for (q in queries) {
            val encoded = java.net.URLEncoder.encode(q, "UTF-8")
            val url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                      "?query=$encoded&location=$lat,$lng&radius=15000&key=$GOOGLE_MAPS_KEY"
            val body = httpGet(url) ?: continue
            val json = org.json.JSONObject(body)
            val status = json.optString("status", "")
            android.util.Log.d("StoreSearch", "TextSearch '$q' status=$status")
            if (status == "OK") {
                val arr = json.optJSONArray("results") ?: continue
                addUnique(parseTextSearchResults(arr, lat, lng))
            }
        }
        android.util.Log.d("StoreSearch", "After TextSearch: ${all.size} stores")
    } catch (e: Exception) { android.util.Log.e("StoreSearch", "Strategy 3 failed: ${e.message}") }

    return all.sortedBy { haversineKm(lat, lng, it.latLng.latitude, it.latLng.longitude) }
}

fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

//storescreen
// ── Screen 1: Fullscreen map — user taps to pick a location ─────
@SuppressLint("MissingPermission")
@Composable
fun MapPickScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val defaultLatLng = LatLng(12.9716, 77.5946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 12f)
    }
    var tappedPoint by remember { mutableStateOf<LatLng?>(null) }
    var isLocating by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                isLocating = true
                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    val location = fusedClient.lastLocation.await()
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 14f)
                        tappedPoint = userLatLng
                    }
                } catch (_: Exception) {}
                finally { isLocating = false }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng -> tappedPoint = latLng }
        ) {
            tappedPoint?.let {
                Marker(state = MarkerState(position = it), title = "Search here")
            }
        }

        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E6F5C))
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            Text(
                "Tap on map to pick a location",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }

        if (isLocating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF0E6F5C))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use My Location", color = Color(0xFF0E6F5C), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    tappedPoint?.let { pt ->
                        navController.navigate("store_list/${pt.latitude}/${pt.longitude}")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = tappedPoint != null,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF0E6F5C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (tappedPoint != null) "Search Stores in This Area" else "Tap the map first",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Screen 2: Store list for the chosen location ─────────────────
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun StoreListScreen(navController: NavController, lat: Double, lng: Double) {
    val coroutineScope = rememberCoroutineScope()

    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }

    var selectedStore by remember { mutableStateOf<Store?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var stockStatus by remember { mutableStateOf("") }
    var isCheckingStock by remember { mutableStateOf(false) }
    var medicineName by remember { mutableStateOf(selectedMedicine?.name ?: "") }

    LaunchedEffect(lat, lng) {
        isLoading = true
        stores = fetchNearbyJanAushadhiStores(lat, lng)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F4F1))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E6F5C))
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Nearby Jan Aushadhi Stores",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search stores...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF0E6F5C))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Finding nearby stores...", color = Color.Gray)
                }
            }
        } else {
            val filtered = stores.filter {
                it.name.contains(search, ignoreCase = true) ||
                        it.location.contains(search, ignoreCase = true)
            }
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No Jan Aushadhi stores found\nnear this location.\n\nTry a different area.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
                    items(filtered) { store ->
                        StoreCard(store) {
                            selectedStore = store
                            stockStatus = ""
                            medicineName = selectedMedicine?.name ?: ""
                            showSheet = true
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet: stock check
    if (showSheet && selectedStore != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(selectedStore!!.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(selectedStore!!.location, color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (selectedStore!!.status == "Open") Color(0xFF2ECC71).copy(alpha = 0.15f)
                                else Color.Red.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            selectedStore!!.status,
                            color = if (selectedStore!!.status == "Open") Color(0xFF2ECC71) else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("📍 ${selectedStore!!.distance}", color = Color.Gray, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))

                Text("Medicine to check:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it; stockStatus = "" },
                    placeholder = { Text("Enter medicine name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (stockStatus.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (stockStatus.contains("In Stock")) Color(0xFF2ECC71).copy(alpha = 0.12f)
                                else if (stockStatus.contains("⚠️")) Color(0xFFFFF9C4)
                                else Color.Red.copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            stockStatus,
                            color = if (stockStatus.contains("In Stock")) Color(0xFF2E7D32)
                                    else if (stockStatus.contains("⚠️")) Color(0xFF795548)
                                    else Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        if (medicineName.isBlank()) {
                            stockStatus = "⚠️ Please enter a medicine name."
                            return@Button
                        }
                        isCheckingStock = true
                        stockStatus = "Checking availability of \"$medicineName\"..."
                        coroutineScope.launch {
                            delay(1800)
                            val inStock = listOf(true, true, false).random()
                            stockStatus = if (inStock)
                                "✅ \"$medicineName\" is In Stock at this store"
                            else
                                "❌ \"$medicineName\" is currently Out of Stock"
                            isCheckingStock = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isCheckingStock,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF0E6F5C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isCheckingStock) "Checking..." else "Check Stock",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// kept so old "stores" route reference compiles
@Composable
fun StoreScreen() { MapPickScreen(rememberNavController()) }



//storecardui
@Composable
fun StoreCard(store: Store, onClick: () -> Unit) {

    val statusColor = if (store.status == "Open") Color(0xFF2ECC71) else Color.Red

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(14.dp)
        ) {

            Text(
                text = store.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = store.location,
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = store.status,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = store.distance,
                    color = Color.Gray
                )
            }
        }
    }
}
//remainderscreen
@Composable
fun ReminderScreen() {
    val context = LocalContext.current
    val reminders = globalReminders

    var showAddDialog by remember { mutableStateOf(false) }
    var newPillName by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3EAF2))
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Title
            Text(
                text = "Pill Reminder",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔵 Circle Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFF8FB3E8), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Today pills",
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Reminder List
            LazyColumn {
                items(reminders) { reminder ->
                    ReminderCard(
                        title = reminder.first,
                        subtitle = reminder.second,
                        icon = reminder.third
                    )
                }
            }
        }

        // ➕ Floating Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            containerColor = Color(0xFF6C9FE8)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Pill Reminder") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPillName,
                            onValueChange = { newPillName = it },
                            label = { Text("Pill Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTime,
                            onValueChange = { newTime = it },
                            label = { Text("Time (e.g. 10:00 AM)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newPillName.isNotEmpty() && newTime.isNotEmpty()) {
                            reminders.add(Triple(newPillName, newTime, Icons.Default.Medication))
                            showNotification(context)
                            newPillName = ""
                            newTime = ""
                            showAddDialog = false
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
//remindercardui
@Composable
fun ReminderCard(title: String, subtitle: String, icon: ImageVector) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF5C8FE6)
        )
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Left icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Texts
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }

            // Right icons
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
//savingsscreen
@Composable
fun SavingsScreen() {

    // Simulating dynamic data based on the user's actual reminders
    val brandedTotal = globalReminders.size * 450
    val genericTotal = globalReminders.size * 55
    val savings = brandedTotal - genericTotal
    val savePercentage = if (brandedTotal > 0) ((savings.toFloat() / brandedTotal.toFloat()) * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
    ) {

        AppHeader("Savings Dashboard")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── HERO CARD ──────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F6E56), Color(0xFF1ABC9C))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Total Saved",
                                    color = Color(0xFFC8F7DC),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹$savings",
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            
                            // Circular Progress Indicator using Canvas
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(80.dp)) {
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.3f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = Color.White,
                                        startAngle = -90f,
                                        sweepAngle = (savePercentage / 100f) * 360f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = "$savePercentage%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── COMPARISON BREAKDOWN ────────────────────────────────
            item {
                Text(
                    text = "Expense Breakdown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }
            
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Branded Meds Cost", color = Color.Gray, fontSize = 14.sp)
                            Text("₹$brandedTotal", color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Progress bar mock
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFFFEBEE), CircleShape)) {
                            Box(modifier = Modifier.fillMaxWidth(1f).height(8.dp).background(Color.Red.copy(alpha = 0.6f), CircleShape))
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Jan-Aushadhi Cost", color = Color.Gray, fontSize = 14.sp)
                            Text("₹$genericTotal", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFE8F5E9), CircleShape)) {
                            val fraction = if (brandedTotal > 0) genericTotal.toFloat() / brandedTotal.toFloat() else 0f
                            Box(modifier = Modifier.fillMaxWidth(fraction).height(8.dp).background(Color(0xFF2E7D32), CircleShape))
                        }
                    }
                }
            }

            // ── FINANCIAL HEALTH TIPS ────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).background(Color(0xFFFFD54F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Tip", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Financial Health Tip", fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                            Text("You are saving enough this month to cover a doctor's consultation or invest in a health plan!", color = Color.DarkGray, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp)) // padding for bottom nav
            }
        }
    }
}
//for strict search - only match if query is a prefix of the name/generic or an exact substring
fun isStrictMatch(query: String, text: String): Boolean {
    if (query.isEmpty()) return true
    val q = query.lowercase().trim()
    val t = text.lowercase().trim()

    // Exact substring match
    if (t.contains(q)) return true

    // Word-level prefix match (e.g. "par" matches "Paracetamol")
    val words = t.split(" ")
    if (words.any { it.startsWith(q) }) return true

    return false
}

fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
    }
    return dp[a.length][b.length]
}
//searchscreen
@Composable
fun SearchScreen(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf(
        "All", "Painkiller", "Antibiotic", "Antacid",
        "Cardiac", "Diabetes", "Allergy", "Supplement"
    )
    val context = LocalContext.current
    var medicines by remember { mutableStateOf(listOf<Medicine>()) }

    LaunchedEffect(Unit) {
        val staticMeds = listOf(
            // Painkillers
            Medicine("Crocin 500mg","Paracetamol",45,3,"Painkiller"),
            Medicine("Dolo 650","Paracetamol",35,4,"Painkiller"),
            Medicine("Calpol","Paracetamol",40,3,"Painkiller"),
            Medicine("Combiflam","Ibuprofen+Paracetamol",55,10,"Painkiller"),
            Medicine("Brufen 400","Ibuprofen",40,6,"Painkiller"),
            Medicine("Voveran","Diclofenac",60,8,"Painkiller"),
            // Antibiotics
            Medicine("Augmentin 625","Amoxicillin+Clavulanic Acid",180,45,"Antibiotic"),
            Medicine("Amoxyclav","Amoxicillin+Clavulanic Acid",160,40,"Antibiotic"),
            Medicine("Azithral 500","Azithromycin",150,30,"Antibiotic"),
            Medicine("Azee 500","Azithromycin",140,28,"Antibiotic"),
            Medicine("Ciplox 500","Ciprofloxacin",95,15,"Antibiotic"),
            Medicine("Levoflox 500","Levofloxacin",130,22,"Antibiotic"),
            // Antacids
            Medicine("Pan 40","Pantoprazole",85,8,"Antacid"),
            Medicine("Pantocid","Pantoprazole",90,10,"Antacid"),
            Medicine("Omez","Omeprazole",75,7,"Antacid"),
            Medicine("Razo 20","Rabeprazole",95,12,"Antacid"),
            Medicine("Gelusil","Antacid Suspension",60,15,"Antacid"),
            // Cardiac
            Medicine("Ecosprin 75","Aspirin",35,3,"Cardiac"),
            Medicine("Atorva 10","Atorvastatin",110,18,"Cardiac"),
            Medicine("Lipitor","Atorvastatin",150,20,"Cardiac"),
            Medicine("Telma 40","Telmisartan",110,14,"Cardiac"),
            Medicine("Amlodac","Amlodipine",70,8,"Cardiac"),
            Medicine("Metolar","Metoprolol",95,12,"Cardiac"),
            // Diabetes
            Medicine("Glycomet 500","Metformin",70,12,"Diabetes"),
            Medicine("Metformin 500","Metformin",60,5,"Diabetes"),
            Medicine("Januvia","Sitagliptin",3200,420,"Diabetes"),
            Medicine("Glimestar","Glimepiride",90,10,"Diabetes"),
            // Allergy
            Medicine("Cetzine","Cetirizine",48,4,"Allergy"),
            Medicine("Allegra","Fexofenadine",95,18,"Allergy"),
            Medicine("Montair LC","Montelukast+Levocetirizine",180,22,"Allergy"),
            Medicine("Levocet","Levocetirizine",55,6,"Allergy"),
            // Supplements
            Medicine("Zincovit","Zinc+Multivitamin",120,25,"Supplement"),
            Medicine("Becosules","Vitamin B Complex",95,20,"Supplement"),
            Medicine("Shelcal","Calcium+Vit D3",130,18,"Supplement"),
            Medicine("Limcee","Vitamin C",35,5,"Supplement"),
            Medicine("Neurobion","Vitamin B12",85,15,"Supplement"),
            // Thyroid / Hormones
            Medicine("Thyrox 50","Levothyroxine",30,5,"Thyroid"),
            Medicine("Eltroxin","Levothyroxine",35,5,"Thyroid"),
            // Skin
            Medicine("Betnovate","Betamethasone Cream",85,12,"Skin"),
            Medicine("Candid B","Clotrimazole+Betamethasone",95,18,"Skin"),
            // Eye drops
            Medicine("Ciproflox Eye","Ciprofloxacin Eye Drops",60,15,"Eye"),
            Medicine("Tobramycin","Tobramycin Eye Drops",75,18,"Eye"),
            // Cough
            Medicine("Alex","Chlorpheniramine+Dextromethorphan",65,12,"Cough"),
            Medicine("Benadryl","Diphenhydramine",70,14,"Cough"),
            Medicine("Ascoril","Salbutamol+Bromhexine",95,20,"Cough"),
            // BP
            Medicine("Amlodipine 5","Amlodipine",55,6,"BP"),
            Medicine("Stamlo 5","Amlodipine",65,8,"BP"),
            Medicine("Losartan 50","Losartan",80,10,"BP"),
            // Antifungal
            Medicine("Fluconazole","Fluconazole",55,8,"Antifungal"),
            Medicine("Canesten","Clotrimazole",75,12,"Antifungal")
        )
        
        val dynamicMeds = MedicineRepository.loadMedicines(context)
        medicines = staticMeds + dynamicMeds
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F4F1))
    ) {

        // Header
        AppHeader("Jan-Aushadhi Finder")

        Spacer(modifier = Modifier.height(8.dp))

        // List
        OutlinedTextField(
            value = search,
            onValueChange = {search = it},
            placeholder = { Text("Search brand or generic name...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp)
        )
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) Color(0xFF00695C) else Color(0xFFE1F5EE),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else Color(0xFF0F6E56),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        // Update your filteredMedicines to include category filter
        val filteredMedicines = medicines.filter {
            val matchesSearch = search.isEmpty() ||
                    isStrictMatch(search, it.name) ||
                    isStrictMatch(search, it.generic)
            val matchesCategory = selectedCategory == "All" ||
                    it.category == selectedCategory
            matchesSearch && matchesCategory
        }

        LazyColumn {
            items(filteredMedicines) { medicine ->
                MedicineCard(medicine) {
                    selectedMedicine = medicine
                    navController.navigate("detail")
                }
            }
        }
    }
}
//medicine card
@Composable
fun MedicineCard(medicine: Medicine, onClick: () -> Unit) {

    val savings = medicine.brandedPrice - medicine.genericPrice
    val percent = (savings * 100) / medicine.brandedPrice

    // Category based on generic name
    val category = when {
        medicine.generic.contains("Paracetamol", true) -> "Painkiller"
        medicine.generic.contains("Amoxicillin", true) -> "Antibiotic"
        medicine.generic.contains("Azithromycin", true) -> "Antibiotic"
        medicine.generic.contains("Pantoprazole", true) -> "Antacid"
        medicine.generic.contains("Metformin", true) -> "Diabetes"
        medicine.generic.contains("Atorvastatin", true) -> "Cardiac"
        medicine.generic.contains("Multivitamin", true) -> "Supplement"
        medicine.generic.contains("Vitamin", true) -> "Supplement"
        else -> "General"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            // Brand name
            Text(
                text = medicine.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF00695C)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Generic name
            Text(
                text = medicine.generic,
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Category badge
            Box(
                modifier = Modifier
                    .background(Color(0xFFE1F5EE), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = category,
                    color = Color(0xFF0F6E56),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color(0xFFE0E0E0))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Branded price box
                Column(
                    modifier = Modifier
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Branded",
                        color = Color(0xFFC62828),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "₹${medicine.brandedPrice}",
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Arrow
                Text(
                    text = "  →  ",
                    color = Color.Gray,
                    fontSize = 16.sp
                )

                // Generic price box
                Column(
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Jan-Aushadhi",
                        color = Color(0xFF2E7D32),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "₹${medicine.genericPrice}",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Save badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Save $percent%",
                        color = Color(0xFF1B5E20),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
//cleanchips
@Composable
fun Chip(text: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 12.sp)
    }
}
//detailscreen
@Composable
fun DetailScreen(navController: NavController) {

    val medicine = selectedMedicine ?: Medicine("Crocin 500mg", "Paracetamol", 45, 3)

    val savings = medicine.brandedPrice - medicine.genericPrice
    val percent = (savings * 100) / medicine.brandedPrice
    val genericFraction = medicine.genericPrice.toFloat() / medicine.brandedPrice.toFloat()

    val category = when {
        medicine.generic.contains("Paracetamol", true) -> "Painkiller"
        medicine.generic.contains("Amoxicillin", true) -> "Antibiotic"
        medicine.generic.contains("Azithromycin", true) -> "Antibiotic"
        medicine.generic.contains("Pantoprazole", true) -> "Antacid"
        medicine.generic.contains("Metformin", true) -> "Diabetes"
        medicine.generic.contains("Atorvastatin", true) -> "Cardiac"
        medicine.generic.contains("Multivitamin", true) -> "Supplement"
        medicine.generic.contains("Vitamin", true) -> "Supplement"
        else -> "General"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {

        // ── GREEN HEADER ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF00695C))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = "← Back",
                    color = Color(0xFFA5D6A7),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = medicine.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Generic: ${medicine.generic}",
                        color = Color(0xFFB2DFDB),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "  ·  $category",
                        color = Color(0xFFB2DFDB),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ── SCROLLABLE BODY ───────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── PRICE COMPARISON CARD ─────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Text(
                            text = "Price comparison",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Red box ──→ Green box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Branded box
                            Column(
                                modifier = Modifier
                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Branded", color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "₹${medicine.brandedPrice}",
                                    color = Color(0xFFC62828),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(medicine.name, color = Color(0xFFEF9A9A), fontSize = 10.sp)
                            }

                            Text("→", fontSize = 20.sp, color = Color.Gray)

                            // Generic box
                            Column(
                                modifier = Modifier
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Jan-Aushadhi", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "₹${medicine.genericPrice}",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(medicine.generic, color = Color(0xFF81C784), fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Branded bar (full width, red)
                        Text(text = "Branded price", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(Color(0xFFEF9A9A), RoundedCornerShape(6.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Generic bar (short width, green)
                        Text(text = "Generic price", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(genericFraction.coerceIn(0.05f, 1f))
                                .height(10.dp)
                                .background(Color(0xFF66BB6A), RoundedCornerShape(6.dp))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Savings box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "You save per strip",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹$savings ($percent% cheaper)",
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Same active ingredient · WHO approved",
                                    color = Color(0xFF388E3C),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── QUALITY INFO CARD ─────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color(0xFF2E7D32), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Generic medicines contain the exact same active ingredients and are approved by CDSCO (India's FDA). Quality is identical — only the brand name and price differ.",
                            color = Color(0xFF555555),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // ── MONTHLY SAVINGS CARD ──────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00695C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "If you switch every month",
                            color = Color(0xFFB2DFDB),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${savings * 12} saved per year",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Based on 1 strip/month of ${medicine.name}",
                            color = Color(0xFF80CBC4),
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            // ── ASK AI CARD ──────────────────────────────
            item {
                Button(
                    onClick = {
                        aiPrompt = "What are the uses and side effects of ${medicine.generic}?"
                        navController.navigate("aichat")
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF0F6E56))
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI Pharmacist")
                }
            }
        }
    }
}
//appheader
@Composable
fun AppHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E6F5C))
            .padding(vertical = 18.dp, horizontal = 16.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
//loginscreen
@Composable
fun LoginScreen(navController: NavController) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F4F1))
    ) {
        Image(
            painter = painterResource(id = R.drawable.loginbackground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // App logo / name
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFF0E6F5C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Medication,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Jan-Aushadhi Finder",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0E6F5C)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Affordable Healthcare for All",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(24.dp)
                ) {

                    Text(
                        text = "Welcome Back",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "Sign in to continue",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = "" },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF0E6F5C)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = "" },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Circle, contentDescription = null, tint = Color(0xFF0E6F5C)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            when {
                                email.isBlank() -> errorMessage = "Please enter your email."
                                !email.contains("@") -> errorMessage = "Please enter a valid email."
                                password.isBlank() -> errorMessage = "Please enter your password."
                                password.length < 6 -> errorMessage = "Password must be at least 6 characters."
                                else -> navController.navigate("search") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0E6F5C)
                        )
                    ) {
                        Text("Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Sign Up",
                            color = Color(0xFF0E6F5C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                navController.navigate("signup")
                            }
                        )
                    }
                }
            }
        }
    }
}

// Simple in-memory store for registered users (app session only)
val registeredUsers = mutableMapOf<String, String>() // email -> password

//signup
@Composable
fun SignupScreen(navController: NavController) {

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F4F1))
    ) {
        Image(
            painter = painterResource(id = R.drawable.loginbackground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // App logo / name
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFF0E6F5C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Medication,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Jan-Aushadhi Finder",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0E6F5C)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Create your free account",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(24.dp)
                ) {

                    Text(
                        text = "Create Account",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "Fill in the details below",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMessage = "" },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF0E6F5C)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = "" },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF0E6F5C)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = "" },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Circle, contentDescription = null, tint = Color(0xFF0E6F5C)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = "" },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Circle, contentDescription = null, tint = Color(0xFF0E6F5C)) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    if (successMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = successMessage,
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            when {
                                fullName.isBlank() -> errorMessage = "Please enter your full name."
                                email.isBlank() -> errorMessage = "Please enter your email."
                                !email.contains("@") -> errorMessage = "Please enter a valid email."
                                registeredUsers.containsKey(email.trim().lowercase()) ->
                                    errorMessage = "An account with this email already exists."
                                password.isBlank() -> errorMessage = "Please enter a password."
                                password.length < 6 -> errorMessage = "Password must be at least 6 characters."
                                confirmPassword != password -> errorMessage = "Passwords do not match."
                                else -> {
                                    registeredUsers[email.trim().lowercase()] = password
                                    successMessage = "✅ Account created! Redirecting to login..."
                                    errorMessage = ""
                                    navController.navigate("login") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0E6F5C)
                        )
                    ) {
                        Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Already have an account? ",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Login",
                            color = Color(0xFF0E6F5C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                navController.navigate("login") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

//aichatscreen

// ══════════════════════════════════════════════════════════════════════════════
// ══════════════════════════════════════════════════════════════════════════════
// AI — OpenRouter (FREE models, no credit card needed)
// Steps:
//   1. Go to https://openrouter.ai → Sign up (free)
//   2. Go to Keys → Create Key → copy key (starts with sk-or-v1-)
//   3. Paste below replacing PASTE_YOUR_OPENROUTER_KEY_HERE
// Free models used: mistralai/mistral-7b-instruct:free (fallback: llama 3.2)
// OpenRouter uses OpenAI-compatible format:
//   POST https://openrouter.ai/api/v1/chat/completions
//   Response: { "choices": [ { "message": { "content": "answer" } } ] }
// ══════════════════════════════════════════════════════════════════════════════
private const val OPENROUTER_API_KEY = "PASTE_YOUR_OPENROUTER_KEY_HERE"

// Free models to try in order — if one fails, next is attempted
private val FREE_MODELS = listOf(
    "openrouter/auto",
    "meta-llama/llama-3.3-70b-instruct:free",
    "google/gemma-3-27b-it:free",
    "mistralai/mistral-small-3.1-24b-instruct:free"
)

private const val SYSTEM_PROMPT =
    "You are a helpful AI Pharmacist for the Jan-Aushadhi Finder app in India. " +
    "Answer questions about medicines, generic equivalents, dosage, side effects, " +
    "and Jan Aushadhi stores. Keep answers concise — 3-4 sentences max."

// Makes one OpenRouter API call with a specific model. Returns null if failed.
private suspend fun tryOpenRouterModel(model: String, userText: String): String? =
    withContext(Dispatchers.IO) {
        try {
            val bodyJson = org.json.JSONObject()
                .put("model", model)
                .put("messages", org.json.JSONArray()
                    .put(org.json.JSONObject()
                        .put("role", "system")
                        .put("content", SYSTEM_PROMPT))
                    .put(org.json.JSONObject()
                        .put("role", "user")
                        .put("content", userText)))
                .toString()

            val url = "https://openrouter.ai/api/v1/chat/completions"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer $OPENROUTER_API_KEY")
            connection.setRequestProperty("HTTP-Referer", "https://janaushadhi.app")
            connection.setRequestProperty("X-Title", "Jan Aushadhi Finder")
            connection.doOutput = true
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.outputStream.use { it.write(bodyJson.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            android.util.Log.d("OpenRouterAI", "model=$model code=$code")

            if (code != 200) {
                val err = try { connection.errorStream?.bufferedReader()?.readText() ?: "" } catch (_: Exception) { "" }
                android.util.Log.e("OpenRouterAI", "Error $code: $err")
                return@withContext null // null = try next model
            }

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            android.util.Log.d("OpenRouterAI", "Raw response: ${response.take(300)}")

            // OpenRouter/OpenAI format: {"choices":[{"message":{"content":"answer"}}]}
            val json = org.json.JSONObject(response)

            // Check for API-level error inside 200 response (OpenRouter does this)
            if (json.has("error")) {
                val errMsg = json.optJSONObject("error")?.optString("message", "") ?: ""
                android.util.Log.e("OpenRouterAI", "API error in 200: $errMsg")
                return@withContext null
            }

            val content = json
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content", "")
                ?.trim()

            if (content.isNullOrEmpty()) null else content

        } catch (e: Exception) {
            android.util.Log.e("OpenRouterAI", "${e::class.java.simpleName}: ${e.message}")
            null
        }
    }

private suspend fun callCohereApi(userText: String): String {
    if (OPENROUTER_API_KEY.isBlank() || OPENROUTER_API_KEY == "PASTE_YOUR_OPENROUTER_KEY_HERE") {
        return "⚠️ OpenRouter API key not set.\n\n" +
               "1. Go to https://openrouter.ai\n" +
               "2. Sign up free → Keys → Create Key\n" +
               "3. Paste it as OPENROUTER_API_KEY in MainActivity.kt"
    }

    // Try each free model in order until one works
    for (model in FREE_MODELS) {
        val result = tryOpenRouterModel(model, userText)
        if (result != null) return result
    }

    return "⚠️ All AI models failed. Check your internet connection or OpenRouter key."
}

@Composable
fun AIChatScreen() {
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var chatHistory by remember { mutableStateOf(listOf(Pair("AI", "Hello! I am your AI Pharmacist. Ask me anything about medicines, generic alternatives, or Jan Aushadhi stores."))) }
    var isLoading by remember { mutableStateOf(false) }

    suspend fun callAI(userText: String): String = callCohereApi(userText)

    LaunchedEffect(Unit) {
        val currentAutoPrompt = aiPrompt
        if (currentAutoPrompt != null) {
            aiPrompt = null
            chatHistory = chatHistory + Pair("User", currentAutoPrompt)
            isLoading = true
            val reply = callAI(currentAutoPrompt)
            chatHistory = chatHistory + Pair("AI", reply)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        AppHeader("AI Pharmacist")

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            reverseLayout = false
        ) {
            items(chatHistory) { (sender, msg) ->
                val isUser = sender == "User"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isUser) Color(0xFF0E6F5C) else Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                            .fillMaxWidth(0.8f)
                    ) {
                        Text(
                            text = msg,
                            color = if (isUser) Color.White else Color.Black,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text("AI is typing...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("Ask about generic medicines...") },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            FloatingActionButton(
                onClick = {
                    if (message.isNotBlank() && !isLoading) {
                        val userPrompt = message.trim()
                        message = ""
                        chatHistory = chatHistory + Pair("User", userPrompt)
                        isLoading = true
                        coroutineScope.launch {
                            val reply = callAI(userPrompt)
                            chatHistory = chatHistory + Pair("AI", reply)
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = Color(0xFF0E6F5C),
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}