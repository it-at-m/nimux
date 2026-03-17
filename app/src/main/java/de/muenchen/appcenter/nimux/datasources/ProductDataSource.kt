package de.muenchen.appcenter.nimux.datasources

import android.os.Build
import android.os.Parcelable
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.toObject
import dagger.internal.Provider
import de.muenchen.appcenter.nimux.model.Product
import de.muenchen.appcenter.nimux.model.TotalStatsDoc
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.model.updateTotalStatDoc
import de.muenchen.appcenter.nimux.util.coll_stats_main_doc
import de.muenchen.appcenter.nimux.util.collection_productLogs
import de.muenchen.appcenter.nimux.util.collection_products
import de.muenchen.appcenter.nimux.util.collection_stats
import de.muenchen.appcenter.nimux.util.collection_stats_total
import de.muenchen.appcenter.nimux.util.productlog_description_add_product
import de.muenchen.appcenter.nimux.util.productlog_description_add_stock
import de.muenchen.appcenter.nimux.util.productlog_description_delete_product
import de.muenchen.appcenter.nimux.util.productlog_description_update
import de.muenchen.appcenter.nimux.util.stringToStringSortID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class ProductDataSource @Inject constructor(
    private val tenantRefProvider: Provider<DocumentReference>
) {

    private val collectionProductRef: CollectionReference?
        get() = tenantRefProvider.get()?.collection(collection_products)

    private fun requireCollectionProductRef(): CollectionReference {
        return collectionProductRef
            ?: throw IllegalStateException("Tenant missing – user is not logged in")
    }

    private val collectionProductLogsRef: CollectionReference?
        get() = tenantRefProvider.get()?.collection(collection_productLogs)

    private fun requireCollectionProductLogsRef(): CollectionReference {
        return collectionProductLogsRef
            ?: throw IllegalStateException("Tenant missing – user is not logged in")
    }


    private val statisticRef: CollectionReference?
        get() = tenantRefProvider.get()?.collection(collection_stats)

    private fun requireStatisticRef(): CollectionReference {
        return statisticRef
            ?: throw IllegalStateException("Tenant missing – user is not logged in")
    }

    fun totalStatQuery(): CollectionReference {
        return requireStatisticRef()
            .document("totalStats")
            .collection(collection_stats_total)
    }

    fun userStatQuery(): DocumentReference {
        return requireStatisticRef()
            .document("userStats")
    }

    suspend fun checkOnlineConnection(): Boolean {

        var connected = true
        try {
            requireCollectionProductRef().get(Source.SERVER).await()
        } catch (e: Exception) {
            connected = false
            Timber.d("ConnectCheck failure: $e")
        }
        return connected
    }

    suspend fun getAllProducts(): List<Product> {
        val res = CompletableDeferred<List<Product>>()
        var products = mutableListOf<Product>()
        requireCollectionProductRef().get(Source.SERVER).addOnCompleteListener {
            products = it.result.toObjects(Product::class.java)
            res.complete(products)
        }
        return res.await()
    }

    suspend fun getProduct(id: String): Product {
        var product = Product()
        requireCollectionProductRef().document(id).get().addOnCompleteListener {
            if (it.isSuccessful && it.result != null) {
                product = it.result?.toObject<Product>()!!
            }
        }.await()
        delay(20)
        return product
    }

    fun getProductQuery(): Query {
        return requireCollectionProductRef().orderBy("stringSortID", Query.Direction.ASCENDING)
    }

    fun getTotalStatQuery(): Query {
        return totalStatQuery().orderBy("totalAmount", Query.Direction.DESCENDING)
    }

    fun getProductLogsQueryWithTimerangeAndOrderByTimestamp(
        timeRange: Date,
        order: Query.Direction
    ): Query {
        return requireCollectionProductLogsRef().whereGreaterThan("timestamp", timeRange)
            .orderBy("timestamp", order)
    }

    fun getUserStatQuery(userId: String): Query {
        return userStatQuery().collection(userId + "stats")
            .orderBy("totalAmount", Query.Direction.DESCENDING)
    }

    suspend fun productExistsCheck(productName: String): Boolean {
        var productExists = false
        requireCollectionProductRef().get().addOnSuccessListener { result ->
            for (document in result) {
                if (document.id == stringToStringSortID(productName))
                    productExists = true
            }
        }.await()
        return productExists
    }

    fun addProduct(product: Product) {
        requireCollectionProductRef().document(product.stringSortID).set(product)
        logProductAction(product.stringSortID, productlog_description_add_product)
    }

    fun deleteProduct(id: String) {
        logProductAction(id, productlog_description_delete_product)
        requireCollectionProductRef().document(id).delete()
    }

    fun updateProduct(
        id: String,
        price: Double,
        productIcon: Int,
        currentStock: Int,
        refillSize: Int,
    ) {
        val product = requireCollectionProductRef().document(id)
        product.update("price", price)
        product.update("productIcon", productIcon)
        product.update("currentStock", currentStock)
        product.update("refillSize", refillSize)
        logProductAction(
            id,
            String.Companion.format(
                productlog_description_update,
                price.toString(),
                productIcon.toString(),
                currentStock.toString(),
                refillSize.toString()
            )
        )
    }

    suspend fun addStock(prodID: String, amount: Int) {
        var currentStock = 0

        requireCollectionProductRef().document(prodID).get().addOnSuccessListener {
            currentStock = it.get("currentStock").toString().toInt()
        }.await()

        requireCollectionProductRef().document(prodID).update("currentStock", currentStock + amount)
        logProductAction(prodID, productlog_description_add_stock, amount)
    }

    private fun logProductAction(id: String, description: String, amount: Int = 0) {
        val log = ProductLog(id, description, amount)
        requireCollectionProductLogsRef().add(log)
    }

    suspend fun productBought(
        userId: String,
        product: Product,
        amount: Int,
        faceDetected: Boolean,
        productDetected: Boolean,
        currentUser: User?
    ) {
        if (product.refillSize > 0)
            requireCollectionProductRef().document(product.stringSortID)
                .update("currentStock", product.currentStock - amount)
        newAddStats(userId, product, amount, faceDetected, productDetected, currentUser)
    }

    private suspend fun newAddStats(
        userId: String,
        product: Product,
        amount: Int,
        faceDetected: Boolean,
        productDetected: Boolean,
        currentUser: User?,
    ) {
        totalStatQuery().document(product.stringSortID + coll_stats_main_doc).get()
            .addOnSuccessListener {
                val totalDoc =
                    it.toObject(TotalStatsDoc::class.java) ?: TotalStatsDoc(
                        product.stringSortID,
                        product.name,
                        firstTimeStamp = Calendar.getInstance().time
                    )
                totalStatQuery().document(product.stringSortID + coll_stats_main_doc).set(
                    updateTotalStatDoc(totalDoc, amount, faceDetected, productDetected)
                )
            }
        if (currentUser != null)
            if (currentUser.collectData)
                userStatQuery().collection(userId + "stats")
                    .document(product.stringSortID + coll_stats_main_doc).get()
                    .addOnSuccessListener {
                        val userDoc =
                            it.toObject(TotalStatsDoc::class.java) ?: TotalStatsDoc(
                                product.stringSortID,
                                product.name,
                                firstTimeStamp = Calendar.getInstance().time
                            )
                        userStatQuery().collection(userId + "stats")
                            .document(product.stringSortID + coll_stats_main_doc).set(
                                updateTotalStatDoc(userDoc, amount, faceDetected, productDetected)
                            )
                    }
    }

}

@Parcelize
class ProductLog(
    val productID: String = "",
    val description: String = "",
    val amount: Int = 0,
) : Parcelable {
    val timestamp = Calendar.getInstance().time
    val device = Build.MODEL
    val changedByEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
}