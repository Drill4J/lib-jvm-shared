package com.epam.drill.agent

interface ConnectionStatusCallbacks {
    fun setOnAvailable(callback: () -> Unit)
    fun setOnUnavailable(callback: () -> Unit)
}
