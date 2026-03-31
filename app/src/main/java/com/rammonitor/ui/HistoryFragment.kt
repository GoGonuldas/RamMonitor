package com.rammonitor.ui

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.rammonitor.R
import com.rammonitor.databinding.FragmentHistoryBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        setupBarChart()
        observeData()
        vm.refreshStats()
    }

    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.WHITE
                labelRotationAngle = -45f
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) =
                        sdf.format(Date(value.toLong()))
                }
            }
            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
                axisMaximum = 100f
                gridColor = Color.argb(40, 255, 255, 255)
            }
            axisRight.isEnabled = false
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.history.collect { history ->
                    if (history.isEmpty()) return@collect

                    val entries = history.takeLast(40).map { entry ->
                        BarEntry(entry.timestamp.toFloat(), entry.usagePercent)
                    }
                    val colors = entries.map { entry ->
                        when {
                            entry.y < 60f -> Color.parseColor("#4CAF50")
                            entry.y < 80f -> Color.parseColor("#FF9800")
                            else -> Color.parseColor("#F44336")
                        }
                    }
                    val dataSet = BarDataSet(entries, getString(R.string.chart_ram_percent_label)).apply {
                        setColors(*colors.toIntArray())
                        valueTextColor = Color.TRANSPARENT
                        valueTextSize = 0f
                    }
                    binding.barChart.data = BarData(dataSet).apply { barWidth = 0.8f }
                    binding.barChart.notifyDataSetChanged()
                    binding.barChart.invalidate()

                    // Summary stats
                    val usages = history.map { it.usagePercent }
                    binding.tvMin.text = getString(R.string.history_min_value, usages.minOrNull() ?: 0f)
                    binding.tvMax.text = getString(R.string.history_max_value, usages.maxOrNull() ?: 0f)
                    binding.tvAvg.text = getString(R.string.history_avg_value, usages.average().toFloat())
                    binding.tvSamples.text = getString(R.string.history_samples_count, history.size)
                }
            }
        }

        vm.avgUsage.observe(viewLifecycleOwner) { avg ->
            binding.tvAvgHour.text = getString(R.string.history_last_hour_avg, avg)
        }

        vm.peakUsage.observe(viewLifecycleOwner) { peak ->
            binding.tvPeakHour.text = getString(R.string.history_last_hour_peak, peak)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
