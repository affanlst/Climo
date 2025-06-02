package com.vergiawan.climo.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.firestore.FirebaseFirestore
import com.vergiawan.climo.R
import com.vergiawan.climo.databinding.ActivityHeartRateChartBinding
import java.text.SimpleDateFormat
import java.util.Locale

class HeartRateChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHeartRateChartBinding
    private lateinit var firestore: FirebaseFirestore
    private var isDarkTheme: Boolean = false
    private lateinit var idSelector: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHeartRateChartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bundle = intent.extras
        idSelector = bundle?.getString("ID_SELECTOR").toString()

        Toast.makeText(this, idSelector, Toast.LENGTH_SHORT).show()

        isDarkTheme = isDarkTheme()
        firestore = FirebaseFirestore.getInstance()
        fetchChartData()
        binding.backIcon.setOnClickListener { finish() }
    }

    private fun isDarkTheme(): Boolean {
        val currentNightMode =
            resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun fetchChartData() {
        firestore.collection(idSelector)
            .orderBy("timeStamp")
            .get()
            .addOnSuccessListener { documents ->
                val entries = ArrayList<Entry>()
                val dateFormat = SimpleDateFormat("MMM dd,yyyy HH:mm:ss", Locale.getDefault())

                for (document in documents) {
                    val heartRate = document.getString("heartRate")
                    val timestampString = document.getString("timeStamp")

                    if (heartRate != null && timestampString != null) {
                        try {
                            val timestamp = dateFormat.parse(timestampString)
                            if (timestamp != null) {
                                val hours = timestamp.hours + (timestamp.minutes / 60f)
                                entries.add(Entry(hours, heartRate.toFloat()))
                            }
                        } catch (e: Exception) {
                            Log.e("HeartRateChartActivity", "Error parsing date: ", e)
                        }
                    }
                }

                displayChart(entries)
            }
            .addOnFailureListener { exception ->
                Log.e("HeartRateChartActivity", "Error getting documents: ", exception)
            }
    }

    private fun displayChart(entries: List<Entry>) {
        val lineDataSet = LineDataSet(entries, "Suhu")
        lineDataSet.color = Color.BLUE
        lineDataSet.valueTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        lineDataSet.lineWidth = 2f
        lineDataSet.setDrawValues(true)
        lineDataSet.setDrawCircles(true)
        lineDataSet.circleRadius = 4f
        lineDataSet.circleHoleRadius = 2f

        val lineData = LineData(lineDataSet)
        binding.lineChart.data = lineData

        binding.lineChart.description.isEnabled = true
        binding.lineChart.legend.isEnabled = false

        val xAxis = binding.lineChart.xAxis
        xAxis.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK

        val leftAxis = binding.lineChart.axisLeft
        leftAxis.textColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        leftAxis.axisLineColor = if (isDarkTheme) Color.WHITE else Color.BLACK
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawAxisLine(true)
        leftAxis.setDrawLabels(true)

        val rightAxis = binding.lineChart.axisRight
        rightAxis.isEnabled = false

        binding.lineChart.invalidate()
    }

}