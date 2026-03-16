package de.muenchen.appcenter.nimux.view.manage.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentManageUsersBinding
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.viewmodel.manage.users.ManageUsersViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ManageUsersFragment : Fragment(), ManageUserAdapter.OnItemClickListener {

    @Inject
    lateinit var usersRepository: UsersRepository

    private lateinit var adapter: ManageUserAdapter
    private var _binding: FragmentManageUsersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManageUsersViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_manage_users,
            container,
            false
        )

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_manage_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_check_network_action -> {
                lifecycleScope.launch {
                    if (usersRepository.connectedOnline())
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.network_check_successful),
                            Toast.LENGTH_SHORT
                        ).show()
                    else
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.network_check_failed))
                            .setPositiveButton(R.string.ok, null)
                            .show()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
        val currentUser: User = documentSnapshot.toObject()!!
        binding.userListRv.isVerticalScrollBarEnabled = false
        val action =
            ManageUsersFragmentDirections.actionManageUsersFragmentToManageUserItem(currentUser)
        findNavController().navigate(action)

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
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        doOther()
    }

    private fun doOther() {
        binding.userListRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                var fabVis = binding.addUserFab.isVisible
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && fabVis) {
                        fabVis = false
                        binding.addUserFab.hide()
                    } else if (dy < 0 && !fabVis) {
                        fabVis = true
                        binding.addUserFab.show()
                    }
                }
            })
        }
        val userQuery = usersRepository.getUserRVQuery()
        val options =
            FirestoreRecyclerOptions.Builder<User>().setQuery(userQuery, User::class.java).build()

        adapter = ManageUserAdapter(options)
        binding.userListRv.adapter = adapter
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        adapter.setOnItemClickListener(this)

        viewModel.navToAddUser.observe(viewLifecycleOwner) { navToAddUser ->
            if (navToAddUser) {
                findNavController().navigate(R.id.action_manageUsersFragment_to_addUserFragment)
            }
        }

    }
}

class ManageUserAdapter internal constructor(options: FirestoreRecyclerOptions<User>) :
    FirestoreRecyclerAdapter<User, ManageUserAdapter.ManageUserViewHolder>(options) {

    private lateinit var listener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageUserViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_manage_user_view, parent, false)
        return ManageUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: ManageUserViewHolder, position: Int, model: User) {
        holder.setAttrs(model.name, model.toPay)
    }

    inner class ManageUserViewHolder internal constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {
        internal fun setAttrs(
            userName: String,
            userPay: Double,
        ) {
            view.findViewById<TextView>(R.id.list_user_name).text = userName
            view.findViewById<TextView>(R.id.list_user_pay).text =
                (view.resources.getString(R.string.credit) + " " + String.format(
                    "%.2f",
                    userPay
                ) + "€")

            view.findViewById<MaterialCardView>(R.id.manage_user_list_card).setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(snapshots.getSnapshot(position), position)
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.listener = onItemClickListener
    }
}