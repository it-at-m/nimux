package de.muenchen.appcenter.nimux.model

import android.os.Parcelable
import de.muenchen.appcenter.nimux.util.product_icon_none
import de.muenchen.appcenter.nimux.util.stringToStringSortID
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val name: String = "",
    val price: Double = 0.0,
    val productIcon: Int = product_icon_none,
    val currentStock: Int = 0,
    val refillSize: Int = 0,
    val stringSortID: String = stringToStringSortID(name),
) : Parcelable