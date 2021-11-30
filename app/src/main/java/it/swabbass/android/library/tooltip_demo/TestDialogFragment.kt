package it.swabbass.android.library.tooltip_demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import it.swabbass.android.library.tooltip_demo.databinding.DialogFragmentBinding
import it.swabbass.android.library.xtooltip.ClosePolicy
import it.swabbass.android.library.xtooltip.Tooltip

class TestDialogFragment : DialogFragment() {
    private var _binding: DialogFragmentBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogFragmentBinding.inflate(inflater, container, false)
        return binding.root    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding. button1.setOnClickListener { button ->
            Tooltip.Builder(context!!)
                .anchor(button, 0, 0, false)
                .closePolicy(ClosePolicy.TOUCH_ANYWHERE_CONSUME)
                .showDuration(0)
                .text("This is a dialog")
                .create()
                .show(button, Tooltip.Gravity.TOP, false)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    companion object {
        fun newInstance(): TestDialogFragment {
            return TestDialogFragment()
        }
    }
}