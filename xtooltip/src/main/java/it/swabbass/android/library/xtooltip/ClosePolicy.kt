package it.swabbass.android.library.xtooltip

class ClosePolicy internal constructor(private val policy: Int) {

    fun consume() = policy and CONSUME == CONSUME

    fun inside(): Boolean {
        return policy and TOUCH_INSIDE == TOUCH_INSIDE
    }

    fun outside(): Boolean {
        return policy and TOUCH_OUTSIDE == TOUCH_OUTSIDE
    }

    fun anywhere() = inside() and outside()

    override fun toString(): String {
        return "ClosePolicy{policy: $policy, inside:${inside()}, outside: ${outside()}, anywhere: ${anywhere()}, consume: ${consume()}}"
    }

    @Suppress("unused")
    class Builder {
        private var policy = NONE

        fun consume(value: Boolean): Builder {
            policy = if (value) policy or CONSUME else policy and CONSUME.inv()
            return this
        }

        fun inside(value: Boolean): Builder {
            policy = if (value) policy or TOUCH_INSIDE else policy and TOUCH_INSIDE.inv()
            return this
        }

        fun outside(value: Boolean): Builder {
            policy = if (value) policy or TOUCH_OUTSIDE else policy and TOUCH_OUTSIDE.inv()
            return this
        }

        fun clear() {
            policy = NONE
        }

        fun build() = ClosePolicy(policy)
    }

    @Suppress("unused")
    companion object {
        private const val NONE = 0
        private const val TOUCH_INSIDE = 1 shl 1
        private const val TOUCH_OUTSIDE = 1 shl 2
        private const val CONSUME = 1 shl 3

        val TOUCH_NONE = ClosePolicy(NONE)
        val TOUCH_INSIDE_CONSUME = ClosePolicy(TOUCH_INSIDE or CONSUME)
        val TOUCH_INSIDE_NO_CONSUME = ClosePolicy(TOUCH_INSIDE)
        val TOUCH_OUTSIDE_CONSUME = ClosePolicy(TOUCH_OUTSIDE or CONSUME)
        val TOUCH_OUTSIDE_NO_CONSUME = ClosePolicy(TOUCH_OUTSIDE)
        val TOUCH_ANYWHERE_NO_CONSUME = ClosePolicy(TOUCH_INSIDE or TOUCH_OUTSIDE)
        val TOUCH_ANYWHERE_CONSUME = ClosePolicy(TOUCH_INSIDE or TOUCH_OUTSIDE or CONSUME)
    }

}