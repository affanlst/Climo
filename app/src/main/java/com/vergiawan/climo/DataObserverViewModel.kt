package com.vergiawan.climo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DataObserverViewModel : ViewModel() {
    private val _objectTemp = MutableLiveData<Double>()
    // val objectTemp: LiveData<Double> get() = _objectTemp
    var dataChange: Boolean? = true

    fun setObjectTemp(temp: Double) {
        if (temp != _objectTemp.value) {
            _objectTemp.value = temp
            dataChange = true
        } else {
            dataChange = false
        }
    }
}