package de.muenchen.appcenter.nimux.view.manage.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.muenchen.appcenter.nimux.datasources.ProductLog

class ProductLogDetailViewModel(val productLog: ProductLog) : ViewModel()

class ProductLogDetailVMFactory(private val productLog: ProductLog) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductLogDetailViewModel::class.java)) return ProductLogDetailViewModel(
            productLog) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}