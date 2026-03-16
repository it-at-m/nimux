package de.muenchen.appcenter.nimux.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MultiOrderProductListWithProduct(
    val multiOrderProductList: MultiOrderProductList,
    val product: Product,
) : Parcelable