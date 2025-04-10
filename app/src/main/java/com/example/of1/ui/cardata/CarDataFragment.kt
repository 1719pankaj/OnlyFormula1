package com.example.of1.ui.cardata

import android.annotation.SuppressLint
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
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    override fun onResume() {
        super.onResume()
        if(args.isLive){
            viewModel.startPolling()
        }
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
        val startDate = args.startDate // Get startDate
        val endDate = args.endDate     // Get endDate

        observeCarData()
        observeThrottleBrakeChartData()
        observeRpmSpeedChartData()
        observeCombinedChartData()

        //Modified
        viewModel.getCarData(sessionKey, driverNumber, startDate, endDate, isLive)
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
    @SuppressLint("SetTextI18n")
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
                viewModel.throttleBrakeData.collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            Log.d("CarDataFragment","Loading Throttle/Brake Data")
                        }
                        is Resource.Success -> {
                            Log.d("CarDataFragment", "Throttle/Brake Chart Success: ${resource.data?.size} entries")
                            updateThrottleBrakeChart(resource.data)
                        }
                        is Resource.Error -> {
                            Log.d("CarDataFragment","Error getting Throttle/Brake data")
                        }

                    }
                }
            }
        }
    }

    private fun observeRpmSpeedChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rpmSpeedData.collectLatest { resource ->
                    when(resource){
                        is Resource.Loading -> {
                            Log.d("CarDataFragment", "Loading RPM/Speed Data")
                        }
                        is Resource.Success -> {
                            Log.d("CarDataFragment", "RPM/Speed Chart Success: ${resource.data?.size} entries")
                            updateRpmSpeedChart(resource.data)
                        }
                        is Resource.Error -> {
                            Log.d("CarDataFragment", "Error getting RPM/Speed Data")
                        }
                    }
                }
            }
        }
    }

    private fun observeCombinedChartData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.combinedData.collectLatest { resource ->
                    when(resource){
                        is Resource.Loading -> {
                            Log.d("CarDataFragment", "Loading combined data")
                        }
                        is Resource.Success -> {
                            Log.d("CarDataFragment", "Combined Chart Success: ${resource.data?.size} entries")
                            updateCombinedChart(resource.data)
                        }
                        is Resource.Error -> {
                            Log.d("CarDataFragment", "Error getting combined data")
                        }
                    }
                }
            }
        }
    }

    private fun setupChart(chart: CombinedChart) {
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)

        // ** KEY CHART CONFIGURATION CHANGES **
        chart.isHighlightPerDragEnabled = false
        chart.isHighlightPerTapEnabled = false
        chart.setAutoScaleMinMaxEnabled(true) // ** IMPORTANT: Auto-scale axes
        chart.isDragEnabled = true // Allow the user to drag the chart
        chart.setScaleEnabled(true) //Allow the user to scale the chart.
        chart.setPinchZoom(true) // Allow the user to zoom in and out with pinch gesture.
        chart.setVisibleXRangeMaximum(60000f) // Show 60 seconds worth of data at a time.
        // Configure X-axis (Time)
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            private val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            init {
                format.timeZone = TimeZone.getTimeZone("UTC") //SET TIMEZONE
            }

            override fun getFormattedValue(value: Float): String {
                return format.format(value.toLong()) // Format as HH:mm:ss

            }
        }

        // Configure Y-axes (Left and Right - if needed)
        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 0f

        val rightAxis = chart.axisRight
        rightAxis.axisMinimum = 0f
        rightAxis.isEnabled = false // Disable by default

        chart.invalidate()
    }
    private fun updateThrottleBrakeChart(data: List<Pair<Float, Pair<Float?, Float?>>>?) {
        Log.d("CarDataFragment", "updateThrottleBrakeChart called with ${data?.size ?: 0} entries")
        val throttleEntries = ArrayList<Entry>()
        val brakeEntries = ArrayList<Entry>()

        if (data != null) {
            for (item in data) {
                val time = item.first
                val throttle = item.second.first
                val brake = item.second.second

                if (throttle != null && brake != null) {
                    throttleEntries.add(Entry(time, throttle))
                    brakeEntries.add(Entry(time, brake))
                }
            }
        }

        val throttleDataSet = LineDataSet(throttleEntries, "Throttle")
        throttleDataSet.color = Color.BLUE
        throttleDataSet.setDrawCircles(false)
        throttleDataSet.axisDependency = YAxis.AxisDependency.LEFT

        val brakeDataSet = LineDataSet(brakeEntries, "Brake")
        brakeDataSet.color = Color.RED
        brakeDataSet.setDrawCircles(false)
        brakeDataSet.axisDependency = YAxis.AxisDependency.LEFT

        val lineData = LineData(throttleDataSet, brakeDataSet)
        val combinedData = CombinedData()
        combinedData.setData(lineData)

        throttleBrakeChart.data = combinedData
        throttleBrakeChart.axisLeft.axisMaximum = 110f  // Max throttle/brake
        throttleBrakeChart.axisRight.isEnabled = false
        throttleBrakeChart.invalidate()
        throttleBrakeChart.fitScreen() // Reset zoom/pan after setting new data
        Log.d("CarDataFragment", "throttleBrakeChart.data set and invalidated")
    }



    private fun updateRpmSpeedChart(data: List<Pair<Float, Pair<Float?, Float?>>>?) {
        Log.d("CarDataFragment", "updateRpmSpeedChart called with ${data?.size ?: 0} entries")
        val rpmEntries = ArrayList<Entry>()
        val speedEntries = ArrayList<Entry>()

        if (data != null){
            for (item in data) {
                val time = item.first
                val rpm = item.second.first
                val speed = item.second.second
                if(rpm != null && speed != null){
                    rpmEntries.add(Entry(time, rpm))
                    speedEntries.add(Entry(time, speed))
                }
            }
        }

        val rpmDataSet = LineDataSet(rpmEntries, "RPM")
        rpmDataSet.color = Color.GREEN
        rpmDataSet.setDrawCircles(false)
        rpmDataSet.axisDependency = YAxis.AxisDependency.LEFT

        val speedDataSet = LineDataSet(speedEntries, "Speed")
        speedDataSet.color = Color.MAGENTA
        speedDataSet.setDrawCircles(false)
        speedDataSet.axisDependency = YAxis.AxisDependency.RIGHT

        val lineData = LineData(rpmDataSet, speedDataSet)
        val combinedData = CombinedData()
        combinedData.setData(lineData)

        rpmSpeedChart.data = combinedData
        rpmSpeedChart.axisLeft.axisMaximum = 20000f  // Max RPM
        rpmSpeedChart.axisRight.isEnabled = true
        rpmSpeedChart.axisRight.axisMinimum = 0f // Min speed
        rpmSpeedChart.axisRight.axisMaximum = 400f // Max speed
        rpmSpeedChart.invalidate()
        rpmSpeedChart.fitScreen() // Reset zoom/pan
        Log.d("CarDataFragment", "rpmSpeedChart.data set and invalidated")
    }




    private fun updateCombinedChart(data: List<Pair<Float, Triple<Float?, Float?, Float?>>>?) {
        Log.d("CarDataFragment", "updateCombinedChart called with ${data?.size ?: 0} entries")

        val throttleEntries = ArrayList<Entry>()
        val rpmEntries = ArrayList<Entry>()
        val speedEntries = ArrayList<Entry>()

        if(data != null){
            for (item in data) {
                val time = item.first
                val throttle = item.second.first
                val rpm = item.second.second
                val speed = item.second.third

                if(throttle != null && rpm != null && speed != null){
                    throttleEntries.add(Entry(time, throttle))
                    rpmEntries.add(Entry(time, rpm))
                    speedEntries.add(Entry(time, speed))
                }
            }
        }

        val throttleDataSet = LineDataSet(throttleEntries, "Throttle")
        throttleDataSet.color = Color.BLUE
        throttleDataSet.setDrawCircles(false)
        throttleDataSet.axisDependency = YAxis.AxisDependency.LEFT // Throttle on left

        val rpmDataSet = LineDataSet(rpmEntries, "RPM")
        rpmDataSet.color = Color.GREEN
        rpmDataSet.setDrawCircles(false)
        rpmDataSet.axisDependency = YAxis.AxisDependency.RIGHT // RPM on right

        val speedDataSet = LineDataSet(speedEntries, "Speed")
        speedDataSet.color = Color.MAGENTA
        speedDataSet.setDrawCircles(false)
        speedDataSet.axisDependency = YAxis.AxisDependency.RIGHT // Speed on right

        val lineData = LineData(throttleDataSet, rpmDataSet, speedDataSet)
        val combinedData = CombinedData()
        combinedData.setData(lineData)
        combinedChart.data = combinedData

        combinedChart.axisLeft.axisMaximum = 110f // Left Y-axis max (Throttle)
        combinedChart.axisRight.isEnabled = true      // Enable right Y-axis
        combinedChart.axisRight.axisMinimum = 0f
        combinedChart.axisRight.axisMaximum = 20000f  // Right Y-axis max (RPM)
        combinedChart.invalidate()
        combinedChart.fitScreen() // Reset zoom after update
        Log.d("CarDataFragment", "combinedChart.data set and invalidated")

    }
}