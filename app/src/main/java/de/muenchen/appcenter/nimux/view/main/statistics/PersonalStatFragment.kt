package de.muenchen.appcenter.nimux.view.main.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.model.TotalStatsDoc
import de.muenchen.appcenter.nimux.model.User

@AndroidEntryPoint
class PersonalStatFragment : StatisticsFragment() {


    private lateinit var user: User

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        user = PersonalStatFragmentArgs.fromBundle(requireArguments()).selectedUser
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(false)
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            getString(R.string.personal_stats_fragment_title, user.name)
    }

    override fun getStats() {
        totalStats.clear()
        if (totalStats.isEmpty())
            productsRepository.getUserStatQuery(user.stringSortID).get()
                .addOnSuccessListener { query ->
                    query.documents.forEach { doc ->
                        val newDoc = doc.toObject(TotalStatsDoc::class.java)
                        if (newDoc != null) {
                            totalStats.add(newDoc)
                        }
                    }
                    if (totalStats.isEmpty())
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.stats_empty_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    else initCharts()
                }.addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Data couldn't be retrieved. Error: ${it.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        else initCharts()
    }
}