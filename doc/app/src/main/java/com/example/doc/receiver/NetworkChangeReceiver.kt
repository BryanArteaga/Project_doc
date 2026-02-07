package com.example.doc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.doc.utils.NetworkMonitor
import timber.log.Timber

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            Timber.d("Cambio de conectividad detectado")
            NetworkMonitor.updateConnectivity(context)
        }
    }
}

