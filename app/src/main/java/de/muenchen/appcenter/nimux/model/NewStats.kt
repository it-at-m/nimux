package de.muenchen.appcenter.nimux.model

import java.util.Calendar
import java.util.Date

data class TotalStatsDoc(
    val id: String = "",
    val name: String = "",
    var firstTimeStamp: Date? = null,

    var totalAmount: Long = 0,

    var highestWeekAmount: Int = 0,
    var highestWeekNYear: String = "",

    var currentWeekAmount: Int = 0,
    var currentWeekNYear: String = "",

    var faceDetected: Long = 0,
    var prodsDetected: Long = 0,

    var thisMonth: String = "",
    var totalThisMonth: Int = 0,
    var totalLastMonth1: Int = 0,
    var totalLastMonth2: Int = 0,
    var totalLastMonth3: Int = 0,
    var totalLastMonth4: Int = 0,
    var totalLastMonth5: Int = 0,
)

fun updateTotalStatDoc(
    totalDoc: TotalStatsDoc,
    amount: Int,
    faceDetected: Boolean,
    prodsDetected: Boolean
): TotalStatsDoc {
    val cal = Calendar.getInstance()
    val currentWeekNYear =
        cal.get(Calendar.WEEK_OF_YEAR).toString() + "/" + cal.get(Calendar.YEAR).toString()
    val currentMonthNYear =
        cal.get(Calendar.MONTH).toString() + "/" + cal.get(Calendar.YEAR).toString()

    totalDoc.totalAmount += amount
    if (totalDoc.currentWeekNYear != currentWeekNYear) {
        totalDoc.currentWeekNYear = currentWeekNYear
        totalDoc.currentWeekAmount = 0
    }
    totalDoc.currentWeekAmount += amount
    if (totalDoc.currentWeekAmount >= totalDoc.highestWeekAmount) {
        totalDoc.highestWeekAmount = totalDoc.currentWeekAmount
        totalDoc.highestWeekNYear = totalDoc.currentWeekNYear
    }
    if (totalDoc.thisMonth != currentMonthNYear) {
        totalDoc.thisMonth = currentMonthNYear
        totalDoc.totalLastMonth5 = totalDoc.totalLastMonth4
        totalDoc.totalLastMonth4 = totalDoc.totalLastMonth3
        totalDoc.totalLastMonth3 = totalDoc.totalLastMonth2
        totalDoc.totalLastMonth2 = totalDoc.totalLastMonth1
        totalDoc.totalLastMonth1 = totalDoc.totalThisMonth
        totalDoc.totalThisMonth = 0
    }
    totalDoc.totalThisMonth += amount
    if (faceDetected) totalDoc.faceDetected += amount
    if (prodsDetected) totalDoc.prodsDetected++
    return totalDoc
}