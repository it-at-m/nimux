package de.muenchen.appcenter.nimux.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DonateItem(
    val userId: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val anon: Boolean = false,
    val timeLimit: Long = 0,
    val reAddIfUnused: Boolean = false,
) : Parcelable