package com.vergiawan.climo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel : ViewModel() {
    private val _dataLat = MutableLiveData<Double>()
    private val _dataLng = MutableLiveData<Double>()

    val dataLat: LiveData<Double> get() = _dataLat
    val dataLng: LiveData<Double> get() = _dataLng

    fun setGeoPoint(lat: Double, lng: Double) {
        _dataLat.value = lat
        _dataLng.value = lng
    }

}