package de.muenchen.appcenter.nimux.view.manage.suggestUser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.databinding.FragmentSuggestedUsersBinding
import de.muenchen.appcenter.nimux.datasources.UserSuggestionDataSource
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.view.main.statistics.PersonalStatsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SuggestedUsersFragment : Fragment(), PersonalStatsAdapter.OnItemClickListener {

    private var _binding: FragmentSuggestedUsersBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var userSuggestionDataSource: UserSuggestionDataSource

    private lateinit var userQuery: Query
    private lateinit var options: FirestoreRecyclerOptions<User>
    private lateinit var userSuggestionsAdapter: PersonalStatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSuggestedUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userQuery = userSuggestionDataSource.getUserSuggestionsQuery()
        options =
            FirestoreRecyclerOptions.Builder<User>().setQuery(userQuery, User::class.java).build()
        userSuggestionsAdapter = PersonalStatsAdapter(options)
        binding.rv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userSuggestionsAdapter
            userSuggestionsAdapter.setOnItemClickListener(this@SuggestedUsersFragment)
        }
    }

    override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
        val userArg = documentSnapshot.toObject(User::class.java)
        if (userArg != null) {
            val action =
                SuggestedUsersFragmentDirections.actionSuggestedUsersFragmentToSuggestedUserManageFragment(
                    (userArg)
                )
            findNavController().navigate(action)
        } else Toast.makeText(requireContext(), "user argument can't be null", Toast.LENGTH_SHORT)
            .show()
    }

    override fun onStart() {
        super.onStart()
        userSuggestionsAdapter.startListening()
        lifecycleScope.launch(Dispatchers.IO) {
            if (userSuggestionDataSource.getUserSuggestionCount() == 0) {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.noSuggestionHint.visibility = View.VISIBLE
                    binding.noSuggestionIcon.visibility = View.VISIBLE
                }
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.noSuggestionHint.visibility = View.GONE
                    binding.noSuggestionIcon.visibility = View.GONE
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        userSuggestionsAdapter.stopListening()
    }
}