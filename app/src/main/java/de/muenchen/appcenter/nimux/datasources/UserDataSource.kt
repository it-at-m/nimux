package de.muenchen.appcenter.nimux.datasources

import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.toObject
import dagger.internal.Provider
import de.muenchen.appcenter.nimux.model.DonateItem
import de.muenchen.appcenter.nimux.model.MultiOrderProductListWithProduct
import de.muenchen.appcenter.nimux.model.NameColors
import de.muenchen.appcenter.nimux.model.Product
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.util.collection_userLogs
import de.muenchen.appcenter.nimux.util.collection_users
import de.muenchen.appcenter.nimux.util.round
import de.muenchen.appcenter.nimux.util.stringToStringSortID
import de.muenchen.appcenter.nimux.util.userlog_description_add_balance
import de.muenchen.appcenter.nimux.util.userlog_description_add_balance_paypal
import de.muenchen.appcenter.nimux.util.userlog_description_add_user
import de.muenchen.appcenter.nimux.util.userlog_description_deleted
import de.muenchen.appcenter.nimux.util.userlog_description_dono_started
import de.muenchen.appcenter.nimux.util.userlog_description_payed
import de.muenchen.appcenter.nimux.util.userlog_description_update
import de.muenchen.appcenter.nimux.util.userlog_mail_update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class UserDataSource @Inject constructor(
    private val tenantRefProvider: Provider<DocumentReference>
) {

    private val userRef
        get() = tenantRefProvider.get()
            ?.collection(collection_users)
            ?: throw IllegalStateException("Tenant missing – user is not logged in")

    private val userLogsRef
        get() = tenantRefProvider.get()
            ?.collection(collection_userLogs)
            ?: throw IllegalStateException("Tenant missing – user is not logged in")


    @Inject
    lateinit var productDataSource: ProductDataSource

    suspend fun checkOnlineConnection(): Boolean {
        var connected = true
        try {
            userRef.get(Source.SERVER).await()
        } catch (e: Exception) {
            connected = false
            Timber.d("ConnectCheck failure: $e")
        }
        return connected
    }

    fun addUser(user: User) {
        userRef.document(user.stringSortID).set(user)
        logUserAction(user.stringSortID, userlog_description_add_user)
    }

    suspend fun userExistsCheck(userName: String): Boolean {
        val res = CompletableDeferred<Boolean>()
        var userExists = false
        userRef.get().addOnSuccessListener { result ->
            for (document in result) {
                if (document.id == stringToStringSortID(userName))
                    userExists = true
            }
            res.complete(userExists)
        }.addOnFailureListener {
            res.complete(false)
        }
        return res.await()
    }

    suspend fun getUser(userID: String): User? {
        val res = CompletableDeferred<User?>()
        var user: User? = null
        userRef.document(userID).get().addOnCompleteListener {
            if (it.isSuccessful && it.result != null) {
                user = it.result.toObject<User>()
                res.complete(user)
            } else {
                res.complete(user)
            }
        }.await()
        return res.await()
    }

    suspend fun addCredit(userID: String, amount: Double): Boolean {
        val res = CompletableDeferred<Boolean>()
        userRef.document(userID).get().addOnSuccessListener {
            val user = it.toObject(User::class.java)
            if (user != null) {
                val topay = user.toPay
                userRef.document(userID).update("toPay", (topay + amount).round(2))
                logUserAction(userID, userlog_description_add_balance, amount)
                res.complete(true)
            } else res.complete(false)
        }.addOnFailureListener {
            res.complete(false)
        }
        return res.await()
    }

    suspend fun addCreditWithPP(
        userID: String,
        amount: Double,
        invoiceId: String,
        transactionCode: String,
    ): Boolean {
        val res = CompletableDeferred<Boolean>()
        userRef.document(userID).get().addOnSuccessListener {
            val user = it.toObject(User::class.java)
            if (user != null) {
                val topay = user.toPay
                userRef.document(userID).update("toPay", (topay + amount).round(2))
                val logText = String.Companion.format(
                    userlog_description_add_balance_paypal,
                    invoiceId,
                    transactionCode
                )
                logUserAction(userID, logText, amount)
                res.complete(true)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    res.complete(
                        addCreditWithPPTryAgainWithWhitespace(
                            userID,
                            amount,
                            invoiceId,
                            transactionCode
                        )
                    )
                }
            }
        }.addOnFailureListener {
            res.complete(false)
        }
        return res.await()
    }

    private suspend fun addCreditWithPPTryAgainWithWhitespace(
        userID: String,
        amount: Double,
        invoiceId: String,
        transactionCode: String,
    ): Boolean {
        val fixedId = "$userID "
        val res = CompletableDeferred<Boolean>()
        userRef.document(fixedId).get().addOnSuccessListener {
            val user = it.toObject(User::class.java)
            if (user != null) {
                val topay = user.toPay
                userRef.document(fixedId).update("toPay", (topay + amount).round(2))
                val logText = String.Companion.format(
                    userlog_description_add_balance_paypal,
                    invoiceId,
                    transactionCode
                )
                logUserAction(fixedId, logText, amount)
                res.complete(true)
            } else {
                res.complete(false)
            }
        }.addOnFailureListener {
            res.complete(false)
        }
        return res.await()
    }

    fun getUserQuery(): Query {
        return userRef.orderBy("stringSortID", Query.Direction.ASCENDING)
    }

    fun getUserLogsRVQuery(currentUserSortId: String): Query {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -28 * 2)
        val lastEightWeeks = cal.time
        return userLogsRef.whereEqualTo(
            "userID",
            currentUserSortId
        )
            .whereGreaterThan("timestamp", lastEightWeeks)
            .orderBy("timestamp", Query.Direction.DESCENDING)
    }

    fun getCountUserLogsTotalStatsQuery(): Task<QuerySnapshot?> {
        return userLogsRef
            .whereNotEqualTo("description", userlog_description_add_balance).get()
    }

    fun getUserLogsQueryWithTimerangeAndOrderByTimestamp(
        timeRange: Date,
        order: Query.Direction
    ): Query {
        return userLogsRef.whereGreaterThan("timestamp", timeRange)
            .orderBy("timestamp", order)
    }

    fun getUsersWithPinQuery(): Query {
        return userRef
            .whereNotEqualTo("pin", null)
    }

    fun deleteUser(userID: String) {
        logUserAction(userID, userlog_description_deleted)
        userRef.document(userID).delete()
    }

    fun logUserAction(id: String, description: String, amount: Double = 0.0) {
        val log = UserLog(id, description, amount)
        userLogsRef.add(log)
    }

    fun getObservableUser(userID: String): LiveData<User> {
        val user = MutableLiveData<User>()

        userRef.document(userID).addSnapshotListener { value, error ->
            if (error != null) {
                Timber.w(UserDataSource::class.java.name, "Observe user $userID failed")
                return@addSnapshotListener
            }
            if (value != null && value.exists()) {
                user.value = value.toObject(User::class.java)
            }
        }
        return user
    }


    suspend fun payProduct(
        userID: String,
        product: Product,
        amount: Int = 1,
        faceDetected: Boolean,
        productDetected: Boolean,
    ) {
        userRef.document(userID).get().addOnSuccessListener {
            userRef.document(userID)
                .update("toPay", (it.getDouble("toPay")!! - product.price * amount).round(2))
        }
        val curUser = getUser(userID)
        productDataSource.productBought(
            userID,
            product,
            amount,
            faceDetected,
            productDetected,
            curUser
        )
        logUserAction(
            userID,
            userlog_description_payed + "$amount * " + product.name,
            product.price * amount
        )

    }

    fun payDono(userID: String, donateItem: DonateItem) {
        userRef.document(userID).get().addOnSuccessListener {
            userRef.document(userID)
                .update("toPay", (it.getDouble("toPay")!! - donateItem.amount).round(2))
        }
        logUserAction(
            userID,
            userlog_description_dono_started + SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.GERMAN
            ).format(
                donateItem.timeLimit
            ),
            donateItem.amount
        )
    }

    fun updateUser(
        userID: String,
        showCredit: Boolean,
        collectData: Boolean,
        pin: String?,
        useProductAI: Boolean,
        faceSkipsPin: Boolean,
    ) {
        val user = userRef.document(userID)
        user.update("showCredit", showCredit)
        user.update("collectData", collectData)
        user.update("pin", pin)
        user.update("useProductAI", useProductAI)
        user.update("faceSkipsPin", faceSkipsPin)
        logUserAction(
            userID,
            String.format(userlog_description_update, showCredit, collectData, pin != null)
        )
    }

    suspend fun buyBoldText(userID: String) {
        userRef.document(userID).get().addOnSuccessListener {
            userRef.document(userID)
                .update("toPay", (it.getDouble("toPay")?.minus(1.toDouble())?.round(2)))
            userRef.document(userID).update("boldEnabled", false)
        }
        logUserAction(userID, "breiten text freigeschalten - 1€")
    }

    suspend fun buyNameColor(userID: String) {
        userRef.document(userID).get().addOnSuccessListener {
            userRef.document(userID)
                .update("toPay", (it.getDouble("toPay")?.minus(1.toDouble())?.round(2)))
            userRef.document(userID).update("nameColor", NameColors.DEFAULT)
        }
        logUserAction(userID, "Farben für Namen gekauft - 1€")
    }

    suspend fun buyEmojiIcon(userID: String) {
        userRef.document(userID).get().addOnSuccessListener {
            userRef.document(userID)
                .update("toPay", (it.getDouble("toPay")?.minus(1.toDouble())?.round(2)))
            userRef.document(userID).update("emojiIcon", "")
        }
        logUserAction(userID, "emoji icon freigeschalten - 1€")
    }

    suspend fun saveCustomizationChanges(
        userID: String,
        boldEnabled: Boolean?,
        emojiIcon: String?,
        nameColor: NameColors?,
    ): Boolean {
        val res = CompletableDeferred<Boolean>()
        userRef.document(userID).update(
            mapOf(
                Pair("boldEnabled", boldEnabled),
                Pair("emojiIcon", emojiIcon),
                Pair("nameColor", nameColor),
            )
        ).addOnSuccessListener {
            res.complete(true)
        }.addOnFailureListener {
            res.complete(false)
        }
        return res.await()
    }

    fun updateUserMail(userId: String, mail: String) {
        val user = userRef.document(userId)
        user.update("mail", mail)
        logUserAction(userId, userlog_mail_update)
    }


    suspend fun payMultiProduct(
        userId: String,
        multiOrderProductListWithProduct: List<MultiOrderProductListWithProduct>,
    ) {
        var totalPay = 0.0
        var productsString = ""
        multiOrderProductListWithProduct.forEach {
            totalPay += it.multiOrderProductList.price * it.multiOrderProductList.amount
            if (productsString.isNotBlank()) productsString += ", "
            productsString += it.multiOrderProductList.amount.toString() + " x " + it.product.name + " (${
                String.format(
                    "%.2f€",
                    it.product.price
                )
            })"
            val curUser = getUser(userId)
            productDataSource.productBought(
                userId, it.product, it.multiOrderProductList.amount,
                faceDetected = false,
                productDetected = false, curUser
            )
        }
        userRef.document(userId).get().addOnSuccessListener {
            userRef.document(userId)
                .update("toPay", (it.getDouble("toPay")!! - totalPay).round(2))
        }

        logUserAction(
            userId,
            "Mehrfachkauf: $productsString",
            totalPay
        )
    }
}

@Parcelize
class UserLog(
    val userID: String = "",
    val description: String = "",
    val amount: Double = 0.0,
) : Parcelable {
    val timestamp = Calendar.getInstance().time
    val device = Build.MODEL
    val changedByEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
}

