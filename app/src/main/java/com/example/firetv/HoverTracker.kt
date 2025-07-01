package com.example.firetv

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View


class HoverTracker(
    private val resolveFocusedItem: () -> Any?,          // current LB card (or null)
    private val onHoverLog:       (Any, Long) -> Unit    // item, duration-ms
) {
    private var currentItem : Any? = null
    private var startMs     = 0L
    private val main = Handler(Looper.getMainLooper())

    /** Call exactly once from Fragment.onViewCreated(view) */
    fun attach(root: View) {
        root.isFocusableInTouchMode = true
        root.setOnKeyListener { _, key, ev ->
            if (ev.action == KeyEvent.ACTION_DOWN && key.isDpad) {
                endHoverIfNeeded()
                main.post { startHoverIfNeeded() }        // wait until focus really changed
            }
            false                                        // let Leanback handle focus
        }
    }

    /* public: call from onStop/onDestroyView for safety */
    fun forceEnd() = endHoverIfNeeded()

    /* ───────── helpers ───────── */

    private fun endHoverIfNeeded() {
        currentItem?.let { item ->
            onHoverLog(item, System.currentTimeMillis() - startMs)
            currentItem = null
        }
    }

    private fun startHoverIfNeeded() {
        val now = resolveFocusedItem() ?: return
        if (now === currentItem) return           // same card
        currentItem = now
        startMs     = System.currentTimeMillis()
    }

    private val Int.isDpad: Boolean
        get() = this == KeyEvent.KEYCODE_DPAD_UP ||
                this == KeyEvent.KEYCODE_DPAD_DOWN ||
                this == KeyEvent.KEYCODE_DPAD_LEFT ||
                this == KeyEvent.KEYCODE_DPAD_RIGHT
}
