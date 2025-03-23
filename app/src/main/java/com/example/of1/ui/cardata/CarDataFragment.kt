package com.example.of1.ui.cardata

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.of1.data.model.openf1.CarData
import com.example.of1.databinding.FragmentCarDataBinding
import com.example.of1.utils.Resource
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class CarDataFragment : Fragment() {

    private lateinit var binding: FragmentCarDataBinding
    private val viewModel: CarDataViewModel by viewModels()
    private val args: CarDataFragmentArgs by navArgs()

    // Charts
    private lateinit var throttleBrakeChart: CombinedChart
    private lateinit var rpmSpeedChart: CombinedChart
    private lateinit var combinedChart: CombinedChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCarDataBinding.inflate(inflater, container, false)

        // Initialize charts
        throttleBrakeChart = binding.chartThrottleBrake
        rpmSpeedChart = binding.chartRpmSpeed
        combinedChart = binding.chartCombined

        // Basic chart setup (common to all charts)
        setupChart(throttleBrakeChart)
        setupChart(rpmSpeedChart)
        setupChart(combinedChart)

        return binding.root
    }
    override fun onPause() {
        super.onPause()
        viewModel.stopPolling() // Stop polling when the fragment is paused/invisible
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopPolling()  // VERY IMPORTANT: Stop polling in onDestroyView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val driverNumber = args.driverNumber
        val sessionKey = args.sessionKey
        val isLive = args.isLive

        observeCarData()
        observeThrottleBrakeChartData()
        observeRpmSpeedChartData()
        observeCombinedChartData()

        viewModel.getCarData(sessionKey, driverNumber, isLive) // Start fetching data
    }

    private fun observeCarData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.carData.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // Show loading indicator (optional, since updates are frequent)
                            binding.progressBar.visibility = if (resource.isLoading) View.VISIBLE else View.GONE
                            Log.d("CarDataFragment", "Loading Car Data...")
                        }
                        is Resource.Success -> {
                            // Update TextViews with the *latest* data
                            val latestData = resource.data?.lastOrNull() // Most recent entry
                            if (latestData != null) {
                                updateUi(latestData)
                            }
                            Log.d("CarDataFragment", "Success: ${resource.data?.size} car data entries")
                        }
                        is Resource.Error -> {
                            // Handle error
                            Toast.makeText(context, resource.message ?: "An error occurred", Toast.LENGTH_LONG).show()
                            Log.e("CarDataFragment", "Error: ${resource.message}")
                        }
                    }
                }
            }
        }
    }
    private fun updateUi(carData: CarData){
        binding.tvThrottle.text = "Throttle: ${carData.throttle ?: "N/A"}"
        binding.tvBrake.text = "Brake: ${carData.brake ?: "N/A"}"
        binding.tvDrs.text = "DRS: ${carData.drs ?: "N/A"}"
        binding.tvGear.text = "Gear: ${carData.nGear ?: "N/A"}"
        binding.tvRpm.text = "RPM: ${carData.rpm ?: "N/A"}"
        binding.tvSpeed.text = "Speed: ${carData.speed ?: "N/A"}"
    }

    private fun observeThrottleBrakeChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.getThrottleBrakeData().collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // Optional: Show loading indicator specifically for the chart
                        }
                        is Resource.Success -> {
                            val data = resource.data ?: emptyList()
                            updateThrottleBrakeChart(data)
                        }
                        is Resource.Error -> {
                            // Handle chart data error
                        }
                    }
                }
            }
        }
    }

    private fun observeRpmSpeedChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getRpmSpeedData().collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {}
                        is Resource.Success -> {
                            val data = resource.data ?: emptyList()
                            updateRpmSpeedChart(data)
                        }
                        is Resource.Error -> {}
                    }
                }
            }
        }
    }

    private fun observeCombinedChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.getCombinedData().collectLatest { resource ->
                    when(resource){
                        is Resource.Loading -> {}
                        is Resource.Success -> {
                            val data = resource.data ?: emptyList()
                            updateCombinedChart(data)
                        }
                        is Resource.Error -> {}
                    }
                }
            }
        }
    }


    private fun setupChart(chart: CombinedChart) {
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.isHighlightPerDragEnabled = false
        chart.isHighlightPerTapEnabled = false

        // Configure X-axis (Time)
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            private val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault()) // Format as you like

            override fun getFormattedValue(value: Float): String {
                // Assuming 'value' is the timestamp in milliseconds
                return format.format(value.toLong())
            }
        }
    }

    private fun updateThrottleBrakeChart(data: List<Pair<String, Pair<Int?, Int?>>>) {
        val throttleEntries = ArrayList<Entry>()
        val brakeEntries = ArrayList<Entry>()

        for (item in data) {
            val time = parseDateToMillis(item.first).toFloat()  // Convert date string to milliseconds
            item.second.first?.let { throttle ->
                throttleEntries.add(Entry(time, throttle.toFloat()))
            }
            item.second.second?.let { brake ->
                brakeEntries.add(Entry(time, brake.toFloat()))
            }
        }

        val throttleDataSet = LineDataSet(throttleEntries, "Throttle")
        throttleDataSet.color = Color.BLUE // Example color
        throttleDataSet.setDrawCircles(false)

        val brakeDataSet = LineDataSet(brakeEntries, "Brake")
        brakeDataSet.color = Color.RED // Example color
        brakeDataSet.setDrawCircles(false)


        val lineData = LineData(throttleDataSet, brakeDataSet)
        val combinedData = CombinedData()
        combinedData.setData(lineData)

        throttleBrakeChart.data = combinedData
        throttleBrakeChart.invalidate() // Refresh the chart
    }

    private fun updateRpmSpeedChart(data: List<Pair<String, Pair<Int?, Int?>>>) {
        val rpmEntries = ArrayList<Entry>()
        val speedEntries = ArrayList<Entry>()

        for (item in data) {
            val time = parseDateToMillis(item.first).toFloat()
            item.second.first?.let { rpm ->
                rpmEntries.add(Entry(time, rpm.toFloat()))
            }
            item.second.second?.let { speed ->
                speedEntries.add(Entry(time, speed.toFloat()))
            }
        }

        val rpmDataSet = LineDataSet(rpmEntries, "RPM")
        rpmDataSet.color = Color.GREEN
        rpmDataSet.setDrawCircles(false)

        val speedDataSet = LineDataSet(speedEntries, "Speed")
        speedDataSet.color = Color.MAGENTA
        speedDataSet.setDrawCircles(false)

        val lineData = LineData(rpmDataSet, speedDataSet)
        val combinedData = CombinedData()
        combinedData.setData(lineData)
        rpmSpeedChart.data = combinedData
        rpmSpeedChart.invalidate()
    }


    private fun updateCombinedChart(data: List<Pair<String, Triple<Int?, Int?, Int?>>>) {
        val throttleEntries = ArrayList<Entry>()
        val rpmEntries = ArrayList<Entry>()
        val speedEntries = ArrayList<Entry>()

        for (item in data) {
            val time = parseDateToMillis(item.first).toFloat()
            item.second.first?.let { throttle ->
                throttleEntries.add(Entry(time, throttle.toFloat()))
            }
            item.second.second?.let { rpm ->
                rpmEntries.add(Entry(time, rpm.toFloat()))
            }
            item.second.third?.let { speed ->
                speedEntries.add(Entry(time, speed.toFloat()))
            }
        }

        val throttleDataSet = LineDataSet(throttleEntries, "Throttle")
        throttleDataSet.color = Color.BLUE
        throttleDataSet.setDrawCircles(false)

        val rpmDataSet = LineDataSet(rpmEntries, "RPM")
        rpmDataSet.color = Color.GREEN
        rpmDataSet.setDrawCircles(false)

        val speedDataSet = LineDataSet(speedEntries, "Speed")
        speedDataSet.color = Color.MAGENTA
        speedDataSet.setDrawCircles(false)

        val lineData = LineData(throttleDataSet, rpmDataSet, speedDataSet)
        val combinedData = CombinedData()
        combinedData.setData(lineData)
        combinedChart.data = combinedData
        combinedChart.invalidate()

    }

    // Helper function to parse the date string to milliseconds
    private fun parseDateToMillis(dateString: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'000+00:00'", Locale.getDefault())
        return try {
            format.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            Log.e("CarDataFragment", "Error parsing date: $dateString", e)
            0L
        }
    }
}