package de.muenchen.appcenter.nimux.view.manage.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.DocumentSnapshot
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.datasources.ProductLog
import de.muenchen.appcenter.nimux.datasources.UserLog
import de.muenchen.appcenter.nimux.util.LogInLogOutLog

class UserLogAdapter internal constructor(options: FirestoreRecyclerOptions<UserLog>) :
    FirestoreRecyclerAdapter<UserLog, UserLogAdapter.UserLogViewHolder>(options) {

    private lateinit var listener: OnItemClickListener

    inner class UserLogViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {
        internal fun setAttrs(
            timestamp: String,
            userID: String,
            description: String,
            amount: Double,
        ) {
            view.findViewById<TextView>(R.id.user_log_timestamp).text = timestamp
            view.findViewById<TextView>(R.id.user_log_user_id).text = userID
            view.findViewById<TextView>(R.id.user_log_description).text = description
            if (amount != 0.0) {
                view.findViewById<TextView>(R.id.user_log_amount).apply {
                    text = amount.toString()
                    visibility = View.VISIBLE
                }
                view.findViewById<TextView>(R.id.user_log_amount_title).visibility = View.VISIBLE
            } else {
                view.findViewById<TextView>(R.id.user_log_amount_title).visibility = View.GONE
                view.findViewById<TextView>(R.id.user_log_amount).visibility = View.GONE
            }

            view.findViewById<MaterialCardView>(R.id.user_log_card).setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION)
                    listener.onItemClick(snapshots.getSnapshot(position), position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_log_rv_item_layout, parent, false)
        return UserLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserLogViewHolder, position: Int, model: UserLog) {
        holder.setAttrs(model.timestamp.toString(), model.userID, model.description, model.amount)
    }


    interface OnItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.listener = onItemClickListener
    }

}

class ProductsLogAdapter internal constructor(options: FirestoreRecyclerOptions<ProductLog>) :
    FirestoreRecyclerAdapter<ProductLog, ProductsLogAdapter.ProductsLogViewHolder>(options) {

    private lateinit var listener: OnItemClickListener

    inner class ProductsLogViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {
        internal fun setAttrs(
            timestamp: String, productId: String, description: String, amount: Int,
        ) {
            view.findViewById<TextView>(R.id.product_log_timestamp).text = timestamp
            view.findViewById<TextView>(R.id.product_log_product_id).text = productId
            view.findViewById<TextView>(R.id.product_log_description).text = description
            if (amount != 0) {
                view.findViewById<TextView>(R.id.product_log_amount).apply {
                    text = amount.toString()
                    visibility = View.VISIBLE
                }
                view.findViewById<TextView>(R.id.product_log_amount_title).visibility = View.VISIBLE
            } else {
                view.findViewById<TextView>(R.id.product_log_amount_title).visibility = View.GONE
                view.findViewById<TextView>(R.id.product_log_amount).visibility = View.GONE
            }

            view.findViewById<MaterialCardView>(R.id.product_log_card).setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION)
                    listener.onItemClick(snapshots.getSnapshot(position), position)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ProductsLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_log_rv_item_layout, parent, false)
        return ProductsLogViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProductsLogViewHolder,
        position: Int,
        model: ProductLog,
    ) {
        holder.setAttrs(model.timestamp.toString(),
            model.productID,
            model.description,
            model.amount)
    }

    interface OnItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.listener = onItemClickListener
    }
}

class AccountLogAdapter internal constructor(options: FirestoreRecyclerOptions<LogInLogOutLog>) :
    FirestoreRecyclerAdapter<LogInLogOutLog, AccountLogAdapter.AccountLogViewHolder>(options) {

    inner class AccountLogViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {
        internal fun settAttrs(
            timestamp: String, mail: String, login: Boolean, device:String
        ) {
            view.findViewById<TextView>(R.id.account_log_timestamp).text = timestamp
            view.findViewById<TextView>(R.id.account_log_mail).text = mail
            if (login) view.findViewById<TextView>(R.id.account_log_description).text = "Login"
            else view.findViewById<TextView>(R.id.account_log_description).text = "Logout"
            view.findViewById<TextView>(R.id.account_log_detail_device).text = device

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AccountLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.account_log_rv_item_layout, parent, false)
        return AccountLogViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: AccountLogViewHolder,
        position: Int,
        model: LogInLogOutLog,
    ) {
        holder.settAttrs(model.timestamp.toString(), model.accountEmail.toString(), model.login, model.device)
    }

}