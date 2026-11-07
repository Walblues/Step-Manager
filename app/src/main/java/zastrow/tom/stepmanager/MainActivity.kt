package zastrow.tom.stepmanager

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var healthDataStore: HealthDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        healthDataStore = HealthDataService.getStore(applicationContext)
        lifecycleScope.launch {
            try {
                checkHealthPermissions()
                scheduleStepSync()
            } catch (e: Exception) {
                Log.e("StepManager", "Step retrieval failed", e)
            }
        }
    }

    private suspend fun checkHealthPermissions() {
        val requiredPermissions = setOf(
            Permission.of(DataTypes.STEPS, AccessType.READ)
        )

        val grantedPermissions =
            healthDataStore.getGrantedPermissions(requiredPermissions)

        if (!grantedPermissions.containsAll(requiredPermissions)) {
            val requestedPermissions = healthDataStore.requestPermissions(
                requiredPermissions,
                this@MainActivity
            )

            if (!requestedPermissions.containsAll(requiredPermissions)) {
                Log.e("StepManager", "Step permission was not granted")
                throw SecurityException("Samsung Health step permission denied")
            }
        }

        Log.d("StepManager", "Samsung Health step permission granted")
    }

    private fun scheduleStepSync() {
        val repeatInterval = 15L
        val workRequest =
            PeriodicWorkRequestBuilder<StepSyncWorker>(
                repeatInterval, TimeUnit.MINUTES
            ).build()

        // Starts repeating task (unless an instance is already running)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "step_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        Log.d("StepManager", "$repeatInterval-minute step sync scheduled")
    }
}