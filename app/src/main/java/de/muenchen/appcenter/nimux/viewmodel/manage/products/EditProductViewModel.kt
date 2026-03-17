package de.muenchen.appcenter.nimux.viewmodel.manage.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.muenchen.appcenter.nimux.model.Product
import de.muenchen.appcenter.nimux.repositories.ProductsRepository
import de.muenchen.appcenter.nimux.util.round
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProductViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var product: Product = savedStateHandle.get<Product>("product")
        ?: error("Product missing in SavedStateHandle")

    val productIcon = MutableLiveData(product.productIcon)
    val productRefillSize = MutableLiveData(product.refillSize.toString())
    val productStock = MutableLiveData(product.currentStock.toString())

    private val _refillable = MutableLiveData(product.refillSize > 0)
    val refillable: LiveData<Boolean> get() = _refillable

    val priceInput = MutableLiveData(String.format("%.2f", product.price))

    fun changeRefill() {
        doHideKeyboard()
        _refillable.value = !refillable.value!!
    }

    private val _priceInputWrong = MutableLiveData<Boolean>()
    val priceInputWrong: LiveData<Boolean>
        get() = _priceInputWrong
    private val _stockInputWrong = MutableLiveData<Boolean>()
    val stockInputWrong: LiveData<Boolean>
        get() = _stockInputWrong
    private val _refillInputWrong = MutableLiveData<Boolean>()
    val refillInputWrong: LiveData<Boolean>
        get() = _refillInputWrong

    private val _canceled = MutableLiveData<Boolean>()
    val canceled: LiveData<Boolean>
        get() = _canceled
    private val _updated = MutableLiveData<Boolean>()
    val updated: LiveData<Boolean>
        get() = _updated

    private val _networkHint = MutableLiveData<Boolean>()
    val networkHint: LiveData<Boolean>
        get() = _networkHint

    private val _showProgressBar = MutableLiveData<Boolean>()
    val showProgressBar: LiveData<Boolean>
        get() = _showProgressBar

    fun updateProduct() {

        _priceInputWrong.value = priceInput.value?.replace(',', '.')?.toDoubleOrNull() == null
        _stockInputWrong.value = productStock.value?.toIntOrNull() == null && refillable.value!!
        _refillInputWrong.value =
            (productRefillSize.value?.toIntOrNull() == null || productRefillSize.value?.toInt()!! <= 0 && refillable.value!!)

        if (refillable.value!!) {
            if (!priceInputWrong.value!! && !stockInputWrong.value!! && !refillInputWrong.value!!) {
                viewModelScope.launch {
                    _showProgressBar.value = true
                    if (productsRepository.connectedOnline()) {
                        _networkHint.value = false
                        productsRepository.updateProduct(
                            product.stringSortID,
                            (priceInput.value?.replace(',', '.')?.toDouble() ?: 0.0).round(2),
                            productIcon.value?.toInt()!!,
                            productStock.value?.toInt()!!,
                            productRefillSize.value?.toInt()!!
                        )
                    } else _networkHint.value = true
                    product = productsRepository.getProduct(product.stringSortID)
                    _updated.value = true
                }
            }
        } else {
            if (!priceInputWrong.value!!) {
                viewModelScope.launch {
                    _showProgressBar.value = true
                    if (productsRepository.connectedOnline()) {
                        _networkHint.value = false
                        productsRepository.updateProduct(
                            product.stringSortID,
                            priceInput.value?.replace(',', '.')?.toDouble() ?: 0.0,
                            productIcon.value?.toInt()!!,
                            0,
                            0
                        )
                    } else _networkHint.value = true
                    _showProgressBar.value = false
                    product = productsRepository.getProduct(product.stringSortID)
                    _updated.value = true
                }
            }
        }
    }

    fun cancel() {
        _canceled.value = true
    }

    fun networkHintShown() {
        _networkHint.value = false
    }


    private val _hideKeyboard = MutableLiveData<Boolean>()
    val hideKeyboard: LiveData<Boolean>
        get() = _hideKeyboard

    private fun doHideKeyboard() {
        _hideKeyboard.value = true
    }

    fun doneHideKeyboard() {
        _hideKeyboard.value = false
    }
}