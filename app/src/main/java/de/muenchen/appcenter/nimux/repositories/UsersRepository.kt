package de.muenchen.appcenter.nimux.repositories

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import de.muenchen.appcenter.nimux.datasources.DonateItemDataSource
import de.muenchen.appcenter.nimux.datasources.UserDataSource
import de.muenchen.appcenter.nimux.model.MultiOrderProductListWithProduct
import de.muenchen.appcenter.nimux.model.Product
import de.muenchen.appcenter.nimux.model.User
import java.util.Date
import javax.inject.Inject

class UsersRepository @Inject constructor() {

    @Inject
    lateinit var userDataSource: UserDataSource

    @Inject
    lateinit var donateItemDataSource: DonateItemDataSource

    suspend fun connectedOnline(): Boolean {
        return userDataSource.checkOnlineConnection()
    }

    fun addUser(user: User) {
        userDataSource.addUser(user)
    }

    suspend fun getUser(userID: String): User? {
        return userDataSource.getUser(userID)
    }

    suspend fun userExistsCheck(username: String): Boolean {
        return userDataSource.userExistsCheck(username)
    }

    suspend fun addCredit(userID: String, amountPayed: Double) {
        userDataSource.addCredit(userID, amountPayed)
    }

    fun getUserRVQuery(): Query {
        return userDataSource.getUserQuery()
    }

    fun deleteUser(userID: String) {
        userDataSource.deleteUser(userID)
    }

    fun updateUser(
        id: String,
        showCredit: Boolean,
        collectData: Boolean,
        pin: String?,
        useProductAI: Boolean,
        faceSkipsPin: Boolean
    ) {
        userDataSource.updateUser(id, showCredit, collectData, pin, useProductAI, faceSkipsPin)
    }

    fun updateUserMail(id: String, mail: String) {
        userDataSource.updateUserMail(id, mail)
    }

    fun getUserLogsRVQuery(currentUserSortId: String): Query {
        return userDataSource.getUserLogsRVQuery(currentUserSortId)
    }

    fun getCountUserLogsTotalStatsQuery(): Task<QuerySnapshot?> {
        return userDataSource.getCountUserLogsTotalStatsQuery()
    }

    fun getUserLogsQueryWithTimerangeAndOrderByTimestamp(
        timeRange: Date,
        order: Query.Direction
    ): Query {
        return userDataSource.getUserLogsQueryWithTimerangeAndOrderByTimestamp(timeRange, order)
    }

    suspend fun payProduct(
        userID: String,
        product: Product,
        amount: Int = 1,
        faceDetected: Boolean,
        productDetected: Boolean,
    ) {
        val dono = donateItemDataSource.getDonationItem(product.price * amount)
        if (dono != null) {
            donateItemDataSource.payWithDonationItem(
                userID,
                dono,
                product,
                amount,
                faceDetected,
                productDetected
            )
        } else {
            userDataSource.payProduct(userID, product, amount, faceDetected, productDetected)
        }
    }

    suspend fun buyMultipleProducts(
        userID: String,
        multiOrderProductListWithProduct: List<MultiOrderProductListWithProduct>,
    ) {
        userDataSource.payMultiProduct(userID, multiOrderProductListWithProduct)
    }

}