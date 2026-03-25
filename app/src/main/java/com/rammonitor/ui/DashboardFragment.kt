package com.rammonitor.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.rammonitor.databinding.FragmentDashboardBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        setupLineChart()
        observeData()
    }

    private fun setupLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            legend.isEnabled = false
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.WHITE
                valueFormatter = object : ValueFormatter() {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    override fun getFormattedValue(value: Float) =
                        sdf.format(Date(value.toLong()))
                }
                labelCount = 4
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.argb(40, 255, 255, 255)
                textColor = Color.WHITE
                axisMinimum = 0f
                axisMaximum = 100f
            }
            axisRight.isEnabled = false
        }
    }

    private fun observeData() {
        vm.ramInfo.observe(viewLifecycleOwner) { info ->
            val pct = info.usagePercent
            binding.tvUsagePercent.text = "%.1f%%".format(pct)
            binding.tvUsedRam.text = "Kullanılan: %.0f MB / %.0f MB".format(info.usedMb, info.totalMb)
            binding.tvFreeRam.text = "Boş: %.0f MB".format(info.availableMb)

            // Color coding
            val color = when {
                pct < 60f -> Color.parseColor("#4CAF50")
                pct < 80f -> Color.parseColor("#FF9800")
                else -> Color.parseColor("#F44336")
            }
            binding.tvUsagePercent.setTextColor(color)
            binding.ramGauge.setProgress(pct.toInt(), true)
            binding.ramGauge.progressTintList =
                android.content.res.ColorStateList.valueOf(color)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.history.collect { history ->
                    if (history.isEmpty()) return@collect
                    val entries = history.takeLast(60).map { entry ->
                        Entry(entry.timestamp.toFloat(), entry.usagePercent)
                    }
                    val dataSet = LineDataSet(entries, "RAM %").apply {
                        color = Color.parseColor("#00E5FF")
                        setDrawCircles(false)
                        lineWidth = 2f
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                        setDrawFilled(true)
                        fillColor = Color.parseColor("#00E5FF")
                        fillAlpha = 40
                        valueTextSize = 0f
                    }
                    binding.lineChart.data = LineData(dataSet)
                    binding.lineChart.notifyDataSetChanged()
                    binding.lineChart.invalidate()
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
