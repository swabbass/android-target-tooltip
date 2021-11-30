package it.swabbass.android.library.xtooltip

data class Animation(val radius: Int, val direction: Int, val duration: Long) {

    @Suppress("unused")
    companion object {
        val DEFAULT = Animation(8, 0, 400)
        val SLOW = Animation(4, 0, 600)
    }
}