package com.rammonitor.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rammonitor.R
import com.rammonitor.data.LiveTrafficInfo
import com.rammonitor.data.NetworkUsageInfo
import com.rammonitor.databinding.FragmentNetworkBinding
import com.rammonitor.databinding.ItemLiveTrafficBinding
import com.rammonitor.databinding.ItemNetworkUsageBinding

class NetworkFragment : Fragment() {

    private var _binding: FragmentNetworkBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: MainViewModel

    private val networkAdapter = NetworkUsageAdapter()
    private val liveAdapter = LiveTrafficAdapter()

    private var showingLive = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = networkAdapter

        binding.recyclerLive.layoutManager = LinearLayoutManager(context)
        binding.recyclerLive.adapter = liveAdapter

        // Tab buttons
        binding.btnTabLive.setOnClickListener { switchTab(live = true) }
        binding.btnTab24h.setOnClickListener { switchTab(live = false) }

        binding.btnRefresh.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            vm.refreshNetworkUsage()
        }

        binding.btnGrantPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // 24h data
        vm.networkUsage.observe(viewLifecycleOwner) { list ->
            binding.progressBar.visibility = View.GONE
            networkAdapter.submit(list)
            binding.tvCount.text = getString(R.string.network_count, list.size)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        // Live traffic
        vm.liveTraffic.observe(viewLifecycleOwner) { list ->
            liveAdapter.submit(list)
            binding.tvLiveEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        // Global speeds
        vm.globalRxSpeed.observe(viewLifecycleOwner) { speed ->
            binding.tvGlobalRx.text = formatSpeed(speed)
        }
        vm.globalTxSpeed.observe(viewLifecycleOwner) { speed ->
            binding.tvGlobalTx.text = formatSpeed(speed)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
    }

    override fun onPause() {
        super.onPause()
        vm.stopLiveNetworkPolling()
    }

    private fun updatePermissionUI() {
        val granted = vm.hasUsageStatsPermission()
        binding.layoutPermission.visibility = if (granted) View.GONE else View.VISIBLE
        binding.layoutContent.visibility = if (granted) View.VISIBLE else View.GONE
        if (granted) {
            vm.startLiveNetworkPolling()
            if (!showingLive) {
                binding.progressBar.visibility = View.VISIBLE
                vm.refreshNetworkUsage()
            }
        }
    }

    private fun switchTab(live: Boolean) {
        showingLive = live
        binding.layoutLive.visibility = if (live) View.VISIBLE else View.GONE
        binding.layout24h.visibility = if (live) View.GONE else View.VISIBLE
        binding.btnTabLive.backgroundTintList =
            if (live) requireContext().getColorStateList(R.color.accent_cyan)
            else requireContext().getColorStateList(R.color.surface_dark)
        binding.btnTab24h.backgroundTintList =
            if (live) requireContext().getColorStateList(R.color.surface_dark)
            else requireContext().getColorStateList(R.color.accent_cyan)
        binding.btnTabLive.setTextColor(if (live) 0xFF000000.toInt() else 0xFF6B6B8A.toInt())
        binding.btnTab24h.setTextColor(if (live) 0xFF6B6B8A.toInt() else 0xFF000000.toInt())
        if (!live && vm.networkUsage.value == null) {
            binding.progressBar.visibility = View.VISIBLE
            vm.refreshNetworkUsage()
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String = when {
        bytesPerSec >= 1024 * 1024 -> getString(R.string.network_speed_mb_s, bytesPerSec / 1024f / 1024f)
        bytesPerSec >= 1024        -> getString(R.string.network_speed_kb_s, bytesPerSec / 1024f)
        else                       -> getString(R.string.network_speed_b_s, bytesPerSec)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── 24h Adapter ──────────────────────────────────────────────────────────────
class NetworkUsageAdapter : RecyclerView.Adapter<NetworkUsageAdapter.VH>() {
    private var items = listOf<NetworkUsageInfo>()

    fun submit(list: List<NetworkUsageInfo>) { items = list; notifyDataSetChanged() }

    inner class VH(val binding: ItemNetworkUsageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNetworkUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]; val b = holder.binding
        b.tvRank.text = "#${position + 1}"
        b.tvAppName.text = item.appName
        b.tvPackage.text = item.packageName
        b.tvTotal.text = b.root.context.getString(R.string.network_total_mb, item.totalMb)
        b.tvRx.text = b.root.context.getString(R.string.network_rx_mb, item.rxMb)
        b.tvTx.text = b.root.context.getString(R.string.network_tx_mb, item.txMb)
    }
}

// ── Live Traffic Adapter ──────────────────────────────────────────────────────
class LiveTrafficAdapter : RecyclerView.Adapter<LiveTrafficAdapter.VH>() {
    private var items = listOf<LiveTrafficInfo>()

    fun submit(list: List<LiveTrafficInfo>) { items = list; notifyDataSetChanged() }

    inner class VH(val binding: ItemLiveTrafficBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLiveTrafficBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]; val b = holder.binding
        val ctx = b.root.context
        b.tvAppName.text = item.appName
        b.tvPackage.text = item.packageName
        b.tvTotalSpeed.text = item.totalSpeedLabel
        b.tvRxSpeed.text = ctx.getString(R.string.network_rx_speed, item.rxSpeedLabel)
        b.tvTxSpeed.text = ctx.getString(R.string.network_tx_speed, item.txSpeedLabel)
    }
}
