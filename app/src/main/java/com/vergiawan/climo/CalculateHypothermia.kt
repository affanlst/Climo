package com.vergiawan.climo

class CalculateHypothermia(
    private val bodyTemperature: Double,
    private val heartRate: Double,
    private val oxygenSaturation: Double
) {
    fun detectHypothermia(): String {
        // Fuzzyfikasi
        val bodyTemperatureFuzzy = fuzzifyBodyTemperature(bodyTemperature)
        val heartRateFuzzy = fuzzifyHeartRate(heartRate)
        val oxygenSaturationFuzzy = fuzzifyOxygenSaturation(oxygenSaturation)

        // Inferensi
        val resultInference = inference(bodyTemperatureFuzzy, heartRateFuzzy, oxygenSaturationFuzzy)

        // Return Defuzzyfikasi
        return defuzzify(resultInference)
    }

    private fun fuzzifyBodyTemperature(bodyTemperature: Double): String {
        return if (bodyTemperature < 28) {
            "Extreme Low"
        } else if (bodyTemperature < 32) {
            "Low"
        } else if (bodyTemperature < 35) {
            "Medium"
        } else {
            "Normal"
        }
    }

    private fun fuzzifyHeartRate(heartRate: Double): String {
        return when (heartRate) {
            in 70.0..120.0 -> { "Normal" }
            in 50.0..69.0 -> { "Medium" }
            else -> { "Low" }
        }
    }

    private fun fuzzifyOxygenSaturation(oxygenSaturation: Double): String {
        return if (oxygenSaturation > 90) {
            "Normal"
        } else if (oxygenSaturation in 80.0..89.0) {
            "Medium"
        } else {
            "Low"
        }
    }

    // Fungsi inferensi
    private fun inference(
        bodyTemperatureFuzzy: String,
        heartRateFuzzy: String,
        oxygenSaturationFuzzy: String
    ): String {
        return when {
            // Rule 1: Mild Hypothermia
            (bodyTemperatureFuzzy == "Low" || bodyTemperatureFuzzy == "Medium") && heartRateFuzzy == "Normal" -> "Mild Hypothermia"
            // Rule 2: Moderate Hypothermia
            (bodyTemperatureFuzzy == "Low" || bodyTemperatureFuzzy == "Extreme Low") && heartRateFuzzy == "Medium" -> "Moderate Hypothermia"
            // Rule 3: Severe Hypothermia
            (bodyTemperatureFuzzy == "Low" || bodyTemperatureFuzzy == "Extreme Low") && heartRateFuzzy == "Low" && oxygenSaturationFuzzy == "Low" -> "Severe Hypothermia"
            else -> "Normal"
        }
    }

    // Fungsi defuzzyfikasi
    private fun defuzzify(resultInference: String): String {
        return resultInference
    }

}