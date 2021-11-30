package it.swabbass.android.library.xtooltip

import android.graphics.PointF
import android.graphics.Rect
import android.view.WindowManager

internal data class Positions(
    val displayFrame: Rect,
    val arrowPoint: PointF,
    val centerPoint: PointF,
    val contentPoint: PointF,
    val gravity: Gravity,
    val params: WindowManager.LayoutParams
) {

    var mOffsetX: Float = 0f
    var mOffsetY: Float = 0f

    fun offsetBy(x: Float, y: Float) {
        mOffsetX += x
        mOffsetY += y
    }

    fun offsetTo(x: Float, y: Float) {
        mOffsetX = x
        mOffsetY = y
    }

    var centerPointX: Float = 0f
        get() = centerPoint.x + mOffsetX

    var centerPointY: Float = 0f
        get() = centerPoint.y + mOffsetY // - displayFrame.top

    var arrowPointX: Float = 0f
        get() = arrowPoint.x + mOffsetX

    var arrowPointY: Float = 0f
        get() = arrowPoint.y + mOffsetY // - displayFrame.top

    var contentPointX: Float = 0f
        get() = contentPoint.x + mOffsetX

    var contentPointY: Float = 0f
        get() = contentPoint.y + mOffsetY // - displayFrame.top
}