package de.ph1b.audiobook.misc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


fun Context.flowBroadcastReceiver(filter: IntentFilter): Flow<Intent> {
  return callbackFlow {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent) {
        offer(intent)
      }
    }
    registerReceiver(receiver, filter)
    awaitClose {
      unregisterReceiver(receiver)
    }
  }
}
