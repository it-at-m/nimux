package de.muenchen.appcenter.nimux.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HelpItem(
    val title: String = "",
    val body: String = "",
    val orderPos: Long = 1000000
) : Parcelable