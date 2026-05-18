package com.rammonitor.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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

        setupTabs()

        if (hasPrivacyConsent()) {
            initAfterConsent()
        } else {
            showPrivacyConsentDialog()
        }
    }

    private fun prefs() = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun hasPrivacyConsent(): Boolean =
        prefs().getBoolean(KEY_PRIVACY_ACCEPTED, false)

    private fun setPrivacyAccepted() {
        prefs().edit().putBoolean(KEY_PRIVACY_ACCEPTED, true).apply()
    }

    private fun showPrivacyConsentDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.privacy_dialog_title))
            .setMessage(getString(R.string.privacy_dialog_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.privacy_dialog_agree)) { dialog, _ ->
                setPrivacyAccepted()
                dialog.dismiss()
                initAfterConsent()
            }
            .setNegativeButton(getString(R.string.privacy_dialog_disagree)) { _, _ ->
                finishAffinity()
            }
            .setNeutralButton(getString(R.string.privacy_dialog_view)) { _, _ ->
                openPrivacyPolicy()
                // Re-show dialog after returning
                showPrivacyConsentDialog()
            }
            .show()
    }

    private fun openPrivacyPolicy() {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)))
            )
        } catch (_: Exception) { /* ignore */ }
    }

    private fun initAfterConsent() {
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Request explicit user consent for background monitoring service
        promptBackgroundMonitoringConsent()
    }

    private fun promptBackgroundMonitoringConsent() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.monitoring_dialog_title))
            .setMessage(getString(R.string.monitoring_dialog_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.monitoring_dialog_enable)) { dialog, _ ->
                dialog.dismiss()
                startMonitorService()
                viewModel.startPolling()
                promptUsageStatsIfNeeded()
            }
            .setNegativeButton(getString(R.string.monitoring_dialog_disable)) { _, _ ->
                // User can still use the app, but without background monitoring
                viewModel.startPolling()
                promptUsageStatsIfNeeded()
            }
            .show()
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
        if (hasPrivacyConsent()) viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }

    companion object {
        private const val PREFS_NAME = "ram_monitor_prefs"
        private const val KEY_PRIVACY_ACCEPTED = "privacy_accepted"
    }
}
