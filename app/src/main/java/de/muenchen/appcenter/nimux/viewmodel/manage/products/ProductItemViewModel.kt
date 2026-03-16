package de.muenchen.appcenter.nimux.viewmodel.manage.products

import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.model.Product
import de.muenchen.appcenter.nimux.repositories.ProductsRepository
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.getProductIcon
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductItemViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val usersRepository: UsersRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var product: Product = savedStateHandle.get<Product>("currentProduct")
        ?: error("Product missing in SavedStateHandle")

    private var buttonAtClick = 0
    private var customValue = 0

    private val _sPrice = MutableLiveData(String.format("%.2f", product.price) + "€")
    val sPrice: LiveData<String>
        get() = _sPrice

    private val _sStock = MutableLiveData(product.currentStock.toString())
    val sStock: LiveData<String>
        get() = _sStock

    private val _sRefill1 = MutableLiveData(product.refillSize.toString())
    val sRefill1: LiveData<String>
        get() = _sRefill1

    private val _sRefill2 = MutableLiveData(product.refillSize.times(2).toString())
    val sRefill2: LiveData<String>
        get() = _sRefill2

    private val _sRefill3 = MutableLiveData(product.refillSize.times(3).toString())
    val sRefill3: LiveData<String>
        get() = _sRefill3

    private val _customAmountChecked = MutableLiveData(false)
    val customAmountChecked: LiveData<Boolean>
        get() = _customAmountChecked

    val productItemResource = getProductIcon(product.productIcon)

    val checkedButton = ObservableInt()

    var customAmount = ""

    private val _networkHint = MutableLiveData<Boolean>()
    val networkHint: LiveData<Boolean>
        get() = _networkHint

    private val _noChipSelected = MutableLiveData<Boolean>()
    val noChipSelected: LiveData<Boolean>
        get() = _noChipSelected

    private val _showProgressBar = MutableLiveData<Boolean>()
    val showProgressBar: LiveData<Boolean>
        get() = _showProgressBar

    private val _deleted = MutableLiveData<Boolean>()
    val deleted: LiveData<Boolean>
        get() = _deleted

    private val _wrongInput = MutableLiveData<Boolean>()
    val wrongInput: LiveData<Boolean>
        get() = _wrongInput

    private val _stockAdded = MutableLiveData<Boolean>()
    val stockAdded: LiveData<Boolean>
        get() = _stockAdded

    fun checkedChanged() {
        _customAmountChecked.value = checkedButton.get() == R.id.chip_product_other
    }

    fun addStockClick() {
        _wrongInput.value = (customAmount.toIntOrNull() == null) || (customAmount.contains('-'))
        buttonAtClick = checkedButton.get()
        customValue = if (buttonAtClick == R.id.chip_product_other &&
            !wrongInput.value!!
        )
            customAmount.toInt()
        else 0
        _noChipSelected.value = checkedButton.get() == 0
        if (!noChipSelected.value!!) {
            addStock()
        }
    }

    fun addStock() {
        viewModelScope.launch {
            _showProgressBar.value = true
            if (usersRepository.connectedOnline()) {
                when (buttonAtClick) {
                    R.id.chip_refill1 -> {
                        productsRepository.addStock(product.stringSortID, product.refillSize)
                        _stockAdded.value = true
                    }

                    R.id.chip_refill2 -> {
                        productsRepository.addStock(
                            product.stringSortID,
                            product.refillSize.times(2)
                        )
                        _stockAdded.value = true
                    }

                    R.id.chip_refill3 -> {
                        productsRepository.addStock(
                            product.stringSortID,
                            product.refillSize.times(3)
                        )
                        _stockAdded.value = true
                    }

                    R.id.chip_product_other -> {
                        if (!wrongInput.value!!) {
                            productsRepository.addStock(product.stringSortID, customValue)
                            _stockAdded.value = true
                        }
                    }
                }
            } else
                _networkHint.value = true
            _showProgressBar.value = false
        }
    }

    fun networkHintShown() {
        _networkHint.value = false
    }

    fun noChipHintShown() {
        _noChipSelected.value = false
    }

    fun resetChip() {
        checkedButton.set(0)
    }

    fun deleteCurrentProduct() {
        viewModelScope.launch {
            _showProgressBar.value = true
            productsRepository.deleteProduct(product.stringSortID)
            _deleted.value = true
            _showProgressBar.value = false
        }
    }
}