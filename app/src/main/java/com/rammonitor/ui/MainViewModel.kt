package com.rammonitor.ui

import android.app.Application
import androidx.lifecycle.*
import com.rammonitor.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RamRepository(app)

    private val _ramInfo = MutableLiveData<RamInfo>()
    val ramInfo: LiveData<RamInfo> = _ramInfo

    private val _appList = MutableLiveData<List<AppMemInfo>>()
    val appList: LiveData<List<AppMemInfo>> = _appList

    private val _avgUsage = MutableLiveData<Float>()
    val avgUsage: LiveData<Float> = _avgUsage

    private val _peakUsage = MutableLiveData<Float>()
    val peakUsage: LiveData<Float> = _peakUsage

    private val _networkUsage = MutableLiveData<List<NetworkUsageInfo>>()
    val networkUsage: LiveData<List<NetworkUsageInfo>> = _networkUsage

    // ── Live traffic ──────────────────────────────────────────────────────────
    private val _liveTraffic = MutableLiveData<List<LiveTrafficInfo>>()
    val liveTraffic: LiveData<List<LiveTrafficInfo>> = _liveTraffic

    /** Global device download speed in bytes/sec */
    private val _globalRxSpeed = MutableLiveData<Long>(0L)
    val globalRxSpeed: LiveData<Long> = _globalRxSpeed

    /** Global device upload speed in bytes/sec */
    private val _globalTxSpeed = MutableLiveData<Long>(0L)
    val globalTxSpeed: LiveData<Long> = _globalTxSpeed

    private var livePollingJob: Job? = null

    fun startLiveNetworkPolling() {
        if (livePollingJob?.isActive == true) return
        livePollingJob = viewModelScope.launch(Dispatchers.IO) {
            // Fixed start time — both snapshots query from THIS point, so the delta
            // between consecutive calls = bytes transferred in exactly one poll interval.
            val pollingStart = System.currentTimeMillis()

            var prevNsm = repo.getNsmSnapshot(pollingStart)
            var prevGlobal = repo.getGlobalTrafficSnapshot()
            var prevTime = pollingStart

            while (isActive) {
                delay(3_000L)

                val nowNsm = repo.getNsmSnapshot(pollingStart)
                val nowGlobal = repo.getGlobalTrafficSnapshot()
                val nowTime = System.currentTimeMillis()
                val delta = nowTime - prevTime

                // ── Global speed (TrafficStats — no permission needed) ─────────
                val rxSpeed: Long
                val txSpeed: Long
                if (nowGlobal.first > 0L && prevGlobal.first > 0L) {
                    rxSpeed = ((nowGlobal.first - prevGlobal.first) * 1000L / delta).coerceAtLeast(0)
                    txSpeed = ((nowGlobal.second - prevGlobal.second) * 1000L / delta).coerceAtLeast(0)
                } else {
                    rxSpeed = 0L; txSpeed = 0L
                }
                _globalRxSpeed.postValue(rxSpeed)
                _globalTxSpeed.postValue(txSpeed)

                // ── Per-app delta from NSM fixed-start snapshots ───────────────
                val list = repo.computeLiveTrafficFromNsm(prevNsm, nowNsm, delta)
                _liveTraffic.postValue(list)

                // If TrafficStats was unsupported, derive global from NSM sum
                if (rxSpeed == 0L && txSpeed == 0L && list.isNotEmpty()) {
                    _globalRxSpeed.postValue(list.sumOf { it.rxBytesPerSec })
                    _globalTxSpeed.postValue(list.sumOf { it.txBytesPerSec })
                }

                prevNsm = nowNsm
                prevGlobal = nowGlobal
                prevTime = nowTime
            }
        }
    }

    fun stopLiveNetworkPolling() {
        livePollingJob?.cancel()
        livePollingJob = null
    }
    // ─────────────────────────────────────────────────────────────────────────

    val history = repo.getHistoryFlow().stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )

    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                refreshRam()
                delay(3000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    fun refreshRam() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = repo.getRamInfo()
            _ramInfo.postValue(info)
            repo.saveSnapshot(info)  // ← Save to history database
        }
    }

    fun hasUsageStatsPermission() = repo.hasUsageStatsPermission()

    fun refreshApps() {
        viewModelScope.launch {
            val list = repo.getAppMemInfoList()
            _appList.postValue(list)
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            _avgUsage.postValue(repo.getAverageUsageLast1h())
            _peakUsage.postValue(repo.getPeakUsageLast1h())
        }
    }

    fun refreshNetworkUsage() {
        viewModelScope.launch {
            val list = repo.getNetworkUsageList()
            _networkUsage.postValue(list)
        }
    }
}
