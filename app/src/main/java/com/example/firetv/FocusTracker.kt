package com.example.firetv


object FocusTracker {
    private var lastFocusedItem: Any? = null


    fun setFocus(item: Any?) {
        lastFocusedItem = item
    }


    fun getLastFocusedItem(): Any? {
        return lastFocusedItem
    }
}