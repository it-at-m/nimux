package de.muenchen.appcenter.nimux.view.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.FragmentFeedbackBinding
import de.muenchen.appcenter.nimux.util.FeedbackDoc

class FeedbackFragment : Fragment() {

    private var _binding: FragmentFeedbackBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendFeedbackButton.setOnClickListener {
            val title = binding.feedbackTitleLayout.editText?.text.toString()
            val description = binding.feedbackDescriptionLayout.editText?.text.toString()
            val name = binding.feedbackNameLayout.editText?.text.toString()
            val occurrence = when (binding.feedbackOccurrenceRadioGroup.checkedRadioButtonId) {
                1 -> getString(R.string.feedback_rb_100)
                2 -> getString(R.string.feedback_rb_50_to_100)
                3 -> getString(R.string.feedback_rb_10_to_50)
                4 -> getString(R.string.feedback_rb_1_to_10)
                5 -> getString(R.string.feedback_rb_under_1)
                else -> "none"
            }

            if (title != "" && description != "") {
                val db = Firebase.firestore
                db.collection("feedback").add(FeedbackDoc(title, description, name, occurrence))
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(),
                            getString(R.string.feedback_success_toast_message),
                            Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(),
                            getString(R.string.feedback_error_occurred_toast_message) + "\n" + it.localizedMessage,
                            Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(requireContext(),
                    getString(R.string.feedback_error_toast_message),
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}