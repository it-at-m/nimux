package de.muenchen.appcenter.nimux.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MultiOrderProductList(
    val stringSortID: String,
    val name: String,
    val price: Double,
    var amount: Int,
) : Parcelable