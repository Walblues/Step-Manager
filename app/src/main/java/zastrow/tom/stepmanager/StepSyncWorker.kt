package zastrow.tom.stepmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.LocalDate
import java.time.LocalDateTime

class StepSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("StepManager", "StepSyncWorker started")

        return try {
            val healthDataStore =
                HealthDataService.getStore(applicationContext)

            val requiredPermissions = setOf(
                Permission.of(DataTypes.STEPS, AccessType.READ)
            )

            val grantedPermissions =
                healthDataStore.getGrantedPermissions(requiredPermissions)

            if (!grantedPermissions.containsAll(requiredPermissions)) {
                Log.e("StepManager", "Samsung Health step permission is missing")

                showPermissionMissingNotification()

                return Result.success() // Keep periodic work scheduled for next execution
            }

            // Get today's aggregated step total.
            val startTime = LocalDate.now().atStartOfDay()
            val endTime = LocalDateTime.now()

            val filter = LocalTimeFilter.of(startTime, endTime)

            val request = DataType.StepsType.TOTAL.requestBuilder
                .setLocalTimeFilter(filter)
                .build()

            val result = healthDataStore.aggregateData(request)

            val steps = result.dataList.firstOrNull()?.value ?: 0L

            Log.d("StepManager", "Background step count: $steps")

            // Send the step count to MacroDroid.
            val intent = Intent("zastrow.tom.STEPS_UPDATED").apply {
                putExtra("step_count", steps.toInt())
            }

            applicationContext.sendBroadcast(intent)

            Log.d("StepManager", "Step broadcast sent to MacroDroid")
            Result.success()
        } catch (e: Exception) {
            Log.e("StepManager", "Background step sync failed", e)
            Result.retry()
        }
    }

    private fun showPermissionMissingNotification() {
        val channelId = "step_sync_alerts"

        // Only works in Android 8+
        val channel = NotificationChannel(
            channelId,
            "Step Sync Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts about Samsung Health permissions"
        }

        val manager = applicationContext.getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(channel)

        // Open MainActivity when the notification is tapped.
        val openAppIntent = Intent(
            applicationContext,
            MainActivity::class.java
        )

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Check whether Android allows notifications.
        if (ContextCompat.checkSelfPermission(
            applicationContext, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED)
        {
            Log.e("StepManager", "Cannot notify: notification permission missing")
            return
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Step Sync Needs Permission")
            .setContentText(
                "Samsung Health permission was revoked. Tap to restore step syncing."
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Step Manager cannot retrieve your steps because Samsung Health permission is missing. Open the app to grant permission again."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(1001, notification)

        Log.d("StepManager", "Permission notification sent")

    }
}