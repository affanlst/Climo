package com.vergiawan.climo

import androidx.lifecycle.ViewModel

class StatusViewModel : ViewModel() {
    private var status: String? = null

    fun setStatus(setStatus: String) {
        status = setStatus
    }

    fun getStatus(): String {
        if (status != null) {
            return status as String
        } else {
            status = "Status null"
            return status as String
        }
    }
}