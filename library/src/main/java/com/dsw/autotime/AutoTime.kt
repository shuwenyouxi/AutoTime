package com.dsw.autotime

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import java.io.IOException

/**
 * Created by Shuwen Dai on 2019-07-23
 * @author shuwenyouxi@gmail.cn
 */
object AutoTime {
    private val TAG = AutoTime::class.java.simpleName
    private const val DEFAULT_NTP_HOST = "ntp1.aliyun.com"

    private const val rootDelayMax = 100F
    private const val rootDispersionMax = 100F
    private const val serverResponseDelayMax = 750
    private const val udpSocketTimeoutInMillis = 30000

    private val client = SntpClient()
    private var connectionLiveData: ConnectionLiveData? = null

    /**
     * Returns the current time in milliseconds.
     * If it has synchronized server success, it will return true current server time, which is not affected by
     * device time. Otherwise, it will return device time [System.currentTimeMillis]
     */
    fun currentTimeMillis(): Long {
        val sntpTime = client.cachedSntpTime
        val deviceUptime = client.cachedDeviceUptime
        return if (sntpTime == 0L || deviceUptime == 0L) {
            System.currentTimeMillis()
        } else {
            sntpTime + SystemClock.elapsedRealtime() - deviceUptime
        }
    }

    @WorkerThread
    @JvmOverloads
    @Throws(IOException::class)
    fun initialize(ntpHost: String = DEFAULT_NTP_HOST) {
        if (client.wasInitialized()) {
            Log.i(TAG, "already initialized")
            return
        }
        requestTime(ntpHost)
    }


    @JvmOverloads
    @Throws(IOException::class)
    fun initializeRx(ntpHost: String = DEFAULT_NTP_HOST): Completable {
        return Completable.fromAction {
            initialize(ntpHost)
        }
    }

    @JvmOverloads
    fun easyInitialize(ntpHost: String = DEFAULT_NTP_HOST) {
        initializeRx(ntpHost)
            .subscribeOn(Schedulers.io())
            .subscribe({
                Log.i(TAG, "init success")
            }, { th ->
                Log.e(TAG, "init failed, msg=${th.message}")
            })
    }

    fun hasInitialized() = client.wasInitialized()

    fun subscribeNetworkChange(context: Application): AutoTime {
        if (connectionLiveData != null) {
            return this
        }
        connectionLiveData = ConnectionLiveData(context).apply {
            observeForever { connected ->
                Log.d(TAG, if (connected) "internet is connected" else "internet is disconnected")
                if (connected && !client.wasInitialized()) {
                    easyInitialize()
                }
            }
        }
        return this
    }

    @WorkerThread
    private fun requestTime(ntpHost: String) {
        client.requestTime(
            ntpHost,
            rootDelayMax,
            rootDispersionMax,
            serverResponseDelayMax,
            udpSocketTimeoutInMillis
        )
    }
}