package de.muenchen.appcenter.nimux.view.manage.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.databinding.FragmentUserPersonalHistoryBinding
import de.muenchen.appcenter.nimux.databinding.UserPersonalHistoryItemBinding
import de.muenchen.appcenter.nimux.datasources.UserLog
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class UserPersonalHistoryFragment : Fragment() {

    private var _binding: FragmentUserPersonalHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var personalHistoryAdapter: PersonalHistoryAdapter
    @Inject lateinit var usersRepository: UsersRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserPersonalHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        val query = usersRepository.getUserLogsRVQuery(UserPersonalHistoryFragmentArgs.fromBundle(requireArguments()).currentUser.stringSortID)
            .orderBy("timestamp", Query.Direction.DESCENDING)
        val options = FirestoreRecyclerOptions.Builder<UserLog>()
            .setQuery(query, UserLog::class.java).build()
        personalHistoryAdapter = PersonalHistoryAdapter(options)
        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = personalHistoryAdapter
            personalHistoryAdapter.startListening()
        }
    }

    override fun onStart() {
        super.onStart()
        if (::personalHistoryAdapter.isInitialized)
            personalHistoryAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        if (::personalHistoryAdapter.isInitialized)
            personalHistoryAdapter.stopListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class PersonalHistoryAdapter internal constructor(options: FirestoreRecyclerOptions<UserLog>) :
    FirestoreRecyclerAdapter<UserLog, PersonalHistoryAdapter.PersonalHistoryViewHolder>(options) {
    class PersonalHistoryViewHolder internal constructor(private val binding: UserPersonalHistoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserLog) {
            binding.date.text = SimpleDateFormat("dd.MM.yyyy HH:mm").format(item.timestamp.time)
            binding.description.text = item.description
            binding.amount.text = String.format("%.2f€", item.amount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonalHistoryViewHolder {
        val binding = UserPersonalHistoryItemBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false)
        return PersonalHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PersonalHistoryViewHolder,
        position: Int,
        model: UserLog,
    ) {
        holder.bind(getItem(position))
    }

}