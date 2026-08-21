package com.safenest.kids.service

class ScrollStateGate(
    private val settleDelayMillis: Long = 250L,
    private val maxBlindMillis: Long = 2_000L,
) {
    var state: ScrollState = ScrollState.IDLE
        private set

    private var lastScrollAtMillis: Long = Long.MIN_VALUE
    private var firstScrollAtMillis: Long = Long.MIN_VALUE

    fun onScroll(nowMillis: Long) {
        if (firstScrollAtMillis == Long.MIN_VALUE) firstScrollAtMillis = nowMillis
        lastScrollAtMillis = nowMillis
        state = ScrollState.SCROLLING
    }

    fun update(nowMillis: Long): ScrollState {
        if (state != ScrollState.SCROLLING) return state
        val quietFor = nowMillis - lastScrollAtMillis
        val blindFor = nowMillis - firstScrollAtMillis
        state = when {
            quietFor >= settleDelayMillis -> ScrollState.SETTLING
            blindFor >= maxBlindMillis -> ScrollState.SETTLING
            else -> ScrollState.SCROLLING
        }
        return state
    }

    fun consumeSettled(): Boolean {
        if (state != ScrollState.SETTLING) return false
        state = ScrollState.IDLE
        firstScrollAtMillis = Long.MIN_VALUE
        return true
    }

    fun shouldClassify(nowMillis: Long): Boolean = update(nowMillis) == ScrollState.SETTLING

    fun reset() {
        state = ScrollState.IDLE
        lastScrollAtMillis = Long.MIN_VALUE
        firstScrollAtMillis = Long.MIN_VALUE
    }
}
