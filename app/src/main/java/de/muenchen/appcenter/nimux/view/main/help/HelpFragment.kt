package de.muenchen.appcenter.nimux.view.main.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.transition.MaterialContainerTransform
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentHelpBinding
import de.muenchen.appcenter.nimux.databinding.HelpItemLayoutBinding
import de.muenchen.appcenter.nimux.model.HelpItem
import de.muenchen.appcenter.nimux.util.collection_help

class HelpFragment : Fragment(), HelpAdapter.HelpItemClickListener {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    //inside the HelpFragment the Firestore database is contacted directly because no tenant is needed
    private val db = FirebaseFirestore.getInstance()
    private val mHelpQuery =
        db.collection(collection_help).orderBy("orderPos", Query.Direction.ASCENDING)
    private val options =
        FirestoreRecyclerOptions.Builder<HelpItem>().setQuery(mHelpQuery, HelpItem::class.java)
            .build()
    private val helpAdapter = HelpAdapter(options)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHelpBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        view.doOnPreDraw {
            startPostponedEnterTransition()
        }

        setupRv()
    }

    private fun setupRv() {
        binding.helpRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = helpAdapter
        }
        helpAdapter.setOnItemClickListener(this)
        helpAdapter.startListening()
    }

    override fun onStart() {
        super.onStart()
        helpAdapter.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        helpAdapter.stopListening()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = resources.getInteger(R.integer.motion_medium).toLong()
            scrimColor = requireContext().getColor(android.R.color.transparent)
            setAllContainerColors(requireContext().getColor(android.R.color.transparent))
        }
    }

    override fun onItemClick(helpItem: HelpItem, cardView: View) {

        exitTransition =
            Fade().setDuration(resources.getInteger(R.integer.motion_medium).toLong().div(2))
        reenterTransition =
            Fade().setDuration(resources.getInteger(R.integer.motion_medium).toLong())

        val cardTransTitle = getString(R.string.help_trans_detail)
        val nameTransTitle = getString(R.string.help_trans_title)
        val extras = FragmentNavigatorExtras(
            cardView to cardTransTitle,
            cardView.findViewById<TextView>(R.id.help_title_tv) to nameTransTitle
        )
        val dir = HelpFragmentDirections.actionHelpFragmentToHelpDetailFragment(helpItem)
        findNavController().navigate(dir, extras)
    }
}

class HelpAdapter internal constructor(
    options: FirestoreRecyclerOptions<HelpItem>,
) :
    FirestoreRecyclerAdapter<HelpItem, HelpAdapter.HelpViewHolder>(options) {

    inner class HelpViewHolder(
        private val binding: HelpItemLayoutBinding,
        listener: HelpItemClickListener,
    ) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.run {
                this.listener = listener
            }
        }

        fun bind(helpItem: HelpItem) {
            binding.helpItem = helpItem
            binding.helpTitleTv.text = helpItem.title.replace("\\n", "\n", true)
            binding.executePendingBindings()
        }
    }

    private lateinit var listener: HelpItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        return HelpViewHolder(
            HelpItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), listener
        )
    }

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int, model: HelpItem) {
        holder.bind(getItem(position))
    }

    interface HelpItemClickListener {
        fun onItemClick(helpItem: HelpItem, cardView: View)
    }

    fun setOnItemClickListener(onItemClickListener: HelpItemClickListener) {
        this.listener = onItemClickListener
    }
}