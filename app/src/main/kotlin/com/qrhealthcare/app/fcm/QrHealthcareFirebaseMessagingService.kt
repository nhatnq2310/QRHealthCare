package com.qrhealthcare.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives push notifications sent by the backend when:
 *  - someone scans a profile's QR code (type = "qr_scan"), or
 *  - a scanner shares their location after opting in (type = "qr_scan_location")
 * ...for profiles this device has registered as a "family notification"
 * device for (see FamilyNotifyScreen / AppRepository.registerFamilyDevice).
 *
 * This class only receives messages once Firebase is actually initialized,
 * which requires google-services.json + the google-services Gradle plugin —
 * see app/FCM_SETUP.md. Without that setup, this service is simply never
 * invoked (no crash, just no push notifications).
 */
class QrHealthcareFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "family_scan_alerts"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // The app re-registers the fresh token the next time the user opens
        // FamilyNotifyScreen for a given profile — tokens can rotate, and we
        // don't have a stable per-profile background sync channel here.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "QR Healthcare"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val linkUrl = message.data["fullViewUrl"] ?: message.data["mapsUrl"]

        ensureChannel()

        val intent = if (linkUrl != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else null

        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                this, System.currentTimeMillis().toInt(), it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // swap for a real app icon/drawable
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .apply { pendingIntent?.let { setContentIntent(it) } }
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Thông báo quét QR người thân",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Thông báo khi có người quét mã QR hồ sơ của người thân bạn" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
