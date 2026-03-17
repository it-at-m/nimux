package de.muenchen.appcenter.nimux.view.main.home

import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.transition.MaterialContainerTransform
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentHomeBinding
import de.muenchen.appcenter.nimux.databinding.HomeUserRvLayoutBinding
import de.muenchen.appcenter.nimux.datasources.DonateItemDataSource
import de.muenchen.appcenter.nimux.model.NameColors
import de.muenchen.appcenter.nimux.model.User
import de.muenchen.appcenter.nimux.repositories.UsersRepository
import de.muenchen.appcenter.nimux.util.scanProductPrefKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class HomeFragment : Fragment(), UserAdapter.UserItemClickListener {

    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var donateItemDataSource: DonateItemDataSource

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var userQuery: Query
    private lateinit var options: FirestoreRecyclerOptions<User>

    private lateinit var adapter: UserAdapter

    private var defaultUseProdAi = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Firebase.firestore(FirebaseApp.initializeApp(requireContext())!!)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userQuery = usersRepository.getUserRVQuery()
        options =
            FirestoreRecyclerOptions.Builder<User>().setQuery(userQuery, User::class.java).build()
        adapter = UserAdapter(options)
        defaultUseProdAi =
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(
                scanProductPrefKey, false
            )

        postponeEnterTransition()
        view.doOnPreDraw {
            startPostponedEnterTransition()
        }

        binding.homeUsersRv.apply {
            setHasFixedSize(false)

            val metrics = resources.displayMetrics

            val yInches = metrics.heightPixels / metrics.ydpi
            val xInches = metrics.widthPixels / metrics.xdpi
            val diagonalInches = sqrt((xInches * xInches + yInches * yInches).toDouble())
            layoutManager = if (diagonalInches >= 7) {
                // 6.5inch device or bigger
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                    GridLayoutManager(requireContext(), 4)
                else
                    GridLayoutManager(requireContext(), 3)
            } else {
                // smaller device
                LinearLayoutManager(requireContext())
            }
            this.adapter = this@HomeFragment.adapter
        }
        adapter.setOnItemClickListener(this)

        if (FirebaseAuth.getInstance().currentUser != null) {
            adapter.startListening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = resources.getInteger(R.integer.motion_medium).toLong()
            scrimColor = requireContext().getColor(android.R.color.transparent)
            setAllContainerColors(requireContext().getColor(android.R.color.transparent))
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.stopListening()
    }

    override fun onItemClick(user: User, cardView: View) {
        requireView().findViewById<RecyclerView>(R.id.home_users_rv).isVerticalScrollBarEnabled =
            false

        val cardDetailName = getString(R.string.home_user_detail_transition_name)
        val nameDetailName = getString(R.string.home_user_name_detail_transition_name)
        val nameView = cardView.findViewById<TextView>(R.id.home_user_item_name)

        val extras =
            FragmentNavigatorExtras(
                cardView to cardDetailName,
                nameView to nameDetailName,
            )
        exitTransition =
            Fade().setDuration(resources.getInteger(R.integer.motion_medium).toLong().div(2))
        reenterTransition =
            Fade().setDuration(resources.getInteger(R.integer.motion_medium).toLong())
        val action = HomeFragmentDirections.actionNavHomeToHomeProductFragment(user, false)
        findNavController().navigate(action, extras)

    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(Dispatchers.IO) {
            val donateItem = donateItemDataSource.getDonationItem()
            if (donateItem != null) {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.donateCardview.visibility = View.VISIBLE
                    if (!donateItem.anon) binding.donateCardName.text = donateItem.userName
                    else binding.donateCardName.visibility = View.GONE
                }
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (_binding != null)
                        binding.donateCardview.visibility = View.GONE
                }
            }
        }
        adapter.startListening()
    }
}

class UserAdapter internal constructor(options: FirestoreRecyclerOptions<User>) :
    FirestoreRecyclerAdapter<User, UserAdapter.UserViewHolder>(options) {

    inner class UserViewHolder(
        private val binding: HomeUserRvLayoutBinding,
        listener: UserItemClickListener,
    ) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.run {
                this.listener = listener
            }
        }

        fun bind(user: User) {
            binding.user = user
            binding.executePendingBindings()

            if (user.nameColor != null) {
                val textColor = TypedValue()
                val theme = binding.root.context.theme
                when (user.nameColor) {
                    NameColors.PURPLE -> theme.resolveAttribute(
                        R.attr.colorAmethyst,
                        textColor,
                        true
                    )

                    NameColors.PINK -> theme.resolveAttribute(R.attr.colorMagenta, textColor, true)
                    NameColors.ORANGE -> theme.resolveAttribute(R.attr.colorOrange, textColor, true)
                    NameColors.BLUE -> theme.resolveAttribute(R.attr.colorCapri, textColor, true)
                    NameColors.CYAN -> theme.resolveAttribute(R.attr.colorSeagreen, textColor, true)
                    else -> theme.resolveAttribute(
                        R.attr.colorOnBackground,
                        textColor,
                        true
                    )
                }
                binding.homeUserItemName.setTextColor(textColor.data)
            }
            if (user.emojiIcon.isNullOrBlank()) binding.userEmojiIcon.visibility = View.GONE
            else {
                binding.userEmojiIcon.visibility = View.VISIBLE
                binding.userEmojiIcon.text = user.emojiIcon
            }
//            if (user.boldEnabled == true) binding.homeUserItemName.typeface = Typeface.DEFAULT_BOLD
//            else binding.homeUserItemName.typeface = Typeface.DEFAULT
        }
    }

    private lateinit var listener: UserItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            HomeUserRvLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), listener
        )
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: User) {
        holder.bind(getItem(position))
    }

    interface UserItemClickListener {
        fun onItemClick(user: User, cardView: View)
    }

    fun setOnItemClickListener(onItemClickListener: UserItemClickListener) {
        this.listener = onItemClickListener
    }

}