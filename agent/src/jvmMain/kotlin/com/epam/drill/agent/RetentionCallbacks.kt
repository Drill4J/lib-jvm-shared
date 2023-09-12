package com.epam.drill.agent

interface RetentionCallbacks {
    fun setOnAvailable(callback: () -> Unit)
    fun setOnUnavailable(callback: () -> Unit)
}
