package de.muenchen.appcenter.nimux.view.main.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentPersonalStatsSelectBinding
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.showEnterUserPin
import javax.inject.Inject

@AndroidEntryPoint
class PersonalStatsSelectFragment : Fragment(), PersonalStatsAdapter.OnItemClickListener {

    @Inject
    lateinit var usersRepository: UsersRepository

    private var _binding: FragmentPersonalStatsSelectBinding? = null
    private val binding get() = _binding!!
    private lateinit var userQuery: Query
    private lateinit var options: FirestoreRecyclerOptions<User>
    private lateinit var personalStatsAdapter: PersonalStatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPersonalStatsSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        personalStatsAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        personalStatsAdapter.stopListening()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            userQuery = usersRepository.getUserRVQuery()

            options = FirestoreRecyclerOptions.Builder<User>()
                .setQuery(userQuery, User::class.java)
                .build()

            personalStatsAdapter = PersonalStatsAdapter(options)
            adapter = personalStatsAdapter
            personalStatsAdapter.setOnItemClickListener(this@PersonalStatsSelectFragment)
        }
    }

    override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
        val clickedUser = documentSnapshot.toObject(User::class.java)
        if (clickedUser != null)
            if (clickedUser.pin != null) {
                showEnterUserPin(clickedUser, requireContext(), requireView()) {
                    findNavController().navigate(
                        PersonalStatsSelectFragmentDirections.actionPersonalStatsSelectFragmentToPersonalStatFragment(
                            clickedUser
                        )
                    )
                }
            } else findNavController().navigate(
                PersonalStatsSelectFragmentDirections.actionPersonalStatsSelectFragmentToPersonalStatFragment(
                    clickedUser
                )
            )
    }

}

class PersonalStatsAdapter internal constructor(options: FirestoreRecyclerOptions<User>) :
    FirestoreRecyclerAdapter<User, PersonalStatsAdapter.PersonalStatsViewHolder>(options) {

    var itemNum = 0

    inner class PersonalStatsViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {
        internal fun setAttrs(
            user: User,
        ) {
            view.findViewById<TextView>(R.id.name_title).text = user.name
            view.findViewById<ConstraintLayout>(R.id.select_user_layout).setOnClickListener {
                val pos = layoutPosition
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(snapshots.getSnapshot(pos), pos)
                }
            }
        }
    }

    private lateinit var listener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonalStatsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.select_user_dialog_rv_layout, parent, false)
        return PersonalStatsViewHolder(view)
    }

    override fun onBindViewHolder(holder: PersonalStatsViewHolder, position: Int, model: User) {
        holder.setAttrs(model)
    }

    interface OnItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.listener = onItemClickListener
    }

    override fun onDataChanged() {
        super.onDataChanged()
        itemNum = itemCount
    }
}