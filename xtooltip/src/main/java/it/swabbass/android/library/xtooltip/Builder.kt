package it.swabbass.android.library.xtooltip

import android.content.Context
import android.graphics.Point
import android.graphics.Typeface
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import timber.log.Timber

@Suppress("unused")
class Builder(private val context: Context) {
    internal var point: Point? = null
    internal var closePolicy = ClosePolicy.TOUCH_INSIDE_CONSUME
    internal var text: CharSequence? = null
    internal var anchorView: View? = null
    internal var maxWidth: Int? = null
    internal var defStyleRes = R.style.ToolTipLayoutDefaultStyle
    internal var defStyleAttr = R.attr.ttlm_defaultStyle
    internal var typeface: Typeface? = null
    internal var overlay = true
    internal var floatingAnimation: Animation? = null
    internal var showDuration: Long = 0
    internal var showArrow = true
    internal var activateDelay = 0L
    internal var followAnchor = false
    internal var animationStyle: Int? = null

    @LayoutRes
    internal var layoutId: Int? = null
    internal var customTooltipView: View? = null


    @IdRes
    internal var textId: Int? = null

    fun typeface(value: Typeface?): Builder {
        this.typeface = value
        return this
    }

    fun styleId(@StyleRes styleId: Int?): Builder {
        styleId?.let {
            this.defStyleRes = it
        } ?: run {
            this.defStyleRes = R.style.ToolTipLayoutDefaultStyle
        }
        this.defStyleAttr = R.attr.ttlm_defaultStyle
        return this
    }

    fun customView(tooltipView: View): Builder {
        this.customTooltipView = tooltipView
        this.layoutId = null
        this.textId = null
        this.text = null
        return this
    }

    fun customView(@LayoutRes layoutId: Int, @IdRes textId: Int): Builder {
        this.layoutId = layoutId
        this.textId = textId
        this.customTooltipView = null
        return this
    }

    fun activateDelay(value: Long): Builder {
        this.activateDelay = value
        return this
    }

    fun arrow(value: Boolean): Builder {
        this.showArrow = value
        return this
    }

    fun showDuration(value: Long): Builder {
        this.showDuration = value
        return this
    }

    fun floatingAnimation(value: Animation?): Builder {
        this.floatingAnimation = value
        return this
    }

    fun maxWidth(w: Int): Builder {
        this.maxWidth = w
        return this
    }

    fun overlay(value: Boolean): Builder {
        this.overlay = value
        return this
    }

    fun anchor(x: Int, y: Int): Builder {
        this.anchorView = null
        this.point = Point(x, y)
        return this
    }

    fun anchor(view: View, xoff: Int = 0, yoff: Int = 0, follow: Boolean = false): Builder {
        this.anchorView = view
        this.followAnchor = follow
        this.point = Point(xoff, yoff)
        return this
    }

    fun text(text: CharSequence): Builder {
        this.text = text
        return this
    }

    fun text(@StringRes text: Int): Builder {
        this.text = context.getString(text)
        return this
    }

    fun text(@StringRes text: Int, vararg args: Any): Builder {
        this.text = context.getString(text, args)
        return this
    }

    fun closePolicy(policy: ClosePolicy): Builder {
        this.closePolicy = policy
        Timber.v("closePolicy: $policy")
        return this
    }

    fun animationStyle(@StyleRes id: Int): Builder {
        this.animationStyle = id
        return this
    }

    fun create(): Tooltip {
        if (null == anchorView && null == point) {
            throw IllegalArgumentException("missing anchor point or anchor view")
        }
        return Tooltip(context, this)
    }
}