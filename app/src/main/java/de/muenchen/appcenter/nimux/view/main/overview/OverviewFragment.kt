package de.muenchen.appcenter.nimux.view.main.overview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentOverviewBinding
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.showEnterUserPin
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class OverviewFragment : Fragment(), OverviewAdapter.OnItemClickListener {


    @Inject
    lateinit var usersRepository: UsersRepository

    private lateinit var adapter: OverviewAdapter
    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_overview,
            container,
            false
        )
        return binding.root
    }

    override fun onDestroyView() {
        adapter.stopListening()
        binding.overviewRv.adapter = null
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.overviewRv.itemAnimator = null

        binding.overviewRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
        }

        val userQuery = usersRepository.getUserRVQuery()
        val options =
            FirestoreRecyclerOptions.Builder<User>().setQuery(userQuery, User::class.java).build()

        adapter = OverviewAdapter(options)
        adapter.setOnItemClickListener(this)
        binding.overviewRv.adapter = adapter
    }

    override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {

        val metrics = resources.displayMetrics

        val yInches = metrics.heightPixels / metrics.ydpi
        val xInches = metrics.widthPixels / metrics.xdpi
        val diagonalInches = sqrt((xInches * xInches + yInches * yInches).toDouble())
        if (diagonalInches >= 7) {
            val clickedUser = documentSnapshot.toObject(User::class.java)
            if (clickedUser != null)
                if (clickedUser.pin != null) {
                    showEnterUserPin(clickedUser, requireContext(), requireView()) {
                        findNavController().navigate(
                            OverviewFragmentDirections.actionNavOverviewToEditUserFragment(
                                clickedUser, true
                            )
                        )
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.no_pin_set_overview_toast),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

}


class OverviewAdapter internal constructor(options: FirestoreRecyclerOptions<User>) :
    FirestoreRecyclerAdapter<User, OverviewAdapter.OverviewViewHolder>(options) {

    inner class OverviewViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {
        internal fun setAttrs(
            user: User,
        ) {
            view.findViewById<TextView>(R.id.overview_item_name).text = user.name
            if (user.showCredit)
                view.findViewById<TextView>(R.id.overview_item_credit).text =
                    String.format("%.2f", user.toPay) + " €"
            else view.findViewById<TextView>(R.id.overview_item_credit).visibility = View.GONE

            view.findViewById<MaterialCardView>(R.id.overview_item_card).setOnClickListener {
                val pos = layoutPosition
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(snapshots.getSnapshot(pos), pos)
                }
            }
        }
    }

    private lateinit var listener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.overview_item_layout, parent, false)
        return OverviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: OverviewViewHolder, position: Int, model: User) {
        holder.setAttrs(model)
    }

    interface OnItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.listener = onItemClickListener
    }
}