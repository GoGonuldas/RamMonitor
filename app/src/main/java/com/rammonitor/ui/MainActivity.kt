package com.rammonitor.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.rammonitor.R
import com.rammonitor.databinding.ActivityMainBinding
import com.rammonitor.service.RamMonitorService
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MainViewModel

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — proceed anyway */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdgeInsets()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setupTabs()
        startMonitorService()
        viewModel.startPolling()
        promptUsageStatsIfNeeded()
    }

    private fun setupEdgeToEdgeInsets() {
        val initialLeft = binding.root.paddingLeft
        val initialTop = binding.root.paddingTop
        val initialRight = binding.root.paddingRight
        val initialBottom = binding.root.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = initialLeft + systemBars.left,
                top = initialTop + systemBars.top,
                right = initialRight + systemBars.right,
                bottom = initialBottom + systemBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun promptUsageStatsIfNeeded() {
        if (!viewModel.hasUsageStatsPermission()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.usage_dialog_title))
                .setMessage(getString(R.string.usage_dialog_message))
                .setPositiveButton(getString(R.string.usage_dialog_open_settings)) { _, _ ->
                    startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    )
                }
                .setNegativeButton(getString(R.string.usage_dialog_later), null)
                .show()
        }
    }

    private fun setupTabs() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 4
            override fun createFragment(position: Int) = when (position) {
                0 -> DashboardFragment()
                1 -> AppListFragment()
                2 -> HistoryFragment()
                else -> NetworkFragment()
            }
        }
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = when (pos) {
                0 -> getString(R.string.tab_dashboard_icon)
                1 -> getString(R.string.tab_apps_icon)
                2 -> getString(R.string.tab_history_icon)
                else -> getString(R.string.tab_network_icon)
            }
        }.attach()
    }

    private fun startMonitorService() {
        val intent = Intent(this, RamMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }
}
