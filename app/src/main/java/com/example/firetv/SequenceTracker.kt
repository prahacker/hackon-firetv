package com.example.firetv

import android.view.KeyEvent

object SequenceTracker {
    private var lastActionTimestamp: Long = System.currentTimeMillis()
    private val actionCounts: MutableMap<String, Int> = mutableMapOf()

    // Holds the last D-pad key that was pressed to be logged later
    var lastDpadKeyCode: Int? = null

    fun timeSinceLastAction(): Float {
        val now = System.currentTimeMillis()
        val delta = (now - lastActionTimestamp) / 1000f
        lastActionTimestamp = now
        return delta
    }

    fun increment(action: String): Int {
        val count = (actionCounts[action] ?: 0) + 1
        actionCounts[action] = count
        return count
    }

    fun reset() {
        actionCounts.clear()
        lastActionTimestamp = System.currentTimeMillis()
        lastDpadKeyCode = null
    }
}