package com.rammonitor.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rammonitor.R
import com.rammonitor.data.AppMemInfo
import com.rammonitor.databinding.FragmentAppListBinding
import com.rammonitor.databinding.ItemAppMemBinding

class AppListFragment : Fragment() {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: MainViewModel
    private val adapter = AppMemAdapter()
    private var latestList: List<AppMemInfo> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        binding.switchKnownOnly.setOnCheckedChangeListener { _, _ ->
            renderList()
        }

        binding.btnRefresh.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            vm.refreshApps()
        }

        // Permission screen button → open Usage Access settings
        binding.btnGrantPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        vm.appList.observe(viewLifecycleOwner) { list ->
            binding.progressBar.visibility = View.GONE
            latestList = list
            renderList()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
    }

    private fun updatePermissionUI() {
        val granted = vm.hasUsageStatsPermission()
        binding.layoutPermission.visibility = if (granted) View.GONE else View.VISIBLE
        binding.layoutList.visibility = if (granted) View.VISIBLE else View.GONE
        if (granted) {
            binding.progressBar.visibility = View.VISIBLE
            vm.refreshApps()
        }
    }

    private fun renderList() {
        val knownOnly = binding.switchKnownOnly.isChecked
        val filtered = if (knownOnly) {
            latestList.filter { it.pssKb > 0 || it.rssKb > 0 }
        } else {
            latestList
        }

        val unknownCount = latestList.count { it.pssKb <= 0 && it.rssKb <= 0 }
        binding.tvInfoBanner.visibility = if (unknownCount > 0) View.VISIBLE else View.GONE
        binding.tvAppCount.text = getString(
            R.string.apps_count_with_unknown,
            filtered.size,
            unknownCount
        )
        adapter.submit(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class AppMemAdapter : RecyclerView.Adapter<AppMemAdapter.VH>() {
    private var items = listOf<AppMemInfo>()

    fun submit(list: List<AppMemInfo>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemAppMemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAppMemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val b = holder.binding
        val ctx = b.root.context
        b.tvAppName.text = item.appName
        b.tvPackage.text = item.packageName
        val memMb = item.pssMb.takeIf { it > 0f } ?: (item.rssKb / 1024f)
        b.tvPss.text = if (memMb > 0f) {
            ctx.getString(R.string.apps_ram_value, memMb)
        } else {
            ctx.getString(R.string.apps_unknown_ram)
        }
        b.tvUss.text = when {
            item.ussKb > 0 -> ctx.getString(R.string.apps_uss_value, item.ussKb / 1024f)
            memMb <= 0f -> ctx.getString(R.string.apps_unknown_ram_reason)
            else -> ""
        }

        val maxMem = items.firstOrNull()?.let {
            it.pssKb.coerceAtLeast(it.rssKb)
        } ?: 1
        val itemMem = item.pssKb.coerceAtLeast(item.rssKb)
        val progress = if (maxMem > 0) (itemMem * 100 / maxMem) else 0
        b.progressBar.progress = progress

        val color = when {
            memMb > 200f -> Color.parseColor("#F44336")
            memMb > 100f -> Color.parseColor("#FF9800")
            memMb > 0f  -> Color.parseColor("#4CAF50")
            else         -> Color.parseColor("#888888")
        }
        b.tvPss.setTextColor(color)
        b.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
        b.tvRank.text = "#${position + 1}"
    }
}
