package de.muenchen.appcenter.nimux.viewmodel.manage.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.muenchen.appcenter.nimux.repositories.ProductsRepository
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val usersRepository: UsersRepository, private val productsRepository: ProductsRepository
) : ViewModel() {

    private val _addProductDone = MutableLiveData<Boolean>()
    val addProductDone: LiveData<Boolean>
        get() = _addProductDone

    private val _productAdded = MutableLiveData<Boolean>()
    val productAdded: LiveData<Boolean>
        get() = _productAdded

    private val _hideKeyboard = MutableLiveData<Boolean>()
    val hideKeyboard: LiveData<Boolean>
        get() = _hideKeyboard

    private val _refillable = MutableLiveData(false)
    val refillable: LiveData<Boolean>
        get() = _refillable

    val productName = MutableLiveData("")
    val productPrice = MutableLiveData("")
    val productIcon = MutableLiveData(0)
    val productStock = MutableLiveData("")
    val productRefillSize = MutableLiveData("")

    private val _performHapticFeedback = MutableLiveData<Boolean>()
    val performHapticFeedback: LiveData<Boolean>
        get() = _performHapticFeedback

    val productNameEmpty = MutableLiveData<Boolean>()
    val productPriceEmpty = MutableLiveData<Boolean>()
    val productStockEmpty = MutableLiveData<Boolean>()
    val productRefillSizeEmpty = MutableLiveData<Boolean>()

    val productPriceWrong = MutableLiveData<Boolean>()
    val productStockWrong = MutableLiveData<Boolean>()
    val productRefillSizeWrong = MutableLiveData<Boolean>()

    val productNameExists = MutableLiveData<Boolean>()

    private val _networkHint = MutableLiveData<Boolean>()
    val networkHint: LiveData<Boolean>
        get() = _networkHint

    private val _showProgressBar = MutableLiveData<Boolean>()
    val showProgressBar: LiveData<Boolean>
        get() = _showProgressBar

    fun addProduct() {
        productNameEmpty.value = productName.value?.isEmpty()
        productPriceEmpty.value = productPrice.value?.isEmpty()
        if (refillable.value!!) {
            productStockEmpty.value = productStock.value?.isEmpty()
            productRefillSizeEmpty.value = productRefillSize.value?.isEmpty()
            if (!productNameEmpty.value!! && !productPriceEmpty.value!! && !productStockEmpty.value!! && !productRefillSizeEmpty.value!!) {
                //Refill is true and no input is empty
                productPriceWrong.value = productPrice.value?.replace(',','.')?.toDoubleOrNull() == null
                productStockWrong.value = productStock.value?.toIntOrNull() == null
                productRefillSizeWrong.value =
                    productRefillSize.value?.toIntOrNull() == null || productRefillSize.value!!.toInt() <= 0
                if (!productPriceWrong.value!! && !productStockWrong.value!! && !productRefillSizeWrong.value!!) {

                    viewModelScope.launch {
                        _showProgressBar.value = true
                        if (usersRepository.connectedOnline()) {
                            if (productsRepository.productExists(productName.value.toString()))
                                productNameExists.value = true
                            else {
                                productsRepository.addRefillableProduct(
                                    productName.value.toString(),
                                    productPrice.value!!.replace(',','.').toDouble(),
                                    productIcon.value!!.toInt(),
                                    productStock.value!!.toInt(),
                                    productRefillSize.value!!.toInt()
                                )
                                _productAdded.value = true
                            }
                        } else _networkHint.value = true
                        _showProgressBar.value = false
                    }
                }
            }
        } else {
            if (!productNameEmpty.value!! && !productPriceEmpty.value!!) {
                //Refill is false and no input is empty
                productPriceWrong.value = productPrice.value?.replace(',','.')?.toDoubleOrNull() == null
                if (!productPriceWrong.value!!) {
                    viewModelScope.launch {
                        _showProgressBar.value = true
                        if (usersRepository.connectedOnline()) {
                            if (productsRepository.productExists(productName.value.toString()))
                                productNameExists.value = true
                            else {
                                productsRepository.addNonRefillableProduct(
                                    productName.value.toString(),
                                    productPrice.value!!.replace(',','.').toDouble(),
                                    productIcon.value!!.toInt()
                                )
                                _productAdded.value = true
                            }
                        } else _networkHint.value = true
                        _showProgressBar.value = false
                    }
                }
            }
        }

    }

    fun cancelAdd() {
        _addProductDone.value = true
        doHideKeyboard()
    }

    fun changeRefill() {
        _refillable.value = !refillable.value!!
        productStockWrong.value = false
        productStockEmpty.value = false
        productRefillSizeEmpty.value = false
        productRefillSizeWrong.value = false
        doHideKeyboard()
    }

    private fun doHideKeyboard() {
        _hideKeyboard.value = true
    }

    fun doneHideKeyboard() {
        _hideKeyboard.value = false
    }

    fun networkHintShown() {
        _networkHint.value = false
    }
}