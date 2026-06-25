package com.poids.tracking

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.poids.MainActivity
import com.poids.R
import com.poids.domain.calc.MetCalculator
import com.poids.domain.model.ActivitySession
import com.poids.domain.model.ActivitySource
import com.poids.domain.model.ActivityType
import com.poids.domain.model.LiveActivityStats
import com.poids.domain.model.RoutePoint
import com.poids.data.tracking.TrackingStateHolder
import com.poids.domain.repository.ActivityRepository
import com.poids.domain.repository.ProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Service de premier plan (type location) qui suit une session GPS via le
 * LocationManager AOSP (sans Google Play Services). Accumule distance/vitesse/
 * dénivelé, publie les stats en direct via [TrackingStateHolder], et enregistre
 * la session à l'arrêt.
 */
@AndroidEntryPoint
class ActivityTrackingService : Service() {

    @Inject lateinit var activityRepository: ActivityRepository
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var stateHolder: TrackingStateHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    private val locationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private var type: ActivityType = ActivityType.WALKING
    private var startTime: Instant? = null
    private var lastLocation: Location? = null
    private var distanceM = 0.0
    private var lastAltitude: Double? = null
    private var elevationGainM = 0.0
    private var currentSpeedMps = 0.0
    private val points = mutableListOf<RoutePoint>()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onNewLocation(location)
        // Surcharges abstraites sur API 26-29.
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(
                ActivityType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: ActivityType.WALKING.name),
            )
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun start(activityType: ActivityType) {
        type = activityType
        startTime = Instant.now()
        lastLocation = null
        distanceM = 0.0
        lastAltitude = null
        elevationGainM = 0.0
        currentSpeedMps = 0.0
        points.clear()

        startForegroundCompat()

        runCatching {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                locationListener,
                Looper.getMainLooper(),
            )
        }

        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                pushStats()
                delay(1000)
            }
        }
    }

    private fun onNewLocation(location: Location) {
        lastLocation?.let { prev ->
            if (location.accuracy <= 50f) distanceM += prev.distanceTo(location)
        }
        if (location.hasSpeed()) currentSpeedMps = location.speed.toDouble()
        if (location.hasAltitude()) {
            val alt = location.altitude
            val ref = lastAltitude
            when {
                ref == null -> lastAltitude = alt
                alt - ref > ELEVATION_THRESHOLD_M -> {
                    elevationGainM += alt - ref
                    lastAltitude = alt
                }
                ref - alt > ELEVATION_THRESHOLD_M -> lastAltitude = alt
            }
        }
        points.add(
            RoutePoint(
                lat = location.latitude,
                lon = location.longitude,
                altitudeM = if (location.hasAltitude()) location.altitude else null,
                speedMps = if (location.hasSpeed()) location.speed.toDouble() else null,
                timestamp = Instant.now(),
            ),
        )
        lastLocation = location
    }

    private fun pushStats() {
        val start = startTime ?: return
        stateHolder.update(
            LiveActivityStats(
                type = type,
                startTime = start,
                durationSec = Duration.between(start, Instant.now()).seconds,
                distanceM = distanceM,
                currentSpeedMps = currentSpeedMps,
                elevationGainM = elevationGainM,
            ),
        )
    }

    private fun stop() {
        locationManager.removeUpdates(locationListener)
        tickerJob?.cancel()
        val start = startTime
        if (start != null) saveSession(start)
        stateHolder.update(null)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun saveSession(start: Instant) {
        val end = Instant.now()
        val durationSec = Duration.between(start, end).seconds
        val avgSpeed = if (durationSec > 0) distanceM / durationSec else 0.0
        val capturedPoints = points.toList()
        val capturedDistance = distanceM
        val capturedElevation = elevationGainM
        val capturedType = type

        scope.launch {
            val weightKg = profileRepository.getProfile()?.weightKg ?: 70.0
            val met = metFor(capturedType, avgSpeed)
            val kcal = MetCalculator.caloriesBurned(met, weightKg, durationSec, net = true)
            val session = ActivitySession(
                type = capturedType,
                source = ActivitySource.GPS,
                startTime = start,
                endTime = end,
                durationSec = durationSec,
                distanceM = capturedDistance,
                avgSpeedMps = avgSpeed,
                elevationGainM = capturedElevation,
                caloriesKcal = kcal,
            )
            activityRepository.saveSession(session, capturedPoints)
        }
    }

    private fun metFor(type: ActivityType, avgSpeedMps: Double): Double {
        val kmh = avgSpeedMps * 3.6
        return when (type) {
            ActivityType.WALKING, ActivityType.WALKING_BRISK ->
                MetCalculator.metForSpeed(kmh, running = false)
            ActivityType.RUNNING -> MetCalculator.metForSpeed(kmh, running = true)
            else -> type.defaultMet
        }
    }

    private fun startForegroundCompat() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Suivi d'activité",
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Suivi d'activité en cours")
            .setContentText("Enregistrement de votre parcours GPS")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, notification, fgsType)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val ACTION_START = "com.poids.tracking.START"
        const val ACTION_STOP = "com.poids.tracking.STOP"
        const val EXTRA_TYPE = "type"
        private const val CHANNEL_ID = "activity_tracking"
        private const val NOTIF_ID = 1001
        private const val ELEVATION_THRESHOLD_M = 1.5

        fun startIntent(context: Context, type: ActivityType): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TYPE, type.name)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, ActivityTrackingService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
