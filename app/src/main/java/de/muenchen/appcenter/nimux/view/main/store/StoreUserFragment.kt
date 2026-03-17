package de.muenchen.appcenter.nimux.view.main.store

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.databinding.FragmentStoreUserBinding
import de.muenchen.appcenter.nimux.datasources.UserDataSource
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.util.showEnterUserPin
import de.muenchen.appcenter.nimux.view.main.home.UserAdapter
import javax.inject.Inject

@AndroidEntryPoint
class StoreUserFragment : Fragment(), UserAdapter.UserItemClickListener {

    @Inject
    lateinit var userDataSource: UserDataSource

    private var _binding: FragmentStoreUserBinding? = null
    private val binding get() = _binding!!

    private lateinit var userQuery: Query
    private lateinit var options: FirestoreRecyclerOptions<User>
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStoreUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userQuery = userDataSource.getUsersWithPinQuery()
        options =
            FirestoreRecyclerOptions.Builder<User>().setQuery(userQuery, User::class.java).build()
        userAdapter = UserAdapter(options)
        binding.rv.apply {
            setHasFixedSize(true)
            val spans =
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 3
            layoutManager = GridLayoutManager(requireContext(), spans)
            adapter = userAdapter
        }
        userAdapter.setOnItemClickListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        userAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        userAdapter.stopListening()
    }

    override fun onItemClick(user: User, cardView: View) {
        showEnterUserPin(user, requireContext(), cardView) {
            findNavController().navigate(
                StoreUserFragmentDirections.actionStoreUserFragmentToEditUserCustomizationFragment(
                    user
                )
            )
        }
    }
}