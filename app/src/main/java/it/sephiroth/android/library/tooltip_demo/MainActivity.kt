package it.sephiroth.android.library.tooltip_demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.sephiroth.android.library.numberpicker.doOnProgressChanged
import it.sephiroth.android.library.tooltip_demo.databinding.ActivityMainBinding
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import it.sephiroth.android.library.xtooltip.Typefaces
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    var tooltip: Tooltip? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.rootView)
        val metrics = resources.displayMetrics

        binding.button1.setOnClickListener { button ->

            val gravity = Tooltip.Gravity.valueOf(binding.spinnerGravities.selectedItem.toString())
            val closePolicy = getClosePolicy()
            val typeface = if (binding.checkboxFont.isChecked) Typefaces[this, "fonts/GillSans.ttc"] else null
            val animation = if (binding.checkboxAnimation.isChecked) Tooltip.Animation.DEFAULT else null
            val showDuration = binding.seekbarDuration.progress.toLong()
            val arrow = binding.checkboxArrow.isChecked
            val overlay = binding.checkboxOverlay.isChecked
            val style = if (binding.checkboxStyle.isChecked) R.style.ToolTipAltStyle else null
            val text =
                    if (binding.textTooltip.text.isNullOrEmpty()) binding.textTooltip.hint else binding.textTooltip.text!!.toString()

            Timber.v("gravity: $gravity")
            Timber.v("closePolicy: $closePolicy")

            tooltip?.dismiss()

            tooltip = Tooltip.Builder(this)
                    .anchor(button, 0, 0, false)
                    .text(text)
                    .styleId(style)
                    .typeface(typeface)
                    .maxWidth(metrics.widthPixels / 2)
                    .arrow(arrow)
                    .floatingAnimation(animation)
                    .closePolicy(closePolicy)
                    .showDuration(showDuration)
                    .overlay(overlay)
                    .create()

            tooltip
                    ?.doOnHidden {
                        tooltip = null
                    }
                    ?.doOnFailure { }
                    ?.doOnShown {}
                    ?.show(button, gravity, true)
        }

       binding. button2.setOnClickListener {
            val fragment = TestDialogFragment.newInstance()
            fragment.showNow(supportFragmentManager, "test_dialog_fragment")
        }

        binding.seekbarDuration.doOnProgressChanged { numberPicker, progress, formUser ->
            binding.textDuration.text = "Duration: ${progress}ms"
        }

    }

    private fun getClosePolicy(): ClosePolicy {
        val builder = ClosePolicy.Builder()
        builder.inside(binding.switch1.isChecked)
        builder.outside(binding.switch3.isChecked)
        builder.consume(binding.switch2.isChecked)
        return builder.build()
    }

    override fun onDestroy() {
        Timber.i("onDestroy")
        super.onDestroy()
        tooltip?.dismiss()
    }

}
