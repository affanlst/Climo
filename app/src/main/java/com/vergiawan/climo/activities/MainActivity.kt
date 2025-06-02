package com.vergiawan.climo.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.vergiawan.climo.CalculateHypothermia
import com.vergiawan.climo.DataObserverViewModel
import com.vergiawan.climo.LocationViewModel
import com.vergiawan.climo.R
import com.vergiawan.climo.StatusViewModel
import com.vergiawan.climo.databinding.ActivityMainBinding
import com.vergiawan.climo.fragments.HistoryFragment
import com.vergiawan.climo.fragments.MapFragment
import nl.joery.animatedbottombar.AnimatedBottomBar
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private lateinit var dialog: BottomSheetDialog
    private lateinit var resultDetection: String

    private val locationViewModel: LocationViewModel by viewModels()
    private val statusViewModel: StatusViewModel by viewModels()
    private val dataObserverViewModel: DataObserverViewModel by viewModels()

    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var idSelector: String
    private val deviceIds = mutableListOf<String>()

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private lateinit var userIndicator: CardView

    private val CHANNEL_ID = "status_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        idSelector = "01"

        // Firebase Instance
        database = FirebaseDatabase.getInstance().getReference()
        getDevicesId()
        getGeoLocation()
        getStatus()

        // Show MapView From Fragment
        supportFragmentManager.beginTransaction().replace(R.id.map_layout, MapFragment()).commit()

        userIndicator = findViewById(R.id.user_indicator)

        binding.openDialog.setOnClickListener {
            showBottomSheet()
        }

        binding.jacketUserIcon.setOnClickListener { view ->
            showPopupMenu(view)
            Log.d("Selector", idSelector)
        }

        binding.bottomBar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                Log.d("Bottom_Bar", "Selected index: $newIndex, title: ${newTab.title}")
                when (newTab.id) {
                    R.id.tab_profile -> {
                        binding.openDialog.visibility = View.VISIBLE
                        binding.titleLayout.visibility = View.VISIBLE

                        val bundle = Bundle()
                        bundle.putString("ID_SELECTOR", idSelector)

                        val fragment = MapFragment()
                        fragment.arguments = bundle

                        supportFragmentManager.beginTransaction()
                            .replace(R.id.map_layout, fragment).commit()
                    }

                    R.id.tab_history -> {
                        binding.openDialog.visibility = View.GONE
                        binding.titleLayout.visibility = View.GONE

                        val bundle = Bundle()
                        bundle.putString("ID_SELECTOR", idSelector)

                        val fragment = HistoryFragment()
                        fragment.arguments = bundle

                        supportFragmentManager.beginTransaction()
                            .replace(R.id.map_layout, fragment).commit()
                    }
                }
            }
        })

        createNotificationChannel()

    }

    private fun showBottomSheet() {
        dialog = BottomSheetDialog(this)
        dialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)

        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val btnClose = view.findViewById<ImageView>(R.id.close_button)
        val tvTemperature = view.findViewById<TextView>(R.id.tv_temperature)
        val tvHeartRate = view.findViewById<TextView>(R.id.tv_heart_rate)
        val tvBloodOxygen = view.findViewById<TextView>(R.id.tv_blood_oxygen)
        val tvStatus = view.findViewById<TextView>(R.id.tv_current_status)
        val temperatureContent = view.findViewById<RelativeLayout>(R.id.temperature_content)
        val heartRateContent = view.findViewById<RelativeLayout>(R.id.heart_rate_content)
        val bloodOxygenContent = view.findViewById<RelativeLayout>(R.id.blood_oxygen_content)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        temperatureContent.setOnClickListener {
            val intent = Intent(this, TemperatureChartActivity::class.java)
            intent.putExtra("ID_SELECTOR", idSelector)
            startActivity(intent)
        }

        heartRateContent.setOnClickListener {
            val intent = Intent(this, HeartRateChartActivity::class.java)
            intent.putExtra("ID_SELECTOR", idSelector)
            startActivity(intent)
        }

        bloodOxygenContent.setOnClickListener {
            val intent = Intent(this, OxygenRateChartActivity::class.java)
            intent.putExtra("ID_SELECTOR", idSelector)
            startActivity(intent)
        }

        getBodyCondition(tvTemperature, tvHeartRate, tvBloodOxygen)
        calculateHypothermia(tvStatus)
        dataChangeObserver()

        dialog.setCancelable(true)
        dialog.setContentView(view)
        dialog.show()

    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        deviceIds.forEachIndexed { index, id ->
            popupMenu.menu.add(0, index, 0, id)
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            idSelector = deviceIds[menuItem.itemId]
            Toast.makeText(this, "ID Terpilih: $idSelector", Toast.LENGTH_SHORT).show()
            true
        }
        popupMenu.show()
    }

    private fun getDevicesId() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deviceIds.clear()
                val totalUser = snapshot.childrenCount
                for (branch in snapshot.children) {
                    Log.d("User", "Key: ${branch.key}")
                    branch.key?.let {
                        deviceIds.add(it)
                    }
                }
                Log.d("Total User", totalUser.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity,
                    "Gagal mendapatkan data: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun dataChangeObserver() {
        val reference = database.child(idSelector).child("MLX90614")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val objectTemp = snapshot.child("object_temp").value.toString()

                dataObserverViewModel.setObjectTemp(objectTemp.toDouble())
                Log.d("DataChange", "Data Change : ${dataObserverViewModel.dataChange}")

                if (dataObserverViewModel.dataChange == true) {
                    val onlineColor =
                        ContextCompat.getColor(this@MainActivity, R.color.online_color)
                    userIndicator.setCardBackgroundColor(onlineColor)
                    runnable?.let { handler.removeCallbacks(it) }
                } else {
                    scheduleTask()
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", "Failed to read value : ${error.message}")
            }
        })
    }

    private fun getBodyCondition(
        tvTemperature: TextView,
        tvHeartRate: TextView,
        tvBloodOxygen: TextView
    ) {
        val reference = database.child(idSelector)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val objectTemp = snapshot.child("MLX90614").child("object_temp").value
                val heartRate = snapshot.child("MAX30102").child("heart_rate").value.toString()
                val bloodOxygen = snapshot.child("MAX30102").child("spo2").value

                tvTemperature.text = "$objectTemp°C"
//                tvHeartRate.text = "$heartRate bpm"
                tvHeartRate.text = String.format("%.1f bpm", heartRate.toDouble())
                // TES Kontributor
                tvBloodOxygen.text = "$bloodOxygen%"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", "Failed to read value : ${error.message}")
            }
        })
    }

    private fun getGeoLocation() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val geoLng =
                    snapshot.child(idSelector).child("GPS").child("longitude").value.toString()
                        .toDouble()
                val geoLat =
                    snapshot.child(idSelector).child("GPS").child("latitude").value.toString()
                        .toDouble()
                locationViewModel.setGeoPoint(geoLat, geoLng)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", "Failed to read value : ${error.message}")
            }
        })
    }

    private fun getStatus() {
        val reference = database.child(idSelector)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bodyTemperature =
                    snapshot.child("MLX90614").child("object_temp").value.toString().toDouble()
//                val heartRate =
//                    snapshot.child("MAX30102").child("heart_rate").value.toString().toInt()
                val heartRate =
                    snapshot.child("MAX30102").child("heart_rate").value.toString().toDouble()
//                val spo2 = snapshot.child("MAX30102").child("spo2").value.toString().toInt()
                val spo2 = snapshot.child("MAX30102").child("spo2").value.toString().toDouble()
                val geoLat = snapshot.child("GPS").child("latitude").value.toString()
                val geoLng = snapshot.child("GPS").child("longitude").value.toString()

                val geoPoint = "$geoLat, $geoLng"

                val statusHypothermia = CalculateHypothermia(bodyTemperature, heartRate, spo2)
                val status: String = statusHypothermia.detectHypothermia()
                //Log.i("Status", status)

                checkStatus(status)

                if (status != statusViewModel.getStatus()) {
                    statusViewModel.setStatus(status)
                    Log.i("Different Status", statusViewModel.getStatus())
                    // Store to firestore
                    historyStore(
                        status,
                        geoPoint,
                        heartRate.toString(),
                        spo2.toString(),
                        bodyTemperature.toString()
                    )
                } else {
                    Log.i("Same Status", statusViewModel.getStatus())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", "Failed to read value : ${error.message}")
            }
        })
    }

    private fun historyStore(
        status: String,
        geoPoint: String,
        heartRate: String,
        spo2: String,
        bodyTemperature: String
    ) {
        val history: MutableMap<String, Any> = HashMap()
        history["status"] = status
        history["geoPoint"] = geoPoint
        history["heartRate"] = heartRate
        history["spo2"] = spo2
        history["bodyTemperature"] = bodyTemperature

        val currentTimestamp = System.currentTimeMillis()
        val formatter = SimpleDateFormat("MMM dd,yyyy HH:mm:ss", Locale.getDefault())
        val formattedTimestamp = formatter.format(currentTimestamp)

        history["timeStamp"] = formattedTimestamp.toString()

        firestore.collection(idSelector).add(history).addOnSuccessListener {
            Log.i("Firestore Upload", "Added ${it.id}")
        }
            .addOnFailureListener {
                Log.i("Firestore Upload Error", "Added $it")
            }
    }

    private fun calculateHypothermia(tvCurrentCondition: TextView) {
        val reference = database.child(idSelector)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val objectTemp =
                    snapshot.child("MLX90614").child("object_temp").value.toString().toDouble()
//                val heartRate =
//                    snapshot.child("MAX30102").child("heart_rate").value.toString().toInt()
//                val bloodOxygen = snapshot.child("MAX30102").child("spo2").value.toString().toInt()

                val heartRate =
                    snapshot.child("MAX30102").child("heart_rate").value.toString().toDouble()
                val bloodOxygen = snapshot.child("MAX30102").child("spo2").value.toString().toDouble()

                val calculateHypothermia =
                    CalculateHypothermia(
                        objectTemp,
                        heartRate,
                        bloodOxygen
                    )
                resultDetection = calculateHypothermia.detectHypothermia()
                tvCurrentCondition.text = resultDetection
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Error", "Failed to read value : ${error.message}")
            }
        })
    }

    private fun scheduleTask() {
        // Membatalkan jadwal Runnable sebelumnya
        runnable?.let { handler.removeCallbacks(it) }

        // Jadwalkan Runnable baru untuk menjalankan fungsi A setelah 15 detik
        runnable = Runnable {
            userOffline()
        }
        handler.postDelayed(runnable!!, 8000)
    }

    private fun userOffline() {
        Log.d("Offline", "User Offline")
        Toast.makeText(this, "Offline", Toast.LENGTH_SHORT).show()
        val offlineColor = ContextCompat.getColor(this, R.color.offline_color)
        userIndicator.setCardBackgroundColor(offlineColor)
    }

    private fun checkStatus(status: String) {
        if (status == "Severe Hypothermia") {
            showNotification()
        } else {
            removeNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hypothermia Notification Channel"
            val descriptionText = "Notification channel for Severe Hypothermia Alert"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.climo_icon)
            .setContentTitle("Status Alert")
            .setContentText("Severe Hypothermia detected!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun removeNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
        }
    }

}
