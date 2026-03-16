package de.muenchen.appcenter.nimux.datasources

import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import dagger.internal.Provider
import de.muenchen.appcenter.nimux.model.DonateItem
import de.muenchen.appcenter.nimux.model.Product
import de.muenchen.appcenter.nimux.repositories.ProductsRepository
import de.muenchen.appcenter.nimux.util.collection_donate
import de.muenchen.appcenter.nimux.util.userlog_description_payed_with_donation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

class DonateItemDataSource @Inject constructor(
    private val tenantRefProvider: Provider<DocumentReference>
) {
    private val tag = "DonateDataSource"
    private val donateItemId = "donationItem"

    private val donateCollectionRef: CollectionReference?
        get() = tenantRefProvider.get()?.collection(collection_donate)

    private fun requireDonateCollectionRef(): CollectionReference {
        return donateCollectionRef
            ?: throw IllegalStateException("Tenant fehlt – User ist nicht eingeloggt")
    }

    @Inject
    lateinit var userDataSource: UserDataSource

    @Inject
    lateinit var productsRepository: ProductsRepository


    suspend fun getDonationItem(priceToPay: Double = 0.0): DonateItem? {
        Log.d(tag, "getDonate")
        val res = CompletableDeferred<DonateItem?>()
        var donateItem: DonateItem? = null
        requireDonateCollectionRef().document(donateItemId).get().addOnCompleteListener {
            if (it.isSuccessful && it.result != null) {
                donateItem =
                    it.result?.toObject(DonateItem::class.java)
                if (donateItem != null) {
                    val timeLimit = donateItem?.timeLimit ?: 0
                    val amount = donateItem?.amount ?: 0.0
                    val now = Calendar.getInstance().timeInMillis

                    if ((timeLimit > 0 && timeLimit > now) || (priceToPay != 0.0 && amount > priceToPay)) {
                        val cancelDonItem = donateItem
                        donateItem = null
                        CoroutineScope(Dispatchers.IO).launch {
                            cancelDonationItem(cancelDonItem!!)
                        }
                    }
                }
            }
            res.complete(donateItem)
        }
        return res.await()
    }

    suspend fun addDonationItem(donateItem: DonateItem): Boolean {
        val res = CompletableDeferred<Boolean>()
        Log.d(tag, "add Donate")
        Log.d(tag, "${getDonationItem()}")
        if (getDonationItem() == null) {
            Log.d(tag, "getDonate equals null")
            requireDonateCollectionRef().document(donateItemId).set(donateItem)
                .addOnCompleteListener {
                    userDataSource.payDono(donateItem.userId, donateItem)
                    res.complete(true)
                }
        } else res.complete(false)
        Log.d(tag, "addDonate complete")
        return res.await()
    }

    private suspend fun cancelDonationItem(donateItem: DonateItem) {
        if (donateItem.reAddIfUnused) {
            userDataSource.addCredit(donateItem.userId, donateItem.amount)
        }
        requireDonateCollectionRef().document(donateItemId).delete()
    }

    suspend fun payWithDonationItem(
        userId: String,
        donateItem: DonateItem,
        product: Product,
        amount: Int,
        faceDetected: Boolean,
        productDetected: Boolean
    ) {
        requireDonateCollectionRef().document(donateItemId)
            .update("amount", donateItem.amount.minus(product.price.times(amount)))
        productsRepository.productBought(userId, product, amount, faceDetected, productDetected)
        logUserAction(
            userId,
            userlog_description_payed_with_donation + "$amount * ${product.name}",
            product.price * amount
        )
    }


    private fun logUserAction(id: String, description: String, amount: Double = 0.0) {
        userDataSource.logUserAction(id, description, amount)
    }
}