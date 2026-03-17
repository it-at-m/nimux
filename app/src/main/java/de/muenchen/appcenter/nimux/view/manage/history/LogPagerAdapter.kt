package de.muenchen.appcenter.nimux.view.manage.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.datasources.ProductLog
import de.muenchen.appcenter.nimux.datasources.UserLog
import de.muenchen.appcenter.nimux.repositories.OtherRepository
import de.muenchen.appcenter.nimux.repositories.ProductsRepository
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.LogInLogOutLog
import java.util.Calendar

class LogPagerAdapter(
    private val usersRepository: UsersRepository,
    private val productRepository: ProductsRepository,
    private val otherRepository: OtherRepository,
    private val words: List<String>,
    private val navController: NavController,
) :
    RecyclerView.Adapter<LogPagerAdapter.PageHolder>() {

    private lateinit var userLogAdapter: UserLogAdapter
    private lateinit var productsLogAdapter: ProductsLogAdapter
    private lateinit var accountLogAdapter: AccountLogAdapter


    inner class PageHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rv = view.findViewById<RecyclerView>(R.id.log_layout_recyclerview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.log_tab_layout, parent, false)

        return PageHolder(view)
    }


    override fun onBindViewHolder(holder: PageHolder, position: Int) {

        holder.rv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -28 * 24)
        val lastTwoYears = cal.time

        when (position) {
            0 -> {
                val query = usersRepository.getUserLogsQueryWithTimerangeAndOrderByTimestamp(
                    lastTwoYears,
                    Query.Direction.DESCENDING
                )
                val options = FirestoreRecyclerOptions.Builder<UserLog>()
                    .setQuery(query, UserLog::class.java).build()
                userLogAdapter = UserLogAdapter(options)
                userLogAdapter.setOnItemClickListener(object : UserLogAdapter.OnItemClickListener {
                    override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                        val dest =
                            HistoryListFragmentDirections.actionHistoryListFragmentToUserLogDetailFragment(
                                documentSnapshot.toObject(UserLog::class.java)!!
                            )
                        navController.navigate(dest)
                    }
                })
                holder.rv.adapter = userLogAdapter
                userLogAdapter.startListening()
                userLogAdapter.stateRestorationPolicy =
                    StateRestorationPolicy.PREVENT_WHEN_EMPTY

            }

            1 -> {
                val query = productRepository.getProductLogsQueryWithTimerangeAndOrderByTimestamp(
                    lastTwoYears,
                    Query.Direction.DESCENDING
                )
                val options = FirestoreRecyclerOptions.Builder<ProductLog>()
                    .setQuery(query, ProductLog::class.java).build()
                productsLogAdapter = ProductsLogAdapter(options)
                productsLogAdapter.setOnItemClickListener(object :
                    ProductsLogAdapter.OnItemClickListener {
                    override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                        val dest =
                            HistoryListFragmentDirections.actionHistoryListFragmentToProductLogDetailFragment(
                                documentSnapshot.toObject(ProductLog::class.java)!!
                            )
                        navController.navigate(dest)
                    }
                })
                holder.rv.adapter = productsLogAdapter
                productsLogAdapter.startListening()
                productsLogAdapter.stateRestorationPolicy =
                    StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            2 -> {
                val query = otherRepository.getLoginLogoutLogsQueryWithTimerangeAndOrderByTimestamp(
                    lastTwoYears,
                    Query.Direction.DESCENDING
                )
                val options = FirestoreRecyclerOptions.Builder<LogInLogOutLog>()
                    .setQuery(query, LogInLogOutLog::class.java).build()
                accountLogAdapter = AccountLogAdapter(options)
                holder.rv.adapter = accountLogAdapter
                accountLogAdapter.startListening()
                accountLogAdapter.stateRestorationPolicy =
                    StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
        }
    }


    override fun getItemCount(): Int = words.size
}