package de.muenchen.appcenter.nimux.view.main.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentStatisticsBinding
import de.muenchen.appcenter.nimux.model.TotalStatsDoc
import de.muenchen.appcenter.nimux.repositories.ProductsRepository
import de.muenchen.appcenter.nimux.util.getChartColors
import de.muenchen.appcenter.nimux.util.round
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
open class StatisticsFragment : Fragment() {

    @Inject
    lateinit var productsRepository: ProductsRepository

    var _binding: FragmentStatisticsBinding? = null
    val binding get() = _binding!!

    val totalStats = mutableListOf<TotalStatsDoc>()

    private var chartAnimationDuration = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.stat_fragment_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_menu_show_personal_stats -> {
                findNavController().navigate(StatisticsFragmentDirections.actionNavStatisticsToPersonalStatsSelectFragment())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        chartAnimationDuration = resources.getInteger(R.integer.motion_long).times(3)
        getStats()
    }

    open fun getStats() {
        if (totalStats.isEmpty())
            productsRepository.getTotalStatQuery().get().addOnSuccessListener { query ->
                query.documents.forEach { doc ->
                    val newDoc = doc.toObject(TotalStatsDoc::class.java)
                    if (newDoc != null) {
                        totalStats.add(newDoc)
                    }
                }
                if (totalStats.isEmpty())
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.statistics_not_found_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                else initCharts()
            }.addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Data couldn't be retrieved. Error: ${it.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        else initCharts()
    }

    open fun initCharts() {
        if (_binding != null) {
            setUpTotalPieChart()
            setUpMostPerWeekChart()
            setUpCurrentWeekChart()
            setUpFaceReconChart()
            setUpAverageChart()
            setUpLastMonthsChart()
        }
    }

    private fun setUpLastMonthsChart() {
        setUpBarCharts(binding.lastMonthsChart)
        val totalLastMonths = IntArray(6)
        totalStats.forEach { doc ->
            totalLastMonths[0] += doc.totalThisMonth
            totalLastMonths[1] += doc.totalLastMonth1
            totalLastMonths[2] += doc.totalLastMonth2
            totalLastMonths[3] += doc.totalLastMonth3
            totalLastMonths[4] += doc.totalLastMonth4
            totalLastMonths[5] += doc.totalLastMonth5
        }
        val cal = Calendar.getInstance()
        val xLabels = ArrayList<String>()
        val monthFormat = SimpleDateFormat("MMMM", Locale.GERMANY)

        val barEntries = ArrayList<BarEntry>()

        for (i in 0..5) {
            cal.add(Calendar.MONTH, -i)
            xLabels.add(monthFormat.format(cal.time))
            barEntries.add(BarEntry(i.toFloat(), totalLastMonths[i].toFloat()))
        }
        binding.lastMonthsChart.xAxis.apply {
            labelCount = xLabels.size
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return xLabels[value.toInt()]
                }
            }
        }
        binding.lastMonthsChart.xAxis.textSize = 16f
        val dataSet = BarDataSet(barEntries, "")
        dataSet.color = requireContext().getColor(R.color.chart_pastel_mauve)
        dataSet.valueTextSize = 16f
        val data = BarData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return DecimalFormat("#").format(value)
            }
        })
        binding.lastMonthsChart.data = data
    }

    private fun setUpAverageChart() {
        setUpBarCharts(binding.weeklyAverageBar)
        val monthCurrent = Calendar.getInstance()
        val statsWithAverage = ArrayList<Pair<TotalStatsDoc, Float>>()
        totalStats.forEach { doc ->
            if (doc.firstTimeStamp != null) {
                val firstTime = Calendar.getInstance()
                firstTime.time = doc.firstTimeStamp!!
                val yearsDif = monthCurrent.get(Calendar.YEAR) - firstTime.get(Calendar.YEAR)
                val weekDif =
                    monthCurrent.get(Calendar.WEEK_OF_YEAR) - firstTime.get(Calendar.WEEK_OF_YEAR) + yearsDif.times(
                        52
                    )
                statsWithAverage.add(
                    Pair(
                        doc,
                        doc.totalAmount.toDouble().div(weekDif.plus(1).toDouble()).round(2)
                            .toFloat()
                    )
                )
            }
        }
        statsWithAverage.sortBy { it.second }
        statsWithAverage.reverse()

        val barEntry = ArrayList<BarEntry>()
        val xLabels = ArrayList<String>()

        statsWithAverage.forEachIndexed { i, statWithA ->
            if (i < 8) {
                barEntry.add(BarEntry(i.toFloat(), statWithA.second))
                xLabels.add(statWithA.first.name)
            }
        }
        binding.weeklyAverageBar.xAxis.apply {
            labelCount = xLabels.size
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return xLabels[value.toInt()]
                }
            }
        }
        val dataSet = BarDataSet(barEntry, "")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.chart_pastel_blue)
        dataSet.valueTextSize = 16f
        val data = BarData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return DecimalFormat("0.00").format(value)
            }
        })
        binding.weeklyAverageBar.data = data
    }

    private fun setUpFaceReconChart() {
        setUpBinaryBarCharts(binding.pieFaceReconStats)
        var totalFaceUnlocks = 0L
        var totalBuy = 0L
        val pieEntries = ArrayList<PieEntry>()
        totalStats.forEach {
            totalFaceUnlocks += it.faceDetected
            totalBuy += it.totalAmount
        }
        pieEntries.add(
            PieEntry(
                totalFaceUnlocks.toFloat(),
                getString(R.string.with_face_chart_data_title)
            )
        )
        pieEntries.add(
            PieEntry(
                totalBuy.minus(totalFaceUnlocks).toFloat(),
                getString(R.string.without_face_chart_data_title)
            )
        )
        val dataset = PieDataSet(pieEntries, "")
        dataset.colors = mutableListOf(
            requireContext().getColor(R.color.chart_pastel_apple),
            requireContext().getColor(R.color.chart_pastel_baby)
        )
        val data = PieData(dataset)
        data.setDrawValues(false)
        binding.pieFaceReconStats.data = data
    }

    private fun setUpCurrentWeekChart() {
        val cal = Calendar.getInstance()
        val currentWeekNYear =
            cal.get(Calendar.WEEK_OF_YEAR).toString() + "/" + cal.get(Calendar.YEAR).toString()
        setUpBarCharts(binding.weeklyCurrentBar)

        val xLabels = ArrayList<String>()

        val sortedByCurrentWeek = totalStats
        sortedByCurrentWeek.forEach {
            if (it.currentWeekNYear != currentWeekNYear) {
                it.currentWeekAmount = 0
            }
        }
        sortedByCurrentWeek.sortBy { it.currentWeekAmount }
        sortedByCurrentWeek.reverse()

        val barEntries = ArrayList<BarEntry>()
//        sortedByMostWeek.forEach {
//            if (it.currentWeekNYear != currentWeekNYear)
//                sortedByMostWeek.remove(it)
//        }
        sortedByCurrentWeek.forEachIndexed { index, totalStatsDoc ->
            if (index < 6) {
                barEntries.add(BarEntry(index.toFloat(), totalStatsDoc.currentWeekAmount.toFloat()))
                xLabels.add(totalStatsDoc.name)
            }
        }

        binding.weeklyCurrentBar.xAxis.apply {
            labelCount = xLabels.size
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return xLabels[value.toInt()]
                }
            }
        }
        val dataSet = BarDataSet(barEntries, "")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.chart_pastel_light_blue)
        dataSet.valueTextSize = 16f
        val data = BarData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return DecimalFormat("#").format(value)
            }
        })
        binding.weeklyCurrentBar.data = data
    }

    private fun setUpMostPerWeekChart() {
        setUpBarCharts(binding.weeklyBestBar)

        val xLabels = ArrayList<String>()

        val sortedByMostWeek = totalStats
        sortedByMostWeek.sortBy { it.highestWeekAmount }
        sortedByMostWeek.reverse()

        val barEntries = ArrayList<BarEntry>()
        sortedByMostWeek.forEachIndexed { index, totalStatsDoc ->
            if (totalStatsDoc.highestWeekAmount > 0 && index < 6) {
                barEntries.add(BarEntry(index.toFloat(), totalStatsDoc.highestWeekAmount.toFloat()))
                xLabels.add(totalStatsDoc.name)
            }
        }
        binding.weeklyBestBar.xAxis.apply {
            labelCount = xLabels.size
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return xLabels[value.toInt()]
                }
            }
        }
        val dataSet = BarDataSet(barEntries, "")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.chart_pastel_electric)
        dataSet.valueTextSize = 16f
        val data = BarData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return DecimalFormat("#").format(value)
            }
        })
        binding.weeklyBestBar.data = data
    }

    private fun setUpTotalPieChart() {
        binding.pieTotalStats.apply {
            setTransparentCircleAlpha(0)
            animateXY(chartAnimationDuration, chartAnimationDuration, Easing.EaseOutCirc)
            setDrawEntryLabels(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setUsePercentValues(false)
            setEntryLabelTextSize(16f)
            setEntryLabelColor(Color.BLACK)
            setHoleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            this.legend.isEnabled = false
        }

        val pieEntries = ArrayList<PieEntry>()
        totalStats.forEach { doc ->
            pieEntries.add(
                PieEntry(
                    doc.totalAmount.toFloat(),
                    doc.name
                )
            )
        }
        val dataSet = PieDataSet(pieEntries, getString(R.string.products))
        dataSet.colors = getChartColors(requireContext(), pieEntries.size).toMutableList()
        val data = PieData(dataSet)
        data.setDrawValues(false)
        binding.pieTotalStats.data = data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpBarCharts(chart: BarChart) {
        chart.apply {
            xAxis.setCenterAxisLabels(false)
            xAxis.granularity = 1f
            xAxis.isGranularityEnabled = true
            setTouchEnabled(false)
            isDoubleTapToZoomEnabled = false
            animateY(chartAnimationDuration, Easing.EaseOutSine)
            setPinchZoom(false)
            description.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            xAxis.setDrawAxisLine(false)
            xAxis.setDrawGridLines(false)
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
        }
    }

    private fun setUpBinaryBarCharts(chart: PieChart) {
        chart.apply {
            setTransparentCircleAlpha(0)
            animateXY(chartAnimationDuration, chartAnimationDuration, Easing.EaseOutCirc)
            setDrawEntryLabels(true)
            description.isEnabled = false
            setUsePercentValues(false)
            setEntryLabelTextSize(16f)
            setEntryLabelColor(Color.BLACK)
            isDrawHoleEnabled = false
            this.legend.isEnabled = false
        }
    }
}