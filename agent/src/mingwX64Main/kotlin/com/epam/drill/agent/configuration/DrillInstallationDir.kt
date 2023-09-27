package com.epam.drill.agent.configuration

import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.getpid
import platform.posix.pclose
import platform.posix.popen
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.streams.Input

internal actual fun drillInstallationDir() = run {
    val isContainsAgentPath: (String) -> Boolean = { it.contains("-agentpath:") }
    val fromEnv: () -> String? = { getenv("JAVA_TOOL_OPTIONS")?.toKString() }
    val agentLine = fromEnv()?.takeIf(isContainsAgentPath) ?: fromWmic().takeIf(isContainsAgentPath)
    val agentPath = Regex("-agentpath:(.+?)=.+").matchEntire(agentLine!!)!!.groups[1]!!.value
    agentPath.substringBeforeLast("\\")
}

private fun fromWmic() = memScoped {
    val pid = getpid()
    val command = "wmic process where \"processid='$pid'\" get commandline"
    val file = popen?.invoke(command.cstr.getPointer(this), "r".cstr.getPointer(this))
    Input(file!!).readText().also { pclose?.invoke(file) }
}
