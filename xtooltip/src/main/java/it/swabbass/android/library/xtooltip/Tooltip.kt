package it.swabbass.android.library.xtooltip

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Spannable
import android.view.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.PopupWindow.INPUT_METHOD_NOT_NEEDED
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

class Tooltip internal constructor(private val context: Context, builder: Builder) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var isShowing = false
        private set

    private val mGravities = Gravity.values().filter { it != Gravity.CENTER }
    private var isVisible = false
    private val mSizeTolerance = context.resources.displayMetrics.density * 10

    private val mLayoutInsetDecor = true
    private val mWindowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
    private val mSoftInputMode = INPUT_METHOD_NOT_NEEDED
    private val mHandler = Handler(Looper.myLooper()!!)

    private var mPopupView: TooltipViewContainer? = null
    private var mText: CharSequence?
    private var mAnchorPoint: Point
    private var mShowArrow: Boolean
    private var mPadding: Int = 0
    private var mActivateDelay: Long
    private var mClosePolicy: ClosePolicy
    private var mShowDuration: Long
    private var mMaxWidth: Int? = null
    private var mTypeface: Typeface? = null
    private var mIsCustomView: Boolean = false
    private var mTooltipLayoutIdRes = R.layout.textview
    private var mCustomTooltipView: View? = null
    private var mTextViewIdRes = android.R.id.text1
    private var mFloatingAnimation: Animation?
    private var mAnimator: ValueAnimator? = null
    private var mShowOverlay: Boolean
    private var mOverlayStyle: Int
    private var mActivated = false
    private var mHasAnchorView = false
    private var mFollowAnchor = false
    private var mAnimationStyleResId: Int
    private var mEnterAnimation: Int
    private var mExitAnimation: Int
    private var mTextStyleResId: Int

    private var mViewOverlay: TooltipOverlay? = null
    private var mDrawable: TooltipTextDrawable? = null
    private var mAnchorView: WeakReference<View>? = null
    private lateinit var mContentView: View
    private var mTextView: TextView? = null
    private var targetView: View? = null
    private val hideRunnable = Runnable { hide() }
    private val activateRunnable = Runnable { mActivated = true }

    var gravity: Gravity? = null
        get() = mCurrentPosition?.gravity
        private set

    var text: CharSequence? = null
        get() = mText
        private set

    var contentView: View? = null
        get() = mContentView
        private set

    private var predrawListener = ViewTreeObserver.OnPreDrawListener {
        if (mHasAnchorView && null != mAnchorView?.get()) {
            val view = mAnchorView?.get()!!
            if (!view.viewTreeObserver.isAlive) {
                removeListeners(view)
            } else {
                if (isShowing && null != mPopupView) {
                    view.getLocationOnScreen(mNewLocation)

                    if (mOldLocation == null) {
                        mOldLocation = intArrayOf(mNewLocation[0], mNewLocation[1])
                    }

                    if (mOldLocation!![0] != mNewLocation[1] || mOldLocation!![1] != mNewLocation[1]) {
                        offsetBy(
                            (mNewLocation[0] - mOldLocation!![0]).toFloat(),
                            (mNewLocation[1] - mOldLocation!![1]).toFloat()
                        )
                    }

                    mOldLocation!![0] = mNewLocation[0]
                    mOldLocation!![1] = mNewLocation[1]
                }
            }
        }
        true
    }

    init {
        val theme = context.theme
            .obtainStyledAttributes(
                null,
                R.styleable.TooltipLayout,
                builder.defStyleAttr,
                builder.defStyleRes
            )
        this.mPadding = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_padding, 30)
        mOverlayStyle =
            theme.getResourceId(
                R.styleable.TooltipLayout_ttlm_overlayStyle,
                R.style.ToolTipOverlayDefaultStyle
            )

        mAnimationStyleResId =
            if (null != builder.animationStyle) {
                builder.animationStyle!!
            } else {
                theme.getResourceId(R.styleable.TooltipLayout_ttlm_animationStyle, android.R.style.Animation_Toast)
            }

        val typedArray =
            context.theme.obtainStyledAttributes(
                mAnimationStyleResId,
                intArrayOf(android.R.attr.windowEnterAnimation, android.R.attr.windowExitAnimation)
            )
        mEnterAnimation = typedArray.getResourceId(typedArray.getIndex(0), 0)
        mExitAnimation = typedArray.getResourceId(typedArray.getIndex(1), 0)
        typedArray.recycle()

        val font = theme.getString(R.styleable.TooltipLayout_ttlm_font)

        mTextStyleResId = theme.getResourceId(R.styleable.TooltipLayout_ttlm_textStyle, 0)

        theme.recycle()

        this.mText = builder.text
        this.mActivateDelay = builder.activateDelay
        this.mAnchorPoint = builder.point!!
        this.mClosePolicy = builder.closePolicy
        this.mMaxWidth = builder.maxWidth
        this.mFloatingAnimation = builder.floatingAnimation
        this.mShowDuration = builder.showDuration
        this.mShowOverlay = builder.overlay
        this.mShowArrow = builder.showArrow && builder.layoutId == null

        builder.anchorView?.let {
            this.mAnchorView = WeakReference(it)
            this.mHasAnchorView = true
            this.mFollowAnchor = builder.followAnchor
        }

        builder.customTooltipView?.let {
            mIsCustomView = true
            mCustomTooltipView = it
            mDrawable = TooltipTextDrawable(context, builder)
        }
        builder.layoutId?.let {
            mTextViewIdRes = builder.textId!!
            mTooltipLayoutIdRes = builder.layoutId!!
            mIsCustomView = true
        } ?: run {
            mDrawable = TooltipTextDrawable(context, builder)
        }

        builder.typeface?.let {
            mTypeface = it
        } ?: run {
            font?.let { mTypeface = Typefaces[context, it] }
        }
    }

    private var mFailureFunc: ((tooltip: Tooltip) -> Unit)? = null
    private var mPrepareFun: ((tooltip: Tooltip) -> Unit)? = null
    private var mShownFunc: ((tooltip: Tooltip) -> Unit)? = null
    private var mHiddenFunc: ((tooltip: Tooltip) -> Unit)? = null

    @Suppress("UNUSED")
    fun doOnFailure(func: ((tooltip: Tooltip) -> Unit)?): Tooltip {
        mFailureFunc = func
        return this
    }

    @Suppress("UNUSED")
    fun doOnShown(func: ((tooltip: Tooltip) -> Unit)?): Tooltip {
        mShownFunc = func
        return this
    }

    @Suppress("UNUSED")
    fun doOnPrepare(func: ((tooltip: Tooltip) -> Unit)?): Tooltip {
        mPrepareFun = func
        return this
    }

    @Suppress("UNUSED")
    fun doOnHidden(func: ((tooltip: Tooltip) -> Unit)?): Tooltip {
        mHiddenFunc = func
        return this
    }

    fun update(text: CharSequence?) {
        mText = text
        if (isShowing && null != mPopupView) {
            mTextView?.text = if (text is Spannable) {
                text
            } else {
                HtmlCompat.fromHtml(text as String, HtmlCompat.FROM_HTML_MODE_COMPACT)
            }
        }
    }

    @Suppress("unused")
    fun update(@StringRes res: Int) {
        update(context.resources.getString(res))
    }

    @SuppressLint("RtlHardcoded", "WrongConstant")
    private fun createPopupLayoutParams(token: IBinder): WindowManager.LayoutParams {
        val p = WindowManager.LayoutParams()
        p.gravity = android.view.Gravity.LEFT or android.view.Gravity.TOP
        p.width = WindowManager.LayoutParams.MATCH_PARENT
        p.height = WindowManager.LayoutParams.MATCH_PARENT
        p.format = PixelFormat.TRANSLUCENT
        p.flags = computeFlags(p.flags)
        p.type = mWindowLayoutType
        p.token = token
        p.softInputMode = mSoftInputMode
        p.title = "ToolTip:" + Integer.toHexString(hashCode())
//        p.windowAnimations = mAnimationStyleResId
        return p
    }


    private fun computeFlags(curFlags: Int): Int {
        var curFlags1 = curFlags
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        curFlags1 = if (mClosePolicy.inside() || mClosePolicy.outside()) {
            curFlags1 and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            curFlags1 or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        if (!mClosePolicy.consume()) {
            curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
        return curFlags1
    }

    @Suppress("UNUSED_PARAMETER")
    private fun preparePopup(params: WindowManager.LayoutParams, gravity: Gravity) {
        mPopupView?.let {
            if (mViewOverlay != null && gravity == Gravity.CENTER) {
                it.removeView(mViewOverlay)
                mViewOverlay = null
            }
        } ?: run {
            val viewContainer = TooltipViewContainer(context)

            if (mShowOverlay && mViewOverlay == null) {
                mViewOverlay = TooltipOverlay(context, 0, mOverlayStyle)
                with(mViewOverlay!!) {
                    adjustViewBounds = true
                    layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                }
            }

            val contentView = LayoutInflater.from(context).inflate(mTooltipLayoutIdRes, viewContainer, false)

            if (!mIsCustomView) {
                mTextView = AppCompatTextView(ContextThemeWrapper(context, mTextStyleResId))
                mTextView?.id = android.R.id.text1
                (contentView as ViewGroup).addView(mTextView)
            }

            mCustomTooltipView?.let {
                (contentView as ViewGroup).addView(it)
                targetView = it
                it.background = mDrawable

                it.setPadding(mPadding / 2, mPadding / 2, mPadding / 2, mPadding / 2)
            }
            contentView.setBackgroundColor(Color.CYAN)
            mFloatingAnimation?.let { contentView.setPadding(it.radius) }

            mTextView = contentView.findViewById(mTextViewIdRes)

            ViewCompat.setLayoutDirection(contentView, ViewCompat.LAYOUT_DIRECTION_LTR)
            ViewCompat.setLayoutDirection(viewContainer, ViewCompat.LAYOUT_DIRECTION_LTR)

            mTextView?.run {
                mDrawable?.let { background = it }
                targetView = this

                if (mShowArrow)
                    setPadding(mPadding, mPadding, mPadding, mPadding)
                else
                    setPadding(mPadding / 2, mPadding / 2, mPadding / 2, mPadding / 2)

                text = if (mText is Spannable) {
                    mText
                } else {
                    HtmlCompat.fromHtml(this@Tooltip.mText as String, HtmlCompat.FROM_HTML_MODE_COMPACT)
                }

                mMaxWidth?.let { maxWidth = it }
                mTypeface?.let { typeface = it }
            }

            if (null != mViewOverlay) {
                viewContainer.addView(
                    mViewOverlay,
                    FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                )
            }

            viewContainer.addView(contentView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            viewContainer.measureAllChildren = true
            viewContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            Timber.i("viewContainer size: ${viewContainer.measuredWidth}, ${viewContainer.measuredHeight}")
            Timber.i("contentView size: ${contentView.measuredWidth}, ${contentView.measuredHeight}")

            targetView?.addOnAttachStateChangeListener {
                onViewAttachedToWindow { _: View?, _: View.OnAttachStateChangeListener ->
                    mAnimator?.start()

                    if (mShowDuration > 0) {
                        mHandler.removeCallbacks(hideRunnable)
                        mHandler.postDelayed(hideRunnable, mShowDuration)
                    }

                    mHandler.removeCallbacks(activateRunnable)
                    mHandler.postDelayed(activateRunnable, mActivateDelay)
                }

                onViewDetachedFromWindow { view: View?, listener: View.OnAttachStateChangeListener ->
                    view?.removeOnAttachStateChangeListener(listener)
                    mAnimator?.cancel()
                    removeCallbacks()
                }
            }

            mContentView = contentView
            mPopupView = viewContainer
        }
    }

    private fun findPosition(
        parent: View,
        anchor: View?,
        offset: Point,
        gravities: ArrayList<Gravity>,
        params: WindowManager.LayoutParams,
        fitToScreen: Boolean = false
    ): Positions? {

        if (null == mPopupView) return null
        if (gravities.isEmpty()) return null

        val gravity = gravities.removeAt(0)

        Timber.i("findPosition. $gravity, offset: $offset")

        val displayFrame = Rect()
        val anchorPosition = intArrayOf(0, 0)
        val centerPosition = PointF(offset)

        parent.getWindowVisibleDisplayFrame(displayFrame)

        anchor?.let {
            anchor.getLocationOnScreen(anchorPosition)
            centerPosition.x += anchorPosition[0] + anchor.width / 2
            centerPosition.y += anchorPosition[1] + anchor.height / 2

            when (gravity) {
                Gravity.LEFT -> {
                    anchorPosition[1] += anchor.height / 2
                }
                Gravity.RIGHT -> {
                    anchorPosition[0] += anchor.width
                    anchorPosition[1] += anchor.height / 2
                }
                Gravity.TOP -> {
                    anchorPosition[0] += anchor.width / 2
                }
                Gravity.BOTTOM -> {
                    anchorPosition[0] += anchor.width / 2
                    anchorPosition[1] += anchor.height
                }
                Gravity.CENTER -> {
                    anchorPosition[0] += anchor.width / 2
                    anchorPosition[1] += anchor.height / 2
                }
            }
        }

        anchorPosition[0] += offset.x
        anchorPosition[1] += offset.y

        Timber.d("anchorPosition: ${anchorPosition[0]}, ${anchorPosition[1]}")
        Timber.d("centerPosition: $centerPosition")
        Timber.d("displayFrame: $displayFrame")

        val w: Int = mContentView.measuredWidth
        val h: Int = mContentView.measuredHeight

        Timber.v("contentView size: $w, $h")

        val contentPosition = Point()
        val arrowPosition = Point()
        val radius = (mFloatingAnimation?.radius ?: run { 0 })

        when (gravity) {
            Gravity.LEFT -> {
                contentPosition.x = anchorPosition[0] - w
                contentPosition.y = anchorPosition[1] - h / 2
                arrowPosition.y = h / 2 - mPadding / 2 - radius
            }
            Gravity.TOP -> {
                contentPosition.x = anchorPosition[0] - w / 2
                contentPosition.y = anchorPosition[1] - h
                arrowPosition.x = w / 2 - mPadding / 2 - radius
            }
            Gravity.RIGHT -> {
                contentPosition.x = anchorPosition[0]
                contentPosition.y = anchorPosition[1] - h / 2
                arrowPosition.y = h / 2 - mPadding / 2 - radius
            }
            Gravity.BOTTOM -> {
                contentPosition.x = anchorPosition[0] - w / 2
                contentPosition.y = anchorPosition[1]
                arrowPosition.x = w / 2 - mPadding / 2 - radius
            }
            Gravity.CENTER -> {
                contentPosition.x = anchorPosition[0] - w / 2
                contentPosition.y = anchorPosition[1] - h / 2
            }
        }

        anchor?.let {
            // pass
        } ?: run {
            mViewOverlay?.let {
                when (gravity) {
                    Gravity.LEFT -> contentPosition.x -= it.measuredWidth / 2
                    Gravity.RIGHT -> contentPosition.x += it.measuredWidth / 2

                    Gravity.TOP -> contentPosition.y -= it.measuredHeight / 2
                    Gravity.BOTTOM -> contentPosition.y += it.measuredHeight / 2
                    Gravity.CENTER -> {
                    }
                }
            }
        }

        Timber.d("arrowPosition: $arrowPosition")
        Timber.d("centerPosition: $centerPosition")
        Timber.d("contentPosition: $contentPosition")

        if (fitToScreen) {
            val finalRect = Rect(
                contentPosition.x,
                contentPosition.y,
                contentPosition.x + w,
                contentPosition.y + h
            )
            if (!displayFrame.rectContainsWithTolerance(finalRect, mSizeTolerance.toInt())) {
                Timber.e("content won't fit! $displayFrame, $finalRect")
                return findPosition(parent, anchor, offset, gravities, params, fitToScreen)
            }
        }

        return Positions(displayFrame, PointF(arrowPosition), centerPosition, PointF(contentPosition), gravity, params)
    }

    private var mCurrentPosition: Positions? = null
    private var mOldLocation: IntArray? = null
    private var mNewLocation: IntArray = intArrayOf(0, 0)

    private fun invokePopup(positions: Positions?): Tooltip? {
        positions?.let {
            isShowing = true
            mCurrentPosition = positions

            setupAnimation(positions.gravity)

            if (mHasAnchorView && mAnchorView?.get() != null) {
                setupListeners(mAnchorView!!.get()!!)
            }

            mDrawable?.setAnchor(
                it.gravity,
                if (!mShowArrow) 0 else mPadding / 2,
                if (!mShowArrow) null else PointF(it.arrowPointX, it.arrowPointY)
            )

            offsetBy(0f, 0f)

            it.params.packageName = context.packageName
            mPopupView?.fitsSystemWindows = mLayoutInsetDecor
            windowManager.addView(mPopupView, it.params)
            fadeIn()
            return this
        } ?: run {
            mFailureFunc?.invoke(this)
            return null
        }
    }

    @Suppress("SpellCheckingInspection")
    fun offsetBy(xoff: Float, yoff: Float) {
        if (isShowing && mPopupView != null && mCurrentPosition != null) {
            Timber.i("offsetBy($xoff, $yoff)")
            mCurrentPosition!!.offsetBy(xoff, yoff)

            mContentView.translationX = mCurrentPosition!!.contentPointX
            mContentView.translationY = mCurrentPosition!!.contentPointY

            mViewOverlay?.let { viewOverlay ->
                viewOverlay.translationX = mCurrentPosition!!.centerPointX - viewOverlay.measuredWidth / 2
                viewOverlay.translationY = mCurrentPosition!!.centerPointY - viewOverlay.measuredHeight / 2
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    fun offsetTo(xoff: Float, yoff: Float) {
        if (isShowing && mPopupView != null && mCurrentPosition != null) {
            Timber.i("offsetTo($xoff, $yoff)")
            mCurrentPosition!!.offsetTo(xoff, yoff)

            mContentView.translationX = mCurrentPosition!!.contentPointX
            mContentView.translationY = mCurrentPosition!!.contentPointY

            mViewOverlay?.let { viewOverlay ->
                viewOverlay.translationX = mCurrentPosition!!.centerPointX - viewOverlay.measuredWidth / 2
                viewOverlay.translationY = mCurrentPosition!!.centerPointY - viewOverlay.measuredHeight / 2
            }
        }
    }

    var offsetX: Float = 0f
        get() = mCurrentPosition?.mOffsetX ?: 0f

    var offsetY: Float = 0f
        get() = mCurrentPosition?.mOffsetY ?: 0f

    private fun setupListeners(anchorView: View) {
        anchorView.addOnAttachStateChangeListener {
            onViewDetachedFromWindow { view: View?, listener: View.OnAttachStateChangeListener ->
                Timber.i("anchorView detached from parent")
                view?.removeOnAttachStateChangeListener(listener)
                dismiss()
            }
        }

        if (mFollowAnchor) {
            anchorView.viewTreeObserver.addOnPreDrawListener(predrawListener)
        }
    }

    private fun removeListeners(anchorView: View?) {
        if (mFollowAnchor) {
            anchorView?.viewTreeObserver?.removeOnPreDrawListener(predrawListener)
        }
    }

    private fun setupAnimation(gravity: Gravity) {
        if (targetView === mContentView || null == mFloatingAnimation) {
            return
        }
        val endValue = mFloatingAnimation!!.radius
        val duration = mFloatingAnimation!!.duration

        val direction: Int = if (mFloatingAnimation!!.direction == 0) {
            if (gravity === Gravity.TOP || gravity === Gravity.BOTTOM) 2 else 1
        } else {
            mFloatingAnimation!!.direction
        }

        val property = if (direction == 2) "translationY" else "translationX"
        mAnimator =
            ObjectAnimator.ofFloat(targetView, property, -endValue.toFloat(), endValue.toFloat())
        mAnimator!!.run {
            setDuration(duration)
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
    }

    fun show(parent: View, gravity: Gravity, fitToScreen: Boolean = false) {
        if (isShowing || (mHasAnchorView && mAnchorView?.get() == null)) return

        isVisible = false

        val params = createPopupLayoutParams(parent.windowToken)
        preparePopup(params, gravity)

        val gravities = mGravities.toCollection(ArrayList())
        gravities.remove(gravity)
        gravities.add(0, gravity)

        mPrepareFun?.invoke(this)

        invokePopup(
            findPosition(
                parent,
                mAnchorView?.get(),
                mAnchorPoint,
                gravities,
                params,
                fitToScreen
            )
        )
    }

    fun hide() {
        Timber.i("hide")
        if (!isShowing) return
        fadeOut()
    }

    fun dismiss() {
        if (isShowing && mPopupView != null) {
            removeListeners(mAnchorView?.get())
            removeCallbacks()
            windowManager.removeView(mPopupView)
            Timber.v("dismiss: $mPopupView")
            mPopupView = null
            isShowing = false
            isVisible = false

            mHiddenFunc?.invoke(this)
        }
    }

    private fun removeCallbacks() {
        mHandler.removeCallbacks(hideRunnable)
        mHandler.removeCallbacks(activateRunnable)
    }

    private fun fadeIn() {
        if (!isShowing || isVisible) return

        if (mEnterAnimation != 0) {
            targetView?.clearAnimation()
            targetView?.startAnimation(AnimationUtils.loadAnimation(context, mEnterAnimation))
        }

        isVisible = true
        mShownFunc?.invoke(this)
    }

    private fun fadeOut() {
        if (!isShowing || !isVisible) return

        if (mExitAnimation != 0) {
            val animation = AnimationUtils.loadAnimation(context, mExitAnimation)
            animation.setListener {
                onAnimationEnd {
                    isVisible = false
                    removeCallbacks()
                    dismiss()
                }
            }.start()

            targetView?.clearAnimation()
            targetView?.startAnimation(animation)

        } else {
            isVisible = false
            removeCallbacks()
            dismiss()
        }

    }

    inner class TooltipViewContainer(context: Context) : FrameLayout(context) {

        init {
            clipChildren = false
            clipToPadding = false
        }

        private var sizeChange: ((w: Int, h: Int) -> Unit)? = null

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            sizeChange?.invoke(w, h)
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)

            if (changed) {
                val out = intArrayOf(-1, -1)
                getLocationOnScreen(out)
                Timber.v("globalVisibleRect: ${out[0]}, ${out[1]}")

                offsetTopAndBottom(-out[1])
            }
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (!isShowing || !isVisible || !mActivated) return super.dispatchKeyEvent(event)
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (keyDispatcherState == null) {
                    return super.dispatchKeyEvent(event)
                }

                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    keyDispatcherState?.startTracking(event, this)
                    return true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    val state = keyDispatcherState
                    if (state != null && state.isTracking(event) && !event.isCanceled) {
                        Timber.v("Back pressed, close the tooltip")
                        hide()
                        return true
                    }
                }
                return super.dispatchKeyEvent(event)
            } else {
                return super.dispatchKeyEvent(event)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!isShowing || !isVisible || !mActivated) return false

            Timber.i("onTouchEvent: $event")
            Timber.d("event position: ${event.x}, ${event.y}")

            val r1 = Rect()
            targetView?.getGlobalVisibleRect(r1)
            val containsTouch = r1.contains(event.x.toInt(), event.y.toInt())

            if (mClosePolicy.anywhere()) {
                hide()
            } else if (mClosePolicy.inside() && containsTouch) {
                hide()
            } else if (mClosePolicy.outside() && !containsTouch) {
                hide()
            }

            return mClosePolicy.consume()
        }
    }

}