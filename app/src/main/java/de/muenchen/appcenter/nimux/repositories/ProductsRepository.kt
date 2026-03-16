package de.muenchen.appcenter.nimux.repositories

import com.google.firebase.firestore.Query
import de.muenchen.appcenter.nimux.datasources.ProductDataSource
import de.muenchen.appcenter.nimux.datasources.UserDataSource
import de.muenchen.appcenter.nimux.model.Product
import java.util.Date
import javax.inject.Inject


class ProductsRepository @Inject constructor() {

    @Inject
    lateinit var productDataSource: ProductDataSource
    @Inject
    lateinit var userDataSource: UserDataSource

    suspend fun connectedOnline(): Boolean {
        return productDataSource.checkOnlineConnection()
    }

    suspend fun productExists(productName: String): Boolean {
        return productDataSource.productExistsCheck(productName)
    }

    fun addRefillableProduct(
        name: String,
        price: Double,
        productIcon: Int,
        currentStock: Int,
        refillSize: Int,
    ) {
        productDataSource.addProduct(
            Product(
                name,
                price,
                productIcon,
                currentStock,
                refillSize
            )
        )
    }

    fun addNonRefillableProduct(name: String, price: Double, productIcon: Int) {
        productDataSource.addProduct(Product(name, price, productIcon))
    }

    fun getProductQuery(): Query {
        return productDataSource.getProductQuery()
    }

    fun getUserStatQuery(userId: String): Query {
        return productDataSource.getUserStatQuery(userId)
    }

    fun getTotalStatQuery(): Query {
        return productDataSource.getTotalStatQuery()
    }

    fun deleteProduct(id: String) {
        productDataSource.deleteProduct(id)
    }

    fun getProductLogsQueryWithTimerangeAndOrderByTimestamp(
        timeRange: Date,
        order: Query.Direction
    ): Query {
        return productDataSource.getProductLogsQueryWithTimerangeAndOrderByTimestamp(
            timeRange,
            order
        )
    }

    suspend fun addStock(id: String, amount: Int) {
        productDataSource.addStock(id, amount)
    }

    fun updateProduct(
        id: String,
        price: Double,
        productIcon: Int,
        currentStock: Int,
        refillSize: Int,
    ) {
        productDataSource.updateProduct(id, price, productIcon, currentStock, refillSize)
    }

    suspend fun productBought(
        userId: String,
        product: Product,
        amount: Int,
        faceDetected: Boolean,
        productDetected: Boolean,
    ) {
        val curUser = userDataSource.getUser(userId)
        productDataSource.productBought(
            userId,
            product,
            amount,
            faceDetected,
            productDetected,
            curUser
        )
    }

    suspend fun getProduct(id: String): Product {
        return productDataSource.getProduct(id)
    }

    suspend fun getAllProducts(): List<Product> = productDataSource.getAllProducts()


}